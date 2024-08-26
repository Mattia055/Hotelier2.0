package serverUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lib.etc.ConfigLoader;
import lib.struct.Hotel;
import lib.struct.Review;

/**
 * Questa classe gestisce il contesto del server, inclusi i parametri di configurazione,
 * i thread pool e le hash map necessarie. Implementa il pattern Singleton per garantire
 * che esista solo un'istanza di `ServerContext` durante l'esecuzione del server.
 */
public class ServerContext {

    private static ServerContext instance = null;
    private static final String CONFIG_DEFAULT = "config/server.properties";
    private static String Config;

    // Parametri del server
    protected static int PORT;
    protected static String MULTI_ADDR;
    protected static int MULTI_PORT;

    // Parametri del MainThreadPool
    protected static ExecutorService    MainPool;
    private static int POOL_MAXSIZE     = Integer.MAX_VALUE;
    private static int POOL_CORESIZE    = 0;
    private static long POOL_KEEP_ALIVE = 60;
    private static int POOL_QUEUESIZE   = 10000;
    private static long POOL_AWAIT      = 1;
    private static final TimeUnit TIME_KEEP_ALIVE = TimeUnit.MILLISECONDS;
    private static final TimeUnit AWAIT_UNIT = TimeUnit.MILLISECONDS;

    // Parametri per il dump delle recensioni
    private static int MAX_DUMP         = 500;
    private static long DUMP_WAIT       = 500;
    private static TimeUnit DUMP_UNIT   = TimeUnit.MILLISECONDS;

    // Parametri per il RankingManager
    protected static double TIME_DECAY = 0.1;
    protected static double EXP_MULTIPLIER = 0.1;

    // Parametri per i thread pool schedulati
    protected static ScheduledExecutorService   RankingPool;
    protected static ScheduledExecutorService   FileHandlerPool;
    private static ArrayList<ScheduledFuture<?>> ScheduledTasks;
    private static int FILE_POOL_CORESIZE = 3;
    private static long SAVE_INIT   = 1000;
    private static long RANK_INIT   = 1000;
    private static long SAVE_DELAY  = 10000;
    private static long RANK_DELAY  = 10000;
    private static final TimeUnit TIME_DELAY = TimeUnit.MILLISECONDS;

    // Parametri per i percorsi dei file
    protected static String Hotelsfrom  = null;
    protected static String Hotelsto    = null;
    protected static String Usersfrom   = "data/Users.json";
    protected static String Usersto     = null;
    protected static String Reviewsfrom = "data/Reviews.json";
    protected static String Reviewsto   = null;

    //flags
    private static boolean FORCE_REVS_INIT = true;

    // Tabelle e code
    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Hotel>> HotelsTable;
    protected static ConcurrentHashMap<String, User> UsersTable;
    protected static ConcurrentHashMap<String, Boolean> LoggedTable; // Non si tiene traccia dei valori
    protected static ConcurrentHashMap<Integer, List<Review>> ReviewsTable;
    protected static LinkedBlockingQueue<Review> DumpingQueue;

    //parametri per RequestHandler
    protected static int MAX_BATCH_SIZE     = 10;

    /**
     * Costruttore privato per inizializzare il contesto del server.
     * 
     * @param filepath Il percorso del file di configurazione.
     */
    private ServerContext(String filepath) {
        Config = filepath;
        try {
            parseArguments();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Inizializza i thread pool
        MainPool = new ThreadPoolExecutor(
            POOL_CORESIZE, POOL_MAXSIZE, POOL_KEEP_ALIVE, TIME_KEEP_ALIVE, new ArrayBlockingQueue<>(POOL_QUEUESIZE)
        );
        FileHandlerPool = Executors.newScheduledThreadPool(FILE_POOL_CORESIZE);
        RankingPool     = Executors.newSingleThreadScheduledExecutor();

        // Inizializza le tabelle e le code
        HotelsTable     = new ConcurrentHashMap<>();
        UsersTable      = new ConcurrentHashMap<>();
        LoggedTable     = new ConcurrentHashMap<>();
        ReviewsTable    = new ConcurrentHashMap<>();
        DumpingQueue    = new LinkedBlockingQueue<>();
    }

    /**
     * Restituisce l'istanza singleton di `ServerContext`.
     * 
     * @param filepath Il percorso del file di configurazione.
     * @return L'istanza di `ServerContext`.
     */
    public static ServerContext getInstance(String filepath) {
        if (instance == null) {
            if (filepath == null) filepath = CONFIG_DEFAULT;
            instance = new ServerContext(filepath);
        }
        return instance;
    }

    /**
     * Restituisce l'istanza singleton di `ServerContext` utilizzando il percorso di configurazione predefinito.
     * 
     * @return L'istanza di `ServerContext`.
     */
    public static ServerContext getInstance() {
        return getInstance(null);
    }

    /**
     * Analizza i parametri di configurazione dal file specificato.
     * 
     * @throws FileNotFoundException Se il file di configurazione non viene trovato.
     * @throws IOException Se si verifica un errore durante la lettura del file di configurazione.
     * @throws MalformedParametersException Se i parametri nel file di configurazione sono malformati.
     */
    private void parseArguments() throws FileNotFoundException, IOException, MalformedParametersException {
        ConfigLoader loadArg = new ConfigLoader(Config);

        // Carica e valida i parametri di configurazione
        POOL_MAXSIZE        = loadArg.getIntAttribute   ("pool_maxsize", POOL_MAXSIZE);
        POOL_CORESIZE       = loadArg.getIntAttribute   ("pool_coresize", POOL_CORESIZE);
        POOL_KEEP_ALIVE     = loadArg.getLongAttribute  ("pool_keepalive", POOL_KEEP_ALIVE);
        POOL_QUEUESIZE      = loadArg.getIntAttribute   ("pool_queuesize", POOL_QUEUESIZE);
        POOL_AWAIT          = loadArg.getLongAttribute  ("pool_await", POOL_AWAIT);
        FILE_POOL_CORESIZE  = loadArg.getIntAttribute   ("file_pool_coresize", FILE_POOL_CORESIZE);
        SAVE_DELAY          = loadArg.getLongAttribute  ("save_delay", SAVE_DELAY);
        RANK_DELAY          = loadArg.getLongAttribute  ("rank_delay", RANK_DELAY);
        SAVE_INIT           = loadArg.getLongAttribute  ("save_init", SAVE_INIT);
        RANK_INIT           = loadArg.getLongAttribute  ("rank_init", RANK_INIT);
        EXP_MULTIPLIER      = loadArg.getDoubleAttribute("exp_multiplier", EXP_MULTIPLIER);
        TIME_DECAY          = loadArg.getDoubleAttribute("time_decay", TIME_DECAY);
        Hotelsfrom          = loadArg.getStringAttribute("hotelsfrom", null);
        PORT                = loadArg.getIntAttribute   ("port", 0);
        MULTI_ADDR          = loadArg.getStringAttribute("multicast-addr", null);
        MULTI_PORT          = loadArg.getIntAttribute   ("multicast-port", 0);
        MAX_BATCH_SIZE      = loadArg.getIntAttribute   ("max_batch_size", MAX_BATCH_SIZE);
        FORCE_REVS_INIT     = loadArg.getBooleanAttribute("force_revs_init", FORCE_REVS_INIT);
        Hotelsto            = loadArg.getStringAttribute("hotelsto", Hotelsfrom);
        Usersto             = loadArg.getStringAttribute("usersto", Usersfrom);
        Reviewsto           = loadArg.getStringAttribute("reviewsto", Reviewsfrom);
        Reviewsfrom         = loadArg.getStringAttribute("reviewsfrom", Reviewsfrom);
        Usersfrom           = loadArg.getStringAttribute("usersfrom", Usersfrom);
        MAX_DUMP            = loadArg.getIntAttribute   ("max_dump", MAX_DUMP);
        DUMP_WAIT           = loadArg.getLongAttribute  ("dump_wait", DUMP_WAIT);


        // Verifica la validità degli attributi
        validateConfiguration();
    }

    /**
     * Valida i parametri di configurazione e l'esistenza dei file specificati.
     * 
     * @throws MalformedParametersException Se i parametri di configurazione non sono validi.
     * @throws FileNotFoundException Se uno dei file specificati non esiste.
     */
    private static void validateConfiguration() throws MalformedParametersException {
        if(FORCE_REVS_INIT && !(validateFileExistence(Reviewsfrom))){
            throw new MalformedParametersException("Il parametro 'reviewsfrom' è attivo ma 'reviewsfrom' non è specificato o non esiste");
        }

        if (!validateFileExistence(Hotelsfrom)) {
            throw new MalformedParametersException("Il percorso specificato da 'hotelsfrom' è mancante o non esiste");
        }
        if (PORT <= 1024 || PORT > 65535) {
            throw new MalformedParametersException("Il parametro 'port' è mancante o non valido. Seleziona un valore tra [1024, 65535].");
        }
        
        if (MULTI_ADDR == null) {
            throw new MalformedParametersException("Il parametro 'multicast-addr' è mancante nella configurazione.");
        }
        if (MULTI_PORT <= 1024 || MULTI_PORT > 65535) {
            throw new MalformedParametersException("Il parametro 'multicast-port' è mancante o non valido. Seleziona un valore tra [1024, 65535].");
        }

        // Verifica se MULTI_ADDR è un indirizzo host valido
         
        try {
            InetAddress.getByName(MULTI_ADDR);
        } catch (UnknownHostException e) {
            throw new MalformedParametersException("Il parametro 'multicast-addr' non risolve in un indirizzo host valido.");
        }

        // Verifica l'esistenza dei file (se specificati)
        /* 
        validateFileExistence(Usersfrom, "usersfrom");
        validateFileExistence(Reviewsfrom, "reviewsfrom");
        validateFileExistence(Hotelsto, "hotelsto");
        validateFileExistence(Usersto, "usersto");
        validateFileExistence(Reviewsto, "reviewsto");
        */
    }

    /**
     * Verifica se il file specificato esiste.
     * 
     * @param filePath Il percorso del file da verificare.
     * @param attributeName Il nome dell'attributo associato al file.
     * @throws FileNotFoundException Se il file specificato non esiste.
     */
    private static boolean validateFileExistence(String filePath) {
        return filePath != null && new File(filePath).exists();
    }

    /**
     * Inizializza le risorse del server eseguendo i task di parsing per gli hotel, gli utenti e le recensioni.
     */
    public static void ResourcesInit() throws MalformedParametersException {
        ArrayList<Future<?>> results = new ArrayList<>();
        Future<?> HotelTask = MainPool.submit(new Parser.HotelsLoad(Hotelsfrom, HotelsTable));
        Future<?> UsersTask = MainPool.submit(new Parser.UsersLoad(Usersfrom, UsersTable));
        Future<?> ReviewsTask = null;

        if(FORCE_REVS_INIT){
            //aggiunge la task qui per 
            ReviewsTask = MainPool.submit(new Parser.ReviewsLoad(Reviewsfrom, ReviewsTable));
            waitForTaskCompletion(HotelTask);
            HotelTask = null;
            for (ConcurrentHashMap<String, Hotel> city : HotelsTable.values()) {
                city.values().forEach(hotel -> {hotel.setRating(null);});
            }

            /*
             * Il do - while esiste nel caso il RankManager non riesca a processare
             * tutte le recensioni di ReviewsTask, in modo da riprovare finché non
             * riesce a processarle tutte.
             */
            do{
                waitForTaskCompletion(MainPool.submit(RankManager.getInstance()));

                // riinizializza la coda dumping dato che non si fa il dumping di
                // recensioni caricate dal file

                DumpingQueue = new LinkedBlockingQueue<>();                

            }
            while(!ReviewsTask.isDone());
        }

        waitForTaskCompletion(HotelTask, UsersTask, ReviewsTask);

        // Attende il completamento di tutti i task di inizializzazione
        for (Future<?> task : results) {
            waitForTaskCompletion(task);
        }
        /*
         * forza il caricamento delle recensioni e il ricalcolo del ranking degli
         * hotel se il flag è attivo
         */
        if(FORCE_REVS_INIT){
            //azzera i punteggi degli hotel
            
        }

        //inizializza hashMap delle recensioni
    }

    /**
     * Pianifica i task ricorrenti per il salvataggio dei file e l'elaborazione del ranking.
     */
    protected static void scheduleTasks() {
        ScheduledTasks = new ArrayList<ScheduledFuture<?>>(){{
        add(FileHandlerPool.scheduleWithFixedDelay(
            new Parser.HotelSave(Hotelsto, HotelsTable), SAVE_INIT, SAVE_DELAY, TIME_DELAY
        ));
        add(FileHandlerPool.scheduleWithFixedDelay(
            new Parser.UsersSave(Usersto, UsersTable), SAVE_INIT, SAVE_DELAY, TIME_DELAY
        ));
        add(FileHandlerPool.scheduleWithFixedDelay(
            new Parser.ReviewsSave(Reviewsto, DumpingQueue, MAX_DUMP, DUMP_WAIT, DUMP_UNIT), SAVE_INIT, SAVE_DELAY, TIME_DELAY
        ));
        add(RankingPool.scheduleWithFixedDelay(
            RankManager.getInstance(), RANK_INIT, RANK_DELAY, TIME_DELAY
        ));}};
    }

    /**
     * Termina il contesto del server, annullando i task pianificati e attendendo il completamento dei task in corso.
     */
    public static void Terminate() {
        // Annulla tutti i task pianificati
        for (ScheduledFuture<?> task : ScheduledTasks) {
            task.cancel(false);
        }

        // Attende il completamento dei task già avviati
        for (ScheduledFuture<?> task : ScheduledTasks) {
            waitForTaskCompletion(task);
        }

        // Completa l'elaborazione del ranking e il salvataggio dei dati
        try{
        Future<?> rank = RankingPool.submit(RankManager.getInstance());
        FileHandlerPool.submit(new Parser.UsersSave(Usersto, UsersTable));
        waitForTaskCompletion(rank);
        FileHandlerPool.submit(new Parser.HotelSave(Hotelsto, HotelsTable));
        waitForTaskCompletion(FileHandlerPool.submit(new Parser.ReviewsSave(Reviewsto, DumpingQueue, MAX_DUMP, DUMP_WAIT, DUMP_UNIT)));
        FileHandlerPool.submit(new Parser.MergeReviews(Reviewsto));

        // Effettua la procedura di shutdown
        shutdownGracefully(POOL_AWAIT, AWAIT_UNIT, MainPool, FileHandlerPool, RankingPool);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // errore nella configurazione del server
            e.printStackTrace();
        }
    }

    /**
     * Arresta i thread pool in modo ordinato e attende la loro terminazione.
     * 
     * @param poolAwait Il tempo massimo da attendere per la terminazione dei pool.
     * @param unit L'unità di tempo per l'attesa.
     * @param poolList I thread pool da arrestare.
     */
    private static void shutdownGracefully(long poolAwait, TimeUnit unit, ExecutorService... poolList) {
        for (ExecutorService pool : poolList) {
            pool.shutdown();
        }

        for (ExecutorService pool : poolList) {
            try {
                if (!pool.awaitTermination(poolAwait, unit)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
                e.printStackTrace();
            }
        }
    }

    /**
     * Attende il completamento di un task e gestisce le eccezioni.
     * 
     * @param task Il task da attendere.
     */
    private static void waitForTaskCompletion(Future<?>... taskSet) {
        for (Future<?> task : taskSet){
            if(task == null) continue; 
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch(CancellationException e){
                //se la task è stata cancellata non succede nulla
            }
        }
    }
}
