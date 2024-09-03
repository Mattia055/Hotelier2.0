package serverUtil;

import java.nio.channels.SelectionKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
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

public class RequestHandler implements Runnable{

    private static Gson     Gson;
    private static final EnumMap<Request.Method,BiFunction<Request,Session,Response>> HandlerTable;

    //hashmaps per la gestione delle richieste
    private static ConcurrentHashMap<String,User>                               UsersTable;
    private static ConcurrentHashMap<String,ArrayList<Hotel>>    HotelsTable;
    private static ConcurrentHashMap<Integer, ArrayList<Review>>                     ReviewsTable;
    private static ConcurrentHashMap<String,Boolean>                            LoggedTable;

    private static final int SALT_LENGTH;

    //parametri per il batch degli hotel
    private static final int DEF_BATCH_SIZE;

    private static final Pattern user_regex;

    static{

        Gson            = new   GsonBuilder()
                                .serializeNulls()
                                .registerTypeAdapter(Request.class,new RequestTypeAdapter())
                                .registerTypeAdapter(Response.class,new ResponseTypeAdapter())
                                .create();

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
        DEF_BATCH_SIZE  = ServerContext.MAX_BATCH_SIZE;
        SALT_LENGTH     = ServerContext.SALT_LENGTH;
        user_regex  = Pattern.compile(ServerContext.USERNAME_REGEX);




    }

    private SelectionKey    key;
    private Session         session;

    public RequestHandler(SelectionKey key) {

        //della chiave mi servono canale, sessione;
        this.key        = key;
        this.session    = (Session) key.attachment();

    }

    @Override
    public void run(){
        
        try{
            System.out.println("HANDLING REQUEST");
            Request requestObject = Gson.fromJson(session.getMessage(), Request.class);
            System.out.println(requestObject.getMethod().name());
            //resetto il buffer
            BiFunction<Request,Session,Response> handler = HandlerTable.get(requestObject.getMethod());
            Response response = (handler != null) ? handler.apply(requestObject, session) : new Response(Error.INVALID_REQUEST);
            //System.out.println("Risposta pronta");
            session.setMessage(Gson.toJson(response,Response.class));
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

    public static Response handleLogout(Request request, Session session) {
        try{
            LoggedTable.remove(session.Username);
        } catch(NullPointerException e){
            return new Response(Error.NOT_LOGGED);
        } finally{
            session.flush();
        }
        return new Response(Status.SUCCESS);
    }
    
    public static Response handleSearch(Request request, Session session) {
        String[] data = (String[]) request.getData();
        String city = data[0].toLowerCase().trim();
        String hotel = data[1].trim();
        
        ArrayList<Hotel> hmap = HotelsTable.get(city);
        if(hmap == null) return new Response(Error.NO_SUCH_CITY);
        synchronized(hmap){
            for(Hotel h : hmap){
                if(h.getName().equals(hotel)) return new Response(Status.SUCCESS,h.toDTO());
            }
            return new Response(Error.NO_SUCH_HOTEL);
        }
    }

    /*
     * La lista viene restituita al contrario quindi il rank manager deve ordinare
     * in ordine crescente
     */
    @SuppressWarnings("unchecked")
    public static Response handleSearchAll(Request request, Session session) {
        ArrayList<HotelDTO> toReturn = null;
        if(session.LastMethod != Method.SEARCH_ALL){
            String city = ((String) request.getData()).toLowerCase().trim();
            ArrayList<Hotel> hmap = HotelsTable.get(city);
            if(hmap == null) return new Response(Error.NO_SUCH_CITY);
            //creo una copia in locale della lista
            toReturn = null;
            synchronized(hmap){
                toReturn = new ArrayList<HotelDTO>(hmap.size());
                for(Hotel h : hmap){
                    System.out.println(h.getName());
                    toReturn.add(h.toDTO());
                }
            }
        }
        //ho cachato la copia quindi posso recuperarla dalla sessione
        else{
            //l'unico caso in cui la sessione contiene un arrayList
            if((session.getData() instanceof ArrayList<?>)){
            toReturn = (ArrayList<HotelDTO>) session.getData();
            } else return new Response(Error.BAD_SESSION);
        }
        int last_index = toReturn.size()-1;
        HotelDTO[] batch = new HotelDTO[DEF_BATCH_SIZE];
        //restituisco un batch di hotels
        for(int i = 0; i< DEF_BATCH_SIZE; i++){
            if(last_index == -1)
                break;
            batch[i] = toReturn.remove(last_index--);
        }
        //se ho finito di inviare tutti gli hotel pulisco la sessione
        if(last_index == -1){
            session.clearData();
            session.LastMethod = null;
            return new Response(Status.SUCCESS,batch);
        } else{
            return new Response(Status.AWAIT_INPUT,batch);
        }
        
    }
    public static Response handleShowBadge(Request request, Session session) {
        if(!LoggedTable.containsKey(session.Username))
            return new Response(Error.NOT_LOGGED);

        try{
            return new Response(Status.SUCCESS,UsersTable.get(session.Username).getBadge().toString());
        } catch(NullPointerException e){
            return new Response(Error.NO_SUCH_USER);
        }
    }
    
    public static Response handleExtLogout(Request request, Session session) {
        //prima invocazione
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
                    LoggedTable.remove(username);
                    return new Response(Status.SUCCESS);
                }
            } catch(NullPointerException e){
                return new Response(Error.NO_SUCH_USER);
            } 
            return new Response(Error.BAD_PASSWD);
        }
    }

    public static Response handleLogin(Request request, Session session){
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
            }
        }
    }

    public static Response handleRegister(Request request, Session session) {
        if(session.getMethod() != Method.REGISTER){
            String username = (String) request.getData();
            //check username validity
            if(!user_regex.matcher(username).matches())
                return new Response(Error.INVALID_PARAMETER);
            try{
                User u = UsersTable.get(username);
                if(u != null) 
                    return new Response(Error.USER_EXISTS);
                else{
                    u = new User(username, null, HashUtils.generateSalt(SALT_LENGTH));
                    session.Data = u;
                    session.LastMethod = Method.REGISTER;
                    return new Response(Status.AWAIT_INPUT,u.salt);
                }
            } catch (NullPointerException e){
                session.flush();
                return new Response(Error.INVALID_REQUEST);
            }
        }

        else{
            String password = null;
            try{
                password = (String) request.getData();
                if(password.length() != HashUtils.HASH_LEN){
                    return new Response(Error.INVALID_PARAMETER);
                }
            } catch(ClassCastException e){
                return new Response(Error.INVALID_REQUEST);
            }
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

    public static Response handleReview(Request request, Session session){
        if(session.getMethod() != Method.REVIEW){
            String username = session.Username;
            if(username == null || !LoggedTable.containsKey(username))
                return new Response(Error.MUST_LOGIN);
            
            String[] data = (String[]) request.getData();
            String city     = data[0].toLowerCase().trim();
            String hotel    = data[1].trim();
            
            ArrayList<Hotel> Hmap = HotelsTable.get(city);
            if(Hmap == null)    return new Response(Error.NO_SUCH_CITY);
            
            synchronized(Hmap){
                for(Hotel h : Hmap){
                    if(h.getName().equals(hotel)){
                        session.setData(h.getID());
                        session.setMethod(Method.REVIEW);
                        return new Response(Status.SUCCESS);
                    }
                }
                return new Response(Error.NO_SUCH_HOTEL);
            }
            
        }
        else{
            Object maybeScore = request.getData();
            if(!(maybeScore instanceof Score))
                return new Response(Error.INVALID_REQUEST);
            Score score = (Score) maybeScore;
            //check if the score is valid
            if(!score.isValidCleaning())      return new  Response(Error.SCORE_CLEANING);
            else if(!score.isValidGlobal())   return new  Response(Error.SCORE_GLOBAL);
            else if(!score.isValidPosition()) return new  Response(Error.SCORE_POSITION);
            else if(!score.isValidPrice())    return new  Response(Error.SCORE_PRICE);
            else if(!score.isValidService())  return new  Response(Error.SCORE_SERVICE);

            int user_exp, hotelID;
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
