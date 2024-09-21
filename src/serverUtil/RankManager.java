package serverUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import lib.share.struct.Score;

/**
 * Gestisce il ranking degli hotel basato sulle recensioni degli utenti e notifica gli aggiornamenti tramite UDP.
 * Questa classe è un singleton che elabora le valutazioni degli hotel e aggiorna le classifiche periodicamente.
 */
public class RankManager implements Runnable {

    public static int exec = 0;

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
    private static ConcurrentHashMap<String, ArrayList<Hotel>>      HotelsTable;
    private static ConcurrentHashMap<Integer, ArrayList<Review>>    ReviewsTable;
    private static ConcurrentHashMap<String,User>                   UsersTable;
    private static LinkedBlockingQueue<Review>                      DumpQueue;

    // Notificatore UDP per la trasmissione degli aggiornamenti
    private static UDPNotifier multicaster;

    // Parametri di configurazione
    private static double time_decay;       // Decadimento temporale per il calcolo del peso delle recensioni
    private static double exp_multiplier;   // Moltiplicatore di esperienza per il calcolo del peso delle recensioni
    private static int max_experience;      // Esperienza massima per calcolare il peso delle recensioni

    // Punteggio fisso per le recensioni
    private static Integer fixed_experience;
    private static int exp_inf;
    private static int exp_sup;
    private static Consumer<String> addExperience;
    private static LocalDateTime comparison;


    /**
     * Costruttore privato per l'implementazione del pattern Singleton.
     */
    private RankManager() {
        HotelsTable         = ServerContext.HotelsTable;
        ReviewsTable        = ServerContext.ReviewsTable;
        DumpQueue           = ServerContext.DumpingQueue;
        time_decay          = ServerContext.TIME_DECAY;
        max_experience      = User.MAX_EXP;
        exp_multiplier      = ServerContext.EXP_MULTIPLIER;
        fixed_experience    = ServerContext.EXP;
        exp_inf             = ServerContext.EXP_INF;
        exp_sup             = ServerContext.EXP_SUP;
        UsersTable          = ServerContext.UsersTable;
        multicaster         = new UDPNotifier(ServerContext.MULTI_ADDR, ServerContext.MULTI_PORT);

        //Setta la funzione di incremento dell'esperienza
        //in base alla configurazione
        if(fixed_experience == null)
            addExperience = RankManager::addExperienceRand;
        else
            addExperience = RankManager::addExperienceFixed;
    }

    private void updateTime() {
        comparison = LocalDateTime.now();
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

    //incremento dell'esperienza fisso
    private static void addExp(String u, int exp) throws NullPointerException{
        UsersTable.get(u).addExperience(exp);
    }

    //incremento dell'esperienza casuale su un intervallo
    private static void addExperienceRand(String u){
        int exp = (int) (Math.random() * (exp_sup - exp_inf) + exp_inf);
        addExp(u,exp);
    }

    private static void addExperienceFixed(String u){
        addExp(u,fixed_experience);
    }

    /*
     * Aggiorna il ranking di un hotel in base a una lista di recensioni
     */

    private boolean updateRankDumpReviews(Hotel h,LocalDateTime comparison) {
        //ottengo la lista delle recensioni dalla mappa e la rimuovo
        ArrayList<Review> reviews = ReviewsTable.remove(h.getID());
        if(reviews == null) return false;
        
        Score score = null;
        synchronized(h){
            Score local = h.getRating();
            score = local == null ? Score.Placeholder() : local.clone();
        }
        
        for(Review r : reviews){
            r.setCounted(comparison);
            //calcolo il peso della recensione
            double time_weight  = Math.exp((-time_decay) * ChronoUnit.DAYS.between(r.getAdded(), comparison));
            double exp_weight   = Math.exp((-exp_multiplier) * ((1 - r.getUserExp()) / max_experience));
            double weight       = time_weight * exp_weight;
            //aggiorno il punteggio dell'hotel
            score.setGlobal  ((1 / (weight + 1)) * (weight * r.getReviewScore().getGlobal()   + score.getGlobal()  ));
            score.setCleaning((1 / (weight + 1)) * (weight * r.getReviewScore().getCleaning() + score.getCleaning()));
            score.setPosition((1 / (weight + 1)) * (weight * r.getReviewScore().getPosition() + score.getPosition()));
            score.setPrice   ((1 / (weight + 1)) * (weight * r.getReviewScore().getPrice()    + score.getPrice()   ));
            score.setService ((1 / (weight + 1)) * (weight * r.getReviewScore().getService()  + score.getService() ));
            //aggiorno l'esperienza dell'utente 
            try{
                addExperience.accept(r.getUsername());
                DumpQueue.add(r);
            }catch(NullPointerException e){
                e.printStackTrace();
            }
        }
        
        //aggiorno il punteggio dell'hotel
        synchronized(h){h.setRating(score);}
        return true;
    }

    private void UpdateTopHotel(ArrayList<Hotel> hlist){
        Hotel hTop = hlist.get(0);
            synchronized(hlist){
                boolean toUpdate = false;
                for(Hotel h : hlist){
                    toUpdate = toUpdate || updateRankDumpReviews(h,comparison);
                }
                //ordinamento lista secondo il rank
                //l'ultimo hotel della lista è quello che ha rank massimo
                if(toUpdate){   //se false la lista è già ordinata
                    Collections.sort(hlist,(h1,h2) -> Double.compare(h2.rank, h1.rank));

                    //aggiorno la posizione di ogni hotel
                    int i = 0;
                    for(Hotel h : hlist){
                        h.rank_position = i+1;
                        i++;
                    }
                }
 
            }
        /* 
        if(hTop.getName().equals(hlist.get(hlist.size()-1).getName())){
            //se l'hotel in cima alla lista è cambiato, invio una notifica
            multicaster.NotifyGroup(hTop.getName());
        }
            */
        if(!(hTop.getName().equals(hlist.get(0).getName()))){
            //se l'hotel in cima alla lista è cambiato, invio una notifica
            System.out.println("[RankManager] Notifying " + hTop.getName());
            multicaster.NotifyGroup(hTop.getName());
        }

    }

    @Override
    public void run() {
        if(++exec % 10 == 0)
            System.out.println("[RankManager] executed 10 times");
        updateTime();
        //aggiorno il ranking di tutti gli hotel e li ordino
        for(ArrayList<Hotel> hotels : HotelsTable.values()){
            UpdateTopHotel(hotels);
        }
        
    }

    


    
}
