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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import lib.server.temp.TempFileUtil;

/**
 * Manages parsing and serialization of data using Gson for hotels, users, and reviews.
 * Provides implementations for saving and loading data in JSON format.
 */
public class Parser {

    private static final Gson Gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private static final Type HotelsListT = new TypeToken<List<Hotel>>(){}.getType();
    private static final Type HotelT = new TypeToken<Hotel>(){}.getType();
    private static final Type UserT = new TypeToken<User>(){}.getType();
    private static final Type UserListT = new TypeToken<List<User>>(){}.getType();
    private static final Type ReviewT = new TypeToken<Review>(){}.getType();
    private static final Type ReviewListT = new TypeToken<List<Review>>(){}.getType();

    private static final String TEMP_DIR_DEF = "tempDirectory";

    /**
     * Carica gli hotel dal file JSON e li inserisce nella mappa degli hotel.
     */
    public static class HotelsLoad implements Runnable {
        private final String filepath; // Percorso del file JSON contenente gli hotel
        private final ConcurrentHashMap<String, ArrayList<Hotel>> Map; // Mappa degli hotel da aggiornare

        public HotelsLoad(String path, ConcurrentHashMap<String, ArrayList<Hotel>> table) {
            this.filepath = path;
            this.Map = table;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader(filepath))) {
                List<Hotel> hotelsFromFile = Gson.fromJson(reader, HotelsListT);
                for (Hotel h : hotelsFromFile) {
                    Map.compute(h.getCity().toLowerCase(), 
                        (String key, ArrayList<Hotel> value) -> {
                            if (value == null) value = new ArrayList<>();
                            value.add(h);
                            return value;
                        }
                    );
                }
            } catch (Exception e) {
                // Problemi riscontrati possono includere file non trovato o JSON malformato
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva gli hotel dalla mappa in un file JSON.
     */
    public static class HotelsSave implements Runnable {
        private final String filepath; // Percorso del file JSON in cui salvare gli hotel
        Collection<ArrayList<Hotel>> HotelsToSaveMap; // Collezione di mappe di hotel da salvare
        private Path tempfile; // Percorso del file temporaneo

        public HotelsSave(String path, ConcurrentHashMap<String, ArrayList<Hotel>> hotels) {
            this.filepath = path;
            this.HotelsToSaveMap = hotels.values();
        }

        @Override
        public void run() {
            Iterator<ArrayList<Hotel>> ListsToSave = HotelsToSaveMap.iterator();
            boolean success = false;
            try {
                tempfile = TempFileUtil.createTempSameDir(filepath, "htl", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    while(ListsToSave.hasNext()) {
                        ArrayList<Hotel> hotelsList = ListsToSave.next();
                        synchronized (hotelsList){
                            hotelsList.forEach(h -> Gson.toJson(h, HotelT, writer));
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
        private final String filepath; // Percorso del file JSON contenente gli utenti
        private final ConcurrentHashMap<String, User> Map; // Mappa degli utenti da aggiornare

        public UsersLoad(String path, ConcurrentHashMap<String, User> table) {
            this.filepath = path;
            this.Map = table;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader("./" + filepath))) {
                List<User> UsersFromFile = Gson.fromJson(reader, UserListT);
                for (User u : UsersFromFile) {
                    Map.put(u.getUsername(), u);
                }
            } catch (FileNotFoundException e) {
                // Gestisce il caso in cui il file non sia trovato
            } catch (Exception e) {
                // Gestisce il caso di utenti malformati
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva gli utenti dalla mappa in un file JSON.
     */
    public static class UsersSave implements Runnable {
        private final String filepath; // Percorso del file JSON in cui salvare gli utenti
        private final Collection<User> UsersToSaveMap; // Collezione di utenti da salvare
        private Path tempfile; // Percorso del file temporaneo

        public UsersSave(String filepath, ConcurrentHashMap<String, User> Users) {
            this.filepath = filepath;
            this.UsersToSaveMap = Users.values();
            tempfile = null;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                Iterator<User> UsersToSave = UsersToSaveMap.iterator();
                tempfile = TempFileUtil.createTempSameDir(filepath, "usr", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    UsersToSave.forEachRemaining(u -> Gson.toJson(u, UserT, writer));
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
        private final String filepath; // Percorso del file JSON contenente le recensioni
        private final ConcurrentHashMap<Integer, List<Review>> RevMap; // Mappa delle recensioni da aggiornare

        public ReviewsLoad(String filepath, ConcurrentHashMap<Integer, List<Review>> queue) {
            this.filepath = filepath;
            this.RevMap = queue;
        }

        @Override
        public void run() {
            try (JsonReader reader = new JsonReader(new FileReader(filepath))) {
                ArrayList<Review> Reviews = Gson.fromJson(reader, ReviewListT);
                for (Review r : Reviews) {
                    RevMap.compute(r.getHotelId(), 
                        (Integer key, List<Review> value) -> {
                            if (value == null) value = new ArrayList<>();
                            value.add(r);
                            return value;
                        }
                    );
                }
            } catch (FileNotFoundException e) {
                // Gestisce il caso in cui il file non sia trovato
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva le recensioni dalla LinkedBlockingQueue in un file JSON.
     */
    public static class ReviewsSave implements Runnable {
        private final String filepath; // Percorso del file JSON in cui salvare le recensioni
        private final LinkedBlockingQueue<Review> DumpingQueue; // Coda delle recensioni da salvare
        private Path tempfile; // Percorso del file temporaneo
        private final long timepoll; // Tempo di attesa per il polling delle recensioni
        private final TimeUnit Unit; // Unità di tempo per il polling
        private final int Max_Elab; // Numero massimo di recensioni da elaborare

        public ReviewsSave(String filepath, LinkedBlockingQueue<Review> queue, int max, long timetopoll, TimeUnit unit) {
            this.filepath = filepath;
            this.DumpingQueue = queue;
            this.timepoll = timetopoll;
            this.Unit = unit;
            this.Max_Elab = max;
        }

        @Override
        public void run() {
            if (DumpingQueue.isEmpty()) return;
            int dumped_revs = 0;
            try {
                File tempDir = TempFileUtil.TempDirSamePath(filepath, TEMP_DIR_DEF);
                tempfile = Files.createTempFile(tempDir.toPath(), "rvs", ".tmp");
                try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                    writer.beginArray();
                    while (dumped_revs < Max_Elab && !DumpingQueue.isEmpty()) {
                        Review tmp = DumpingQueue.poll(timepoll, Unit);
                        if (tmp == null) break;
                        Gson.toJson(tmp, ReviewT, writer);
                        dumped_revs++;
                    }
                    writer.endArray().flush();
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
        private final String file_path; // Percorso del file principale delle recensioni
        private Path tempfile; // Percorso del file temporaneo
        private File tempDir = null; // Directory temporanea

        /**
         * Costruttore per MergeReviews.
         *
         * @param filepath Percorso del file principale delle recensioni.
         */
        public MergeReviews(String filepath) {
            this.file_path = filepath;
            this.tempfile = null;
        }

        @Override
        public void run() {
            boolean success = false;

            // Apre la directory temporanea
            tempDir = TempFileUtil.TempDirOpen(file_path, TEMP_DIR_DEF);
            File[] list_files = tempDir.listFiles();

            // Verifica che ci siano file temporanei da unire
            if (list_files.length > 0) {
                try {
                    tempfile = TempFileUtil.createTempSameDir(file_path, "rev-merge", ".tmp");
                    try (JsonWriter writer = Gson.newJsonWriter(new FileWriter(tempfile.toFile()))) {
                        writer.beginArray();
                        ArrayList<Review> rev_list = null;

                        // Scrive il contenuto del file principale esistente
                        try (JsonReader reader = new JsonReader(new FileReader(file_path))) {
                            rev_list = Gson.fromJson(reader, ReviewListT);

                            // Aggiunge le recensioni dal file principale se esistono
                            if (rev_list != null) {
                                for (Review r : rev_list) {
                                    Gson.toJson(r, ReviewT, writer);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            // Nel caso il file principale non esista viene skippato
                        }

                        // Aggiunge i contenuti dei file temporanei
                        for (File f : list_files) {
                            try (JsonReader reader = new JsonReader(new FileReader(f))) {
                                rev_list = Gson.fromJson(reader, ReviewListT);
                            }

                            // Scrive ogni recensione letta dai file temporanei
                            for (Review r : rev_list) {
                                Gson.toJson(r, ReviewT, writer);
                            }
                        }

                        writer.endArray().flush();
                        TempFileUtil.AtomicMove(tempfile, file_path);
                        success = true;
                    } 
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Se l'unione ha avuto successo, elimina i file temporanei e la directory temporanea
                    if (success) {
                        try {
                            TempFileUtil.deleteDirectoryRecursively(tempDir);
                            TempFileUtil.deleteTempFileIfExists(tempDir.toPath());
                            TempFileUtil.deleteTempFileIfExists(tempfile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
