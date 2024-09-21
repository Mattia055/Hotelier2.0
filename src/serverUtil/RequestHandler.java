package serverUtil;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.share.packet.*;
import lib.share.packet.Request.Method;
import lib.share.packet.Response.Status;
import lib.share.security.HashUtils;
import lib.share.packet.Response.Error;
import lib.share.struct.HotelDTO;
import lib.share.struct.Score;
import lib.share.typeAdapter.RequestTypeAdapter;
import lib.share.typeAdapter.ResponseTypeAdapter;

/*
 * Classe che istanzia task che vengono eseguite dai 
 * thread del MainPool.
 * 
 * Gestiscono le richieste dei clients
 */
public class RequestHandler implements Runnable{

    private static Gson     Gson;
    private static final EnumMap<Request.Method,BiFunction<Request,Session,Response>> HandlerTable;

    //hashmaps per la gestione delle richieste
    private static ConcurrentHashMap<String,User>                   UsersTable;
    private static ConcurrentHashMap<String,ArrayList<Hotel>>       HotelsTable;
    private static ConcurrentHashMap<Integer, ArrayList<Review>>    ReviewsTable;
    private static ConcurrentHashMap<String,Boolean>                LoggedTable;

    private static final int SALT_LENGTH;
    private static final int DEF_BATCH_SIZE;
    private static final Pattern USER_REGEX;

    private static final Selector selector;

    static{

        Gson = new  GsonBuilder()
                    .serializeNulls()
                    .registerTypeAdapter(Request.class, new RequestTypeAdapter() )
                    .registerTypeAdapter(Response.class,new ResponseTypeAdapter())
                    .create();

        HandlerTable = new EnumMap<>(Request.Method.class);

        /*
         * La handler Table associa ogni metodo dei pacchetti a un Handler
         */
        HandlerTable.put(Method.REGISTER,       RequestHandler::handleRegister  );
        HandlerTable.put(Method.LOGIN,          RequestHandler::handleLogin     );
        HandlerTable.put(Method.LOGOUT,         RequestHandler::handleLogout    );
        HandlerTable.put(Method.EXT_LOGOUT,     RequestHandler::handleExtLogout );
        HandlerTable.put(Method.IS_LOGGED,      RequestHandler::handleIsLogged  );
        HandlerTable.put(Method.PEEK_HOTEL,     RequestHandler::handlePeek      );
        HandlerTable.put(Method.SEARCH_HOTEL,   RequestHandler::handleSearch    );
        HandlerTable.put(Method.SEARCH_ALL,     RequestHandler::handleSearchAll );
        HandlerTable.put(Method.REVIEW,         RequestHandler::handleReview    );
        HandlerTable.put(Method.SHOW_BADGE,     RequestHandler::handleShowBadge );

        /*
         * Si salvano i riferimenti alle tabelle
         * reperendoli da ServerContext
         */
        UsersTable      = ServerContext.UsersTable;
        HotelsTable     = ServerContext.HotelsTable;
        ReviewsTable    = ServerContext.ReviewsTable;
        LoggedTable     = ServerContext.LoggedTable;

        //Dimensione massima del batch di Hotel [handleSearchAll]
        DEF_BATCH_SIZE  = ServerContext.MAX_BATCH_SIZE;
        //Lunghezza del salt
        SALT_LENGTH     = ServerContext.SALT_LENGTH;
        //Regex per il controllo dell'username
        USER_REGEX  = Pattern.compile(ServerContext.USERNAME_REGEX);

        //Si ottiene il selettore
        selector = ServerMain.getSelector();

    }

    //Parametri dell'Istanza
    private SelectionKey    key;
    private Session         session;

    public RequestHandler(SelectionKey key) {
        
        this.key        = key;
        this.session    = (Session) key.attachment();

    }

    @Override
    public void run(){
        try{
            //Deserializzazione della richiesta
            Request requestObject = Gson.fromJson(session.getMessage(), Request.class);
            System.out.println("[MainPool] gestione di "+ requestObject.getMethod().name());

            //si ottiene l'Handler dalla Handler Table
            BiFunction<Request,Session,Response> handler = HandlerTable.get(requestObject.getMethod());
            //L'Handler viene eseguito
            Response response = (handler != null) ? handler.apply(requestObject, session) : new Response(Error.INVALID_REQUEST);
            //Serializzazione della risposta e salvataggio
            session.setMessage(Gson.toJson(response,Response.class));
            key.interestOps(SelectionKey.OP_WRITE);
            
        } catch(Exception e) {
            //In caso di errore la registrazione della chiave
            //viene annullata
            key.cancel();
            e.printStackTrace();
        } finally{
            //Il selettore viene risvegliato in ogni caso
            selector.wakeup();
        }
    }


    /*
     * Metodi per la gestione delle richieste
     */

    
    private static Response handleLogout(Request request, Session session) {
        try{
            LoggedTable.remove(session.Username);
        } catch(NullPointerException e){
            return new Response(Error.NOT_LOGGED);
        } finally{
            session.flush();
        }
        return new Response(Status.SUCCESS);
    }

    /*
     * Restituisce un pacchetto FAILURE se l'utente non è loggato
     * 
     * Metodo non utilizzato nell'implementazione attuale
     */
    private static Response handleIsLogged(Request request, Session session) {
        try{
            if(LoggedTable.containsKey(session.Username))
                return new Response(Status.SUCCESS);
        }
        catch(NullPointerException e){
            session.flush();
        }
        return new Response(Error.NOT_LOGGED);
    }
    

    /*
     * Restituisce un pacchetto SUCCESS se l'hotel è presente nella città
     * specificata
     * 
     * Il pacchetto di risposta (se successo) contiene l'HotelDTO (Data Transfer Object)
     */
    private static Response handleSearch(Request request, Session session) {
        String[] data   = (String[]) request.getData();

        //I campi sono case insensitive
        String city     = data[0].toLowerCase().trim();
        String hotel    = data[1].toLowerCase().trim();
        
        ArrayList<Hotel> hmap = HotelsTable.get(city);
        if(hmap == null) return new Response(Error.NO_SUCH_CITY);
        /*
        *    La ricerca sulla lista è sincronizzata perche il 
        *    rank manager potrebbe modificare l'ordine
        */
        synchronized(hmap){
            for(Hotel h : hmap){
                if(h.name.toLowerCase().equals(hotel)) 
                    return new Response(Status.SUCCESS,h.toDTO());
            }
            return new Response(Error.NO_SUCH_HOTEL);
        }
    }

    /*
     * Analogo a handleSearch ma non restituisce l'hotel
     */
    private static Response handlePeek(Request request, Session session) {
        String[] data = (String[]) request.getData();
        String city = data[0].toLowerCase().trim();
        String hotel = data[1].trim();
        
        ArrayList<Hotel> hmap = HotelsTable.get(city);
        if(hmap == null) return new Response(Error.NO_SUCH_CITY);
        synchronized(hmap){
            for(Hotel h : hmap){
                if(h.name.toLowerCase().equals(hotel)) return new Response(Status.SUCCESS);
            }
            return new Response(Error.NO_SUCH_HOTEL);
        }
    }

    /*
     * Metodo che restituisce un batch di Hotels.
     * 
     * Utilizza la sessione per memorizzare l'intera lista e la restituisce al client
     * in batch di dimensione DEF_BATCH_SIZE di HotelDTO.
     * 
     * Se prima invocazione setta LastMethod
     * 
     * Per ogni invocazione salva la lista dei rimanenti
     */

    @SuppressWarnings("unchecked")
    private static Response handleSearchAll(Request request, Session session) {
        //Utilizza una coda per memorizzare la lista
        Queue<HotelDTO> Snapshot = null;

        //Prima invocazione
        if( session.LastMethod != Method.SEARCH_ALL){
            Snapshot = new LinkedList<HotelDTO>();
            String city = ((String) request.getData()).toLowerCase().trim();
            ArrayList<Hotel> hmap = HotelsTable.get(city);
            if(hmap == null) return new Response(Error.NO_SUCH_CITY);
            /*
             * Crea uno snapshot della lista
             */
            synchronized(hmap){
                for(Hotel h : hmap){
                    Snapshot.add(h.toDTO());
                }
            }
            // 
            session.setMethod(Method.SEARCH_ALL);
            session.setData(Snapshot);
        }
        //recupera la lista dall'iteratore
        else{
            /*
             * Errori della sessione. Questo if non dovrebbe mai essere falso
             */
            if((session.getData() instanceof LinkedList<?>))
                Snapshot = (LinkedList<HotelDTO>) session.getData();

            else return new Response(Error.BAD_SESSION);
        }

        HotelDTO[] batch = new HotelDTO[DEF_BATCH_SIZE];
        //restituisco un batch di hotels
        for(int i = 0; i< DEF_BATCH_SIZE; i++){
            batch[i] = Snapshot.poll(); //La lista è ordinata [decrescente]
            if(batch[i] == null) break;
        }

        //Se la lista è vuota, la sessione viene pulita
        if(Snapshot.isEmpty()){
            session.clearData();
            session.clearMethod();
            session.LastMethod = null;
            return new Response(Status.SUCCESS,batch);
        } else return new Response(Status.AWAIT_INPUT,batch);
    
    }
    
    /*
     * Restituisce il badge dell'utente se questo è loggato
     */
    private static Response handleShowBadge(Request request, Session session) {
        if(!LoggedTable.containsKey(session.Username))
            return new Response(Error.NOT_LOGGED);
        try{
            return new Response(Status.SUCCESS,UsersTable.get(session.Username).getBadge().toString());
        } catch(NullPointerException e){    //l'eccezione non dovrebbe mai essere sollevata
            return new Response(Error.NO_SUCH_USER);
        }
    }
    
    /*
     * Effettua il Logout da tutti i dispositivi
     * 
     * Metodo a due passaggi
     */
    private static Response handleExtLogout(Request request, Session session) {
        // Prima invocazione del metodo 
        if(session.LastMethod != Method.EXT_LOGOUT){
            String username = (String) request.getData();
            try{
                User u = UsersTable.get(username);
                if(u != null){
                    session.Data = username;
                    session.LastMethod = Method.EXT_LOGOUT;
                    return new Response(Status.AWAIT_INPUT,u.salt);
                }
            } catch(NullPointerException e){
                return new Response(Error.INVALID_REQUEST);
            } return new Response(Error.NO_SUCH_USER);
        }
        //seconda invocazione
        else{
            String password = (String) request.getData();
            String username = (String) session.Data;
            session.flush();
            try{
                User u = UsersTable.get(username);
                if(u.passwordTest(password)){
                    return LoggedTable.remove(username) == null ? 
                       new Response(Error.NOT_LOGGED) : 
                       new Response(Status.SUCCESS);
                }
            } catch(NullPointerException e){ //l'eccezione non dovrebbe mai essere sollevata
                return new Response(Error.NO_SUCH_USER);
            } finally{
                session.clearData();
                session.clearMethod();
            }
            return new Response(Error.BAD_PASSWD);
        }
    }

    /*
     * Effettua il login dell'utente
     * 
     * Metodo a due passaggi
     */
    private static Response handleLogin(Request request, Session session){
        //prima invocazione
        if(session.getMethod() != Method.LOGIN){
            String username = (String) request.getData();
            try{
                User u = UsersTable.get(username);
                if(u == null) 
                    return new Response(Error.NO_SUCH_USER);
                else if(LoggedTable.containsKey(username)) 
                    return new Response(Error.ALREADY_LOGGED);
                else{
                    session.Data = username;
                    session.LastMethod = Method.LOGIN;
                    return new Response(Status.AWAIT_INPUT,u.salt);
                }
            } catch (NullPointerException e){
                return new Response(Error.INVALID_REQUEST);
            }
        }
        //seconda invocazione  
        else{
            String password = (String) request.getData();
            String username = (String) session.Data;
            try {
                User u = UsersTable.get(username);
                if(LoggedTable.containsKey(username)) 
                    return new Response(Error.ALREADY_LOGGED);
                else if(u.passwordTest(password)){
                    LoggedTable.put(username,true);
                    session.Username = username;
                    return new Response(Status.SUCCESS);
                }
                else return new Response(Error.BAD_PASSWD);
            } catch (NullPointerException e){
                return new Response(Error.INVALID_REQUEST);
            } finally{
                session.clearData();
                session.clearMethod();
            }
        }
    }

    /*
     * Effettua la registrazione dell'utente
     * 
     * Metodo a due passaggi
     * 
     * Genera un salt e lo invia al client per eseguire l'hash
     */
    private static Response handleRegister(Request request, Session session) {
        //prima comunicazione
        if(session.getMethod() != Method.REGISTER){
            String username = (String) request.getData();
            //controlla validità username
            if(!USER_REGEX.matcher(username).matches())
                return new Response(Error.INVALID_PARAMETER);
            try{
                User u = UsersTable.get(username);
                if(u != null) 
                    return new Response(Error.USER_EXISTS);
                else{
                    //genera il salt e lo invia al client
                    u = new User(username, null, HashUtils.generateSalt(SALT_LENGTH));
                    //salva l'utente nella sessione
                    session.Data = u;
                    session.LastMethod = Method.REGISTER;
                    return new Response(Status.AWAIT_INPUT,u.salt);
                }
            } catch (NullPointerException e){
                session.flush();
                return new Response(Error.INVALID_REQUEST);
            }
        }
        //seconda comunicazione
        else{
            String password = null;
            try{
                //controlla validità dell'Hash generato dal client
                password = (String) request.getData();
                if(password.length() != HashUtils.HASH_LEN){
                    return new Response(Error.INVALID_PARAMETER);
                }
            } catch(ClassCastException e){
                return new Response(Error.INVALID_REQUEST);
            }
            //effettua il retreival dell'utente dalla sessione e lo salva nella mappa
            User u = (User) session.Data;
            u.setFingerprint(password);
            try {
                if(UsersTable.putIfAbsent(u.username,u) == null) 
                    return new Response(Status.SUCCESS);
                else return new Response(Error.USER_EXISTS);
            } catch (NullPointerException e){
                return new Response(Error.BAD_SESSION);
            } finally{
                session.flush();
            }
        }
    }

    /*
     * Inserice una recensione per un hotel
     * 
     * Metodo a due passaggi
     * 
     * Se la recensione è valida, viene inserita nella mappa
     */
    private static Response handleReview(Request request, Session session){
        //prima invocazione
        if(session.getMethod() != Method.REVIEW){
            String username = session.Username;
            //controllo che l'utente sia loggato
            if(username == null || !LoggedTable.containsKey(username))
                return new Response(Error.MUST_LOGIN);
            
            String[] data = (String[]) request.getData();
            String city     = data[0].toLowerCase().trim();
            String hotel    = data[1].toLowerCase().trim();
            
            ArrayList<Hotel> Hmap = HotelsTable.get(city);
            if(Hmap == null)    return new Response(Error.NO_SUCH_CITY);
            
            synchronized(Hmap){
                for(Hotel h : Hmap){
                    if(h.name.toLowerCase().equals(hotel)){
                        session.setData(h.id);
                        session.setMethod(Method.REVIEW);
                        return new Response(Status.SUCCESS);
                    }
                }
                return new Response(Error.NO_SUCH_HOTEL);
            }
            
        }
        //seconda invocazione
        else{
            Object maybeScore = request.getData();
            if(!(maybeScore instanceof Score))
                return new Response(Error.INVALID_REQUEST); //non dovrebbe mai succedere
            Score score = (Score) maybeScore;
            //Controlla la validità del punteggio
            if      (!score.isValidCleaning())  return new  Response(Error.SCORE_CLEANING);
            else if (!score.isValidGlobal())    return new  Response(Error.SCORE_GLOBAL);
            else if (!score.isValidPosition())  return new  Response(Error.SCORE_POSITION);
            else if (!score.isValidPrice())     return new  Response(Error.SCORE_PRICE);
            else if (!score.isValidService())   return new  Response(Error.SCORE_SERVICE);

            int user_exp, hotelID;

            //recupera l'esperienza dell'utente e l'ID dell'hotel
            try{
                hotelID = (int) session.getData();
                user_exp = UsersTable.get(session.Username).getExperience();
            } catch(NullPointerException e){
                return new Response(Error.NO_SUCH_USER);
            } catch(ClassCastException e){
                return new Response(Error.BAD_SESSION);
            }
            try{
                ReviewsTable.compute(hotelID,(key,value) -> {
                    //la lista delle recensioni viene cancellata dal rank manager periodicamente
                    if(value == null) value = new ArrayList<Review>();
                    value.add(new Review(hotelID,session.Username,user_exp,LocalDateTime.now(),score));
                    return value;
                });
                return new Response(Status.SUCCESS);
            } catch(Exception e){
                return new Response(Error.SERVER_ERROR);
            } finally{
                session.clearData();
                session.clearMethod();
            }

        }
        
    }

    
    
}
