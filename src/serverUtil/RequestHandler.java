package serverUtil;

import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import lib.share.packet.*;
import lib.share.packet.Request.Method;
import lib.share.packet.Response.Status;
import lib.share.packet.Response.Error;
import lib.share.struct.HotelDTO;
import lib.share.struct.Score;

public class RequestHandler implements Runnable{

    private static final Type RequestT;
    private static final Type ResponseT;
    private static final Type HotelDTOT;
    private static final Type HotelDTOListT;
    private static final Type ScoreT;

    private static Gson     Gson;
    private static final EnumMap<Request.Method,BiFunction<Request,Session,Response>> HandlerTable;

    //hashmaps per la gestione delle richieste
    private static ConcurrentHashMap<String,User>                               UsersTable;
    private static ConcurrentHashMap<String,ConcurrentHashMap<String,Hotel>>    HotelsTable;
    private static ConcurrentHashMap<Integer, List<Review>>                     ReviewsTable;
    private static ConcurrentHashMap<String,Boolean>                            LoggedTable;

    //parametri per il batch degli hotel
    private static final int DEF_BATCH_SIZE;

    static{

        RequestT        = new TypeToken<Request>(){}.getType();
        ResponseT       = new TypeToken<Response>(){}.getType();
        HotelDTOT       = new TypeToken<Hotel>(){}.getType();
        HotelDTOListT   = new TypeToken<ArrayList<Hotel>>(){}.getType();
        ScoreT          = new TypeToken<Score>(){}.getType();
        Gson            = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        HandlerTable = new EnumMap<>(Request.Method.class);

        //inizializzo la HandlerTable
        HandlerTable.put(Method.REGISTER,       RequestHandler::handleRegister);
        HandlerTable.put(Method.LOGIN,          RequestHandler::handleLogin);
        HandlerTable.put(Method.LOGOUT,         RequestHandler::handleLogout);
        HandlerTable.put(Method.EXT_LOGOUT,     RequestHandler::handleExtLogout);
        HandlerTable.put(Method.SEARCH_HOTEL,   RequestHandler::handleSearch);
        HandlerTable.put(Method.SEARCH_ALL,     RequestHandler::handleSearchAll);
        HandlerTable.put(Method.REVIEW,         RequestHandler::handleReview);
        HandlerTable.put(Method.SHOW_BADGE,     RequestHandler::handleShowBadge);

        ServerContext.getInstance();


        //init delle tabelle hash per non
        //inizializzarle a ogni richiesta;
        UsersTable      = ServerContext.UsersTable;
        HotelsTable     = ServerContext.HotelsTable;
        ReviewsTable    = ServerContext.ReviewsTable;
        LoggedTable     = ServerContext.LoggedTable;

        //inizializzazione del batch size
        DEF_BATCH_SIZE = ServerContext.MAX_BATCH_SIZE;



    }

    private SelectionKey    key;
    private Session         session;
    private ClientBuffer    buffer;

    public RequestHandler(SelectionKey key) {

        //della chiave mi servono canale, sessione;
        this.key        = key;
        this.session    = (Session) key.attachment();
        this.buffer     = session.getBuffer();

    }

    @Override
    public void run(){
        
        try{
            System.out.println("HANDLING REQUEST");
            String str = buffer.extractString();
            Request requestObject = Gson.fromJson(str, RequestT);
            //resetto il buffer
            BiFunction<Request,Session,Response> handler = HandlerTable.get(requestObject.getMethod());
            Response response = (handler != null) ? handler.apply(requestObject, session) : new Response(Error.INVALID_REQUEST);
            //System.out.println("Risposta pronta");
            buffer.wrapString(Gson.toJson(response,ResponseT));
            System.out.println("FINISHED HANDLING");
            key.interestOps(SelectionKey.OP_WRITE);
            /*
             * chiamare wakeup sul selettore è per evitare la
             * race condition tra il thread che setta la nuova 
             * modalità del canale e il thread che gestisce il
             * selettore
             * 
             * Setting Interest Operations: When you set new interest operations on a SelectionKey 
             * (e.g., key.interestOps(SelectionKey.OP_WRITE);), it modifies 
             * the key's interest set.
             * Race Condition: If the selector is currently blocked in the select() 
             * method, it might not immediately recognize the change in interest 
             * operations. This can lead to a race condition where the selector 
             * thread might miss the new interest operation.
             * Calling wakeup(): By calling ServerMain.getSelector().wakeup();, you ensure that the selector 
             * immediately wakes up from the select() method. This forces the selector to re-evaluate the 
             * interest operations of all registered keys, including the one you just modified.
             */
            //ServerMain.getSelector().wakeup();
            
        } catch(Exception e) {
            key.cancel();
            e.printStackTrace();
        } finally{
            ServerMain.getSelector().wakeup();
        }
        return;
    }

    /*
     * I metodi sono stateful dato che utilizzano la sessione
     * per immagazzinare dati intermedi
     */

    /*
     * 2 - passaggi:
     * REGISTER username:
     * setta la sessione con l'username controllando la validità
     * - Manda risposta: SUCCESS o USER_TAKEN
     * 
     */
    public static Response handleRegister(Request request, Session session) {
        //controllo la validità dello username 2 volte

        //secondo passaggio
        if(session.getMethod() == Method.REGISTER){
            //inserisco l'utente nella hashTable;
            String password = (String) request.getData();
            User u = new User(session.Username,password);

            //effettuo il flush della sessione qualunque cosa succeda
            /*
             * se fallimento la procedura deve riniziare
             * se successo allora non devo mantenere dati dell'utente;
             */
            session.flush();
            if(UsersTable.putIfAbsent(u.username,u) == null){
                return new Response(Status.SUCCESS);
            }
            
            return new Response(Error.USER_EXISTS);
        }
        
        //controllo se l'utente esiste
        String username = (String) request.getData();
        if(UsersTable.get(username) == null){
            session.Username = username;
            session.LastMethod = Method.REGISTER;
            return new Response(Status.AWAIT_INPUT);
        }
        else{
            session.flush();
            return new Response(Error.USER_EXISTS);
        }

    }
    
    public static Response handleLogin(Request request, Session session) {
        /*
         * Due passaggi:
         * controllo validità username
         * controllo validità password
         */
        if(session.getMethod() == Method.LOGIN){
            //inserisco l'utente nella hashTable;
            String password = (String) request.getData();
            
            //controllo che la password corrisponda
            //non devo controllare che il valore sia null (gia fatto)
            boolean password_status = false;
            if(UsersTable.get(session.Username).passwordTest(password) == true){
                //non so se funziona il fast circuit
                password_status = true;
                if(LoggedTable.putIfAbsent(session.Username,true) == null){
                    return new Response(Status.SUCCESS);
                }
                //utente gia presente o gia loggato o qualunque altra cosa
            }
            //ripulisco la sessione
            session.flush();
            return new Response(password_status?
                                Error.ALREADY_LOGGED : 
                                Error.BAD_PASSWD);
        }
        
        //controllo se l'utente esiste e se è gia loggato
        String username = (String) request.getData();
        if(UsersTable.get(username) != null){
            if(LoggedTable.get(username) == null) {
                session.Username = username;
                session.LastMethod = Method.LOGIN;
                return new Response(Status.AWAIT_INPUT);
            }
            else return new Response(Error.ALREADY_LOGGED);
        }
        else return new Response(Error.NO_SUCH_USER);

    }
    
    public static Response handleLogout(Request request, Session session) {
        //solo un passaggio
        /*
         * se l'utente è loggato allora lo elimino
         * 
         * flush della sessione
         */
        if(session.Username != null) {
                     
            LoggedTable.remove(session.Username);
            return new Response(Status.SUCCESS);
        }
        else {
            session.flush();
            return new Response(Error.NOT_LOGGED);
        }
    }
    
    public static Response handleExtLogout(Request request, Session session) {
        /*funziona come un login ma nel caso
         * username e password coincidono allora elimina
         * la entry dalla tabella loggati;
         */
        if(session.getMethod() == Method.EXT_LOGOUT){
            //inserisco l'utente nella hashTable;
            String password = (String) request.getData();
            
            //controllo che la password corrisponda
            //non devo controllare che il valore sia null (gia fatto)
            boolean password_status = false;
            if(UsersTable.get(session.Username).passwordTest(password) == true){
                password_status = true;
                LoggedTable.remove(session.Username);
            }
            //ripulisco la sessione
            session.flush();
            return password_status? new Response(Status.SUCCESS) :
                                    new Response(Error.BAD_PASSWD);
        }
        
        //controllo se l'utente esiste e se è gia loggato
        String username = (String) request.getData();
        if(UsersTable.get(username) != null){
                session.Username = username;
                session.LastMethod = Method.EXT_LOGOUT;
                return new Response(Status.AWAIT_INPUT);
        }
        else return new Response(Error.NO_SUCH_USER);

    }
    
    public static Response handleSearch(Request request, Session session) {
        /*
         * Due passaggi:
         * riceve città e mette nella sessione (Session.Data)
         * 
         * Riceve nome hotel e mette nella sessione
         * nel caso uno dei due non esista,setta il metodo a 
         * null e ricomincia
         */
        if(session.LastMethod == Method.SEARCH_HOTEL){
            //effettua il casting a stringa della città () e cerca nella tabella hash
            //L'hotel, prima lo rende case insensitive
            String city     = (String) session.getData();
            String hotel    = ((String)request.getData()).toLowerCase().trim();
            
            //trova l'hotel, lo converte a JSON per portabilità e lo invia
            Hotel h = HotelsTable.get(city).get(hotel);
            return h != null?   new Response(Status.SUCCESS,Gson.toJson(h.toDTO(),HotelDTOT)) : 
                                new Response(Error.NO_SUCH_HOTEL);
        }

        //controllo se la città esiste
        String city = ((String)request.getData()).toLowerCase().trim();
        if(HotelsTable.get(city) != null){
            session.setData(city);
            session.setMethod(Method.SEARCH_HOTEL);
            return new Response(Status.AWAIT_INPUT);
        }
        else return new Response(Error.NO_SUCH_CITY);

    }
    
    @SuppressWarnings("unchecked")
    public static Response handleSearchAll(Request request, Session session) {
        /*
         * 2 passaggi:
         * controlla se la città esiste,
         * se esiste allora invia la lista degli hotel in formato JSON
         * in batches di DEF_BATCH_SIZE
         * 
         * Se è la prima richiesta allora setta il metodo a SEARCH_ALL
         * e memorizza l'iteratore della collection nella sessione;
         * 
         * Se non è la prima richiesta, controlla che il payload sia vuoto e in quel
         * caso elabora direttamente dalla sessione
         */
        Iterator<Hotel>  iterator   = null;
        ArrayList<HotelDTO> batch      = null;

        if(session.LastMethod != Method.SEARCH_ALL){
            if(request.getData() == null) return new Response(Error.INVALID_REQUEST);
            //inizializzo l'iteratore
            String city = ((String)request.getData()).toLowerCase().trim();
            if(HotelsTable.get(city) == null) return new Response(Error.NO_SUCH_CITY);
            iterator = HotelsTable.get(city).values().iterator();
            batch = new ArrayList<HotelDTO>(DEF_BATCH_SIZE);
            session.setMethod(Method.SEARCH_ALL);
        }
        
        
        //istanzio l'iteratore della collezione nella sessione
        /*
        * Devo controllare che l'iteratore sia di tipo Hotel
        */
        else{
            Iterator<?>  GenericIterator = (Iterator<?>) session.getData();
            Object nextElement = GenericIterator.next();
            if(!(nextElement instanceof Hotel)){
                session.clearMethod();
                session.clearData();
                return new Response(Error.BAD_SESSION);
            }
            iterator = (Iterator<Hotel>) GenericIterator;

            batch = new ArrayList<HotelDTO>(DEF_BATCH_SIZE){{
                add(((Hotel) nextElement).toDTO());
            }};  
        }
        
        boolean finished = true;
        for(int i = batch.size(); i <= DEF_BATCH_SIZE ; i++){
            if(iterator.hasNext()) batch.add(iterator.next().toDTO());
            else{
                session.clearMethod();
                session.clearData();
                finished = false;
                break;
            }
        }

        //Directly implemented in the first loop
        /*
        List<HotelDTO> DTObatch =  batch.stream()
                                        .map(Hotel::toDTO)
                                        .collect(Collectors.toList());
        */
        /*
        ArrayList<HotelDTO> DTObatch = new ArrayList<HotelDTO>();
        for(Hotel h : batch){
            DTObatch.add(h.toDTO());
        */

        return new Response(finished ? Status.SUCCESS : Status.AWAIT_INPUT,Gson.toJson(batch,HotelDTOListT));

    }
    
    public static Response handleReview(Request request, Session session) {
        /*se l'ultimo metodo invocato era REVIEW allora casta session.data a String[]
         * e inserisce la recensione nella tabella hash effettuando il casting del payload a Score
        */
        if(session.LastMethod == Method.REVIEW){
            
            Score score     = Gson.fromJson(request.getData().toString(),ScoreT);

            //controllo che i punteggi siano validi
            if(score.isValidCleaning()) return new  Response(Error.SCORE_CLEANING);
            if(score.isValidGlobal())   return new  Response(Error.SCORE_GLOBAL);
            if(score.isValidPosition()) return new  Response(Error.SCORE_POSITION);
            if(score.isValidPrice())    return new  Response(Error.SCORE_PRICE);
            if(score.isValidService())  return new  Response(Error.SCORE_SERVICE);

            int user_exp;
            int hotelID ;
            try{ 
                hotelID   = Integer.parseInt(session.getData().toString());
                user_exp  = UsersTable.get(session.Username).getExperience();
            }
            catch(NullPointerException | NumberFormatException e){
                // session.flush();
                // non devo fare il flush della sessione
                /*
                 * In teoria dovrei gestire il caso in cui l'utente non esiste
                 * dopo che la procedura di login viene effettuata con successo
                 */
                return new Response(e instanceof NullPointerException?
                                    Error.NO_SUCH_USER:
                                    Error.BAD_SESSION);
            }

            Review review   = new Review(hotelID,session.Username,user_exp,LocalDateTime.now(),score);
            //lazy loading della recensione
            /*
             * Utilizzando computeIfAbsent creo le liste mano a mano che aggiungo
             * recensioni (popolando la tabella hash). Le liste nella tabella 
             * hash sono sincronizzate tramite Collections.synchronizedList
             */
            List<Review> revsList = ReviewsTable.computeIfAbsent(hotelID, key -> {
                // Create a new synchronized list
                return Collections.synchronizedList(new ArrayList<Review>());
            });
            revsList.add(review);
            
            return new Response(Status.SUCCESS);
        }
        /*
         * prima chiamata a REVIEW, controllo che l'utente sia loggato,
         * in quel caso controllo che la citta e l'hotel esistano.
         * 
         * Se esistono allora setto il metodo a REVIEW e calcolo l'hotelID
         * memorizzandolo nella sessione
         */
        else {
            if(LoggedTable.get(session.Username) == null)
                return new Response(Error.NOT_LOGGED);
            
            String[] data = (String[]) request.getData();
            String city     = data[0].toLowerCase().trim();
            String hotel    = data[1].toLowerCase().trim();

            if(HotelsTable.get(city) == null)               return new Response(Error.NO_SUCH_CITY);
            if(HotelsTable.get(city).get(hotel) == null)    return new Response(Error.NO_SUCH_HOTEL);
            
            session.setData(HotelsTable.get(city).get(hotel).getID());
            session.setMethod(Method.REVIEW);
            return new Response(Status.AWAIT_INPUT);
        }
    }
    
    public static Response handleShowBadge(Request request, Session session) {
        /*
         * controlla se l'utente è loggato, in quel caso restituisce
         * il mnemonic del badge. Se non è loggato restituisce un errore
         */
        if(LoggedTable.get(session.Username) == null)
            return new Response(Error.NOT_LOGGED);

        try{
            return new Response(Status.SUCCESS,UsersTable.get(session.Username).getBadge().toString());

        } catch(NullPointerException e){
            return new Response(Error.NO_SUCH_USER);
        }
    }
    

    
}
