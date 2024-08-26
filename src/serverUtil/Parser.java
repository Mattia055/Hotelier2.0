package serverUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import lib.etc.TempFileUtil;
import lib.struct.Hotel;
import lib.struct.Review;

/**
 * Gestisce il parsing e la serializzazione dei dati utilizzando la libreria Gson per gli hotel, gli utenti e le recensioni.
 * Fornisce anche implementazioni per il salvataggio e il caricamento dei dati in formato JSON.
 */
public class Parser {

    // Configurazione di Gson per la serializzazione e deserializzazione JSON
    private static Gson Gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    // Tipi per la deserializzazione dei dati
    private static Type HotelsListT = new TypeToken<List<Hotel>>(){}.getType();
    private static Type HotelT = new TypeToken<Hotel>(){}.getType();
    private static Type UserT = new TypeToken<User>(){}.getType();
    private static Type UserListT = new TypeToken<List<User>>(){}.getType();
    private static Type ReviewT = new TypeToken<Review>(){}.getType();
    private static Type ReviewListT = new TypeToken<List<Review>>(){}.getType();

    private static final String TEMP_DIR_DEF = "tempDirectory";

    /**
     * Carica gli hotel dal file JSON e li inserisce nella mappa degli hotel.
     */
    public static class HotelsLoad implements Runnable {
        private String filepath; // Percorso del file JSON contenente gli hotel
        private ConcurrentHashMap<String, ConcurrentHashMap<String, Hotel>> Map; // Mappa degli hotel da aggiornare

        /**
         * Costruttore per HotelLoad.
         *
         * @param path Percorso del file JSON.
         * @param Table Mappa degli hotel da aggiornare.
         */
        public HotelsLoad(String path, ConcurrentHashMap<String, ConcurrentHashMap<String, Hotel>> Table) {
            filepath = path;
            Map = Table;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader(filepath))) {
                List<Hotel> hotelsFromFile = Gson.fromJson(reader, HotelsListT);
                for (Hotel h : hotelsFromFile) {
                    //faccio nel rankmanager
                    //if(h.getRating() == null)
                        //h.setRating(Score.Placeholder());
                    Map.compute(h.getCity().toLowerCase(), (String key, ConcurrentHashMap<String, Hotel> value) -> {
                        if (value == null) value = new ConcurrentHashMap<String, Hotel>();
                        value.put(h.getName(), h);
                        return value;
                    });
                }
            } catch (Exception e) {
                // Problemi riscontrati possono includere file non trovato o JSON malformato
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva gli hotel nella mappa in un file JSON.
     */
    public static class HotelSave implements Runnable {
        private String filepath; // Percorso del file JSON in cui salvare gli hotel
        private Collection<ConcurrentHashMap<String, Hotel>> HotelsToSave; // Collezione di mappe di hotel da salvare
        private Path tempfile; // Percorso del file temporaneo

        /**
         * Costruttore per HotelSave.
         *
         * @param path Percorso del file JSON.
         * @param hotels Collezione di mappe di hotel da salvare.
         */
        public HotelSave(String path, ConcurrentHashMap<String, ConcurrentHashMap<String, Hotel>> hotels) {
            tempfile = null;
            HotelsToSave = hotels.values();
            filepath = path;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                if(HotelsToSave.isEmpty()) return;
                tempfile = TempFileUtil.createTempSameDir(filepath, "htl", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    for (ConcurrentHashMap<String, Hotel> hotelMap : HotelsToSave) {
                        synchronized (hotelMap) {
                            for (Hotel h : hotelMap.values()) {
                                Gson.toJson(h, HotelT, writer);
                            }
                        }
                    }
                    writer.endArray().flush();
                    TempFileUtil.AtomicMove(tempfile, filepath);
                    success = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (success) {
                    TempFileUtil.deleteTempFileIfExists(tempfile);
                }
            }
        }
    }

    /**
     * Carica gli utenti dal file JSON e li inserisce nella mappa degli utenti.
     */
    public static class UsersLoad implements Runnable {
        private String filepath; // Percorso del file JSON contenente gli utenti
        private ConcurrentHashMap<String, User> Map; // Mappa degli utenti da aggiornare

        /**
         * Costruttore per UsersLoad.
         *
         * @param path Percorso del file JSON.
         * @param table Mappa degli utenti da aggiornare.
         */
        public UsersLoad(String path, ConcurrentHashMap<String, User> table) {
            filepath = path;
            Map = table;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader(filepath))) {
                List<User> UsersFromFile = Gson.fromJson(reader, UserListT);
                for (User u : UsersFromFile) Map.put(u.getUsername(), u);
            } catch (FileNotFoundException e) {
                // Gestisce il caso in cui il file non sia trovato
                return;
            } catch (Exception e) {
                // Gestisce il caso di utenti malformati
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva gli utenti nella mappa in un file JSON.
     */
    public static class UsersSave implements Runnable {
        private String filepath; // Percorso del file JSON in cui salvare gli utenti
        private Collection<User> UsersToSave; // Collezione di utenti da salvare
        private Path tempfile; // Percorso del file temporaneo

        /**
         * Costruttore per UsersSave.
         *
         * @param filepath Percorso del file JSON.
         * @param Users Collezione di utenti da salvare.
         */
        public UsersSave(String filepath, ConcurrentHashMap<String, User> Users) {
            this.filepath = filepath;
            UsersToSave = Users.values();
            tempfile = null;
        }

        @Override
        public void run() {
            Boolean success = false;
            try {
                if(UsersToSave.isEmpty()) return;
                tempfile = TempFileUtil.createTempSameDir(filepath, "usr", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    for (User u : UsersToSave) {
                        Gson.toJson(u, UserT, writer);
                    }
                    writer.endArray().flush();
                }
                TempFileUtil.AtomicMove(tempfile, filepath);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (success) {
                    TempFileUtil.deleteTempFileIfExists(tempfile);
                }
            }
        }
    }

    /**
     * Carica le recensioni dal file JSON e le inserisce nella mappa delle recensioni.
     */
    public static class ReviewsLoad implements Runnable {
        private String filepath; // Percorso del file JSON contenente le recensioni
        private ConcurrentHashMap<Integer, List<Review>> RevMap; // Mappa delle recensioni da aggiornare

        /**
         * Costruttore per ReviewsLoad.
         *
         * @param filepath Percorso del file JSON.
         * @param queue Mappa delle recensioni da aggiornare.
         */
        public ReviewsLoad(String filepath, ConcurrentHashMap<Integer, List<Review>> queue) {
            this.filepath = filepath;
            RevMap = queue;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader(filepath))) {
                ArrayList<Review> Reviews = Gson.fromJson(reader, ReviewListT);
                for (Review r : Reviews) {
                    RevMap.compute(r.getHotelId(), (Integer key, List<Review> value) -> {
                        if (value == null) value = new ArrayList<Review>();
                        value.add(r);
                        return value;
                    });
                }
            } catch (FileNotFoundException e) {
                // Gestisce il caso in cui il file non sia trovato
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva le recensioni dalla LinkedBlockingQueue in un file JSON.
     */
    public static class ReviewsSave implements Runnable {
        private String filepath; // Percorso del file JSON in cui salvare le recensioni
        private LinkedBlockingQueue<Review> DumpingQueue; // Coda delle recensioni da salvare
        private Path tempfile; // Percorso del file temporaneo
        private long timepoll; // Tempo di attesa per il polling delle recensioni
        private TimeUnit Unit; // Unità di tempo per il polling
        private int Max_Elab; // Numero massimo di recensioni da elaborare

        /**
         * Costruttore per ReviewsSave.
         *
         * @param filepath Percorso del file JSON.
         * @param queue Coda delle recensioni da salvare.
         * @param max Numero massimo di recensioni da elaborare.
         * @param timetopoll Tempo di attesa per il polling.
         * @param unit Unità di tempo per il polling.
         */
        public ReviewsSave(String filepath, LinkedBlockingQueue<Review> queue, int max, long timetopoll, TimeUnit unit) {
            this.filepath = filepath;
            tempfile = null;
            DumpingQueue = queue;
            timepoll = timetopoll;
            Unit = unit;
            Max_Elab = max;
        }

        @Override
        public void run() {
            if(DumpingQueue.isEmpty()) return;
            int dumped_revs = 0;
            try {
                File tempDir = TempFileUtil.TempDirSamePath(filepath, TEMP_DIR_DEF);
                tempfile = Files.createTempFile(tempDir.toPath(), "rvs", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    System.out.println("Dumping reviews... "+DumpingQueue.isEmpty());
                    while (dumped_revs < Max_Elab && (!DumpingQueue.isEmpty())) {
                        Review current_rev = DumpingQueue.poll(timepoll, Unit);
                        if (current_rev == null) break;
                        Gson.toJson(current_rev, ReviewT, writer);
                        dumped_revs++;
                    }
                    writer.endArray();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unisce tutte le recensioni dai file temporanei nel file principale.
     */
    public static class MergeReviews implements Runnable {
        private String file_path; // Percorso del file principale delle recensioni
        private Path tempfile; // Percorso del file temporaneo
        private File tempDir = null; // Directory temporanea

        /**
         * Costruttore per MergeReviews.
         *
         * @param filepath Percorso del file principale delle recensioni.
         */
        public MergeReviews(String filepath) {
            file_path = filepath;
            tempfile = null;
        }

        @Override
        public void run() {
            boolean success = false;

            tempDir = TempFileUtil.TempDirOpen(file_path, TEMP_DIR_DEF);
            File[] list_files = tempDir.listFiles();
            
            if (list_files.length > 0) {
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    ArrayList<Review> rev_list = null;
                    // Scrive il contenuto del file principale esistente
                    try (JsonReader reader = new JsonReader(new FileReader(new File(file_path)))) {
                        rev_list = Gson.fromJson(reader, ReviewListT);
                    }
                    for (Review r : rev_list) {
                        Gson.toJson(r, ReviewT, writer);
                    }

                    // Aggiunge i contenuti dei file temporanei
                    for (File f : list_files) {
                        try (JsonReader reader = new JsonReader(new FileReader(f))) {
                            rev_list = Gson.fromJson(reader, ReviewListT);
                        }
                        for (Review r : rev_list) {
                            Gson.toJson(r, ReviewT, writer);
                        }
                    }
                    writer.endArray().flush();
                    TempFileUtil.AtomicMove(tempfile, file_path);
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (success) {
                        try {
                            TempFileUtil.deleteDirectoryRecursively(tempDir);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
