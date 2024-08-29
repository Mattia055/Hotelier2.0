package serverUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import lib.share.struct.Score;

/**
 * Gestisce il ranking degli hotel basato sulle recensioni degli utenti e notifica gli aggiornamenti tramite UDP.
 * Questa classe è un singleton che elabora le valutazioni degli hotel e aggiorna le classifiche periodicamente.
 */
public class RankManager implements Runnable {

    /**
     * Classe di utilità per l'invio di notifiche UDP.
     */
    public static class UDPNotifier {
        private int UDPport; // Porta UDP per l'invio delle notifiche
        private InetAddress UDPaddress; // Indirizzo UDP per l'invio delle notifiche

        /**
         * Costruisce un UDPNotifier con l'indirizzo e la porta specificati.
         * 
         * @param address l'indirizzo al quale inviare le notifiche
         * @param port la porta sulla quale inviare le notifiche
         */
        public UDPNotifier(String address, int port) {
            this.UDPport = port;
            try {
                this.UDPaddress = InetAddress.getByName(address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Invia un messaggio di notifica all'indirizzo e alla porta specificati.
         * 
         * @param message il messaggio da inviare
         */
        public void NotifyGroup(String message) {
            try (DatagramSocket UDPsocket = new DatagramSocket()) {
                byte[] payload = message.getBytes();
                UDPsocket.send(new DatagramPacket(payload, payload.length, UDPaddress, UDPport));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Istanza singleton
    private static RankManager instance = null;

    // Strutture dati per memorizzare informazioni su hotel e recensioni
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, Hotel>> HotelsTable;
    private static ConcurrentHashMap<Integer, List<Review>> ReviewsTable;
    private static LinkedBlockingQueue<Review> DumpQueue;

    // Notificatore UDP per la trasmissione degli aggiornamenti
    private static UDPNotifier multicaster;

    // Parametri di configurazione
    private static double time_decay; // Decadimento temporale per il calcolo del peso delle recensioni
    private static double exp_multiplier; // Moltiplicatore di esperienza per il calcolo del peso delle recensioni
    private static int max_experience; // Esperienza massima per calcolare il peso delle recensioni

    // Tabella per tenere traccia degli hotel con il punteggio più alto per città
    private static HashMap<String, Hotel> MaxRank;

    /**
     * Costruttore privato per l'implementazione del pattern Singleton.
     */
    private RankManager() {
        HotelsTable = ServerContext.HotelsTable;
        ReviewsTable = ServerContext.ReviewsTable;
        DumpQueue = ServerContext.DumpingQueue;
        multicaster = new UDPNotifier(ServerContext.MULTI_ADDR, ServerContext.MULTI_PORT);
        time_decay = ServerContext.TIME_DECAY;
        max_experience = User.MAX_EXP;
        exp_multiplier = ServerContext.EXP_MULTIPLIER;

        // Inizializza la tabella dei massimi
        MaxRank = new HashMap<String, Hotel>();
        for (String City : HotelsTable.keySet()) {
            MaxRank.put(City, null);
        }
    }

    /**
     * Restituisce l'istanza singleton di RankManager.
     * 
     * @return l'istanza singleton di RankManager
     */
    public static RankManager getInstance() {
        if (instance == null) {
            instance = new RankManager();
        }
        return instance;
    }

    /**
     * Aggiunge una città alla tabella dei massimi se non è già presente.
     * 
     * @param City il nome della città da aggiungere
     */
    public static void addCityToMaxRank(String City) {
        synchronized (MaxRank) {
            MaxRank.putIfAbsent(City, null);
        }
    }

    /**
     * Aggiorna il punteggio dell'hotel basato sulle recensioni e aggiunge le recensioni alla DumpQueue.
     * 
     * @param h l'hotel da aggiornare
     * @param comparison il tempo corrente utilizzato per calcolare il peso delle recensioni
     */
    private static void updateHotelScoreDumpReviews(Hotel h, LocalDateTime comparison) {
        // Dichiara uno score locale basato su quello dell'hotel
        Score local = null;

        synchronized (h) {
            local = h.getRating() == null ? Score.Placeholder() : h.getRating().clone();
        }

        List<Review> RevList = ReviewsTable.get(h.getID());
        if(RevList != null){
            synchronized (RevList) {
                for (Review r : RevList) {
                    // Calcola i pesi basati sull'orario della recensione e l'esperienza dell'utente
                    double time_weight = Math.exp(-time_decay * ChronoUnit.DAYS.between(r.getTime(), comparison));
                    double exp_weight = Math.exp(-exp_multiplier * ((1 - r.getUserExp()) / max_experience));
                    double weight = time_weight * exp_weight;

                    // Aggiorna i punteggi locali con quelli delle recensioni
                    local.setGlobal  ((1 / (weight + 1)) * (weight * r.getReviewScore().getGlobal()   + local.getGlobal()  ));
                    local.setCleaning((1 / (weight + 1)) * (weight * r.getReviewScore().getCleaning() + local.getCleaning()));
                    local.setPosition((1 / (weight + 1)) * (weight * r.getReviewScore().getPosition() + local.getPosition()));
                    local.setPrice   ((1 / (weight + 1)) * (weight * r.getReviewScore().getPrice()    + local.getPrice()   ));
                    local.setService ((1 / (weight + 1)) * (weight * r.getReviewScore().getService()  + local.getService() ));

                    // Aggiunge la recensione alla DumpQueue
                    try {
                        DumpQueue.add(r);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // Azzero la lista delle recensioni dopo averle processate
                RevList = new ArrayList<Review>();
            }
        }

        // Aggiorna il punteggio dell'hotel con il punteggio calcolato
        synchronized (h) {
            h.setRating(local);
        }
    }

    /**
     * Esegue il thread per aggiornare i punteggi degli hotel e notificare gli aggiornamenti.
     */
    @Override
    public void run() {
        LocalDateTime comparison = LocalDateTime.now();
        
        // Itera su ogni città nella tabella degli hotel
        for (Map.Entry<String, ConcurrentHashMap<String, Hotel>> City : HotelsTable.entrySet()) {
            // Ordina gli hotel per città in base al punteggio
            ArrayList<Hotel> HotelsPerCity = new ArrayList<Hotel>(City.getValue().values()); 
            for (Hotel h : HotelsPerCity) {
                updateHotelScoreDumpReviews(h, comparison); // Aggiorna il punteggio dell'hotel e gestisce le recensioni
            }

            // Ordina gli hotel in base al punteggio medio in modo decrescente
            Collections.sort(HotelsPerCity, Comparator.comparingDouble((Hotel hotel) -> hotel.getRating().getMean()).reversed());

            // Aggiorna la tabella dei massimi
            synchronized (MaxRank) {
                if (MaxRank.get(City.getKey()) == null || MaxRank.get(City.getKey()).getID() != HotelsPerCity.get(0).getID()) {
                    MaxRank.put(City.getKey(), HotelsPerCity.get(0));
                    
                    // Invia una notifica UDP con il miglior hotel
                    multicaster.NotifyGroup("Il miglior hotel a " + City.getKey() + " è " + HotelsPerCity.get(0).getName() + " con un punteggio di " + HotelsPerCity.get(0).getRating().getMean());
                }
            }
        }
    }
}
