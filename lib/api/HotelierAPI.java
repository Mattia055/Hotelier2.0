package lib.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lib.packet.Request;
import lib.packet.Response;
import lib.packet.Request.Method;
import lib.struct.HotelDTO;
import lib.struct.Score;

public class HotelierAPI {

    private String          serverAddress;
    private int             serverPort;
    private Gson            gson;
    private Socket          socket;
    private OutputStream    out;
    private InputStream     in;
    private static final Type RequestT;
    private static final Type ResponseT;
    private static final Type ScoreT;
    private static final Type HotelDTOListT;

    static{
        RequestT    = new TypeToken<Request>(){}.getType();
        ResponseT   = new TypeToken<Response>(){}.getType();
        ScoreT      = new TypeToken<Score>(){}.getType();
        HotelDTOListT = new TypeToken<HotelDTO[]>(){}.getType();


    }
    

    public HotelierAPI(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.gson = new Gson();
    }

    public void connect() throws ConnectionException {
        try{
            this.socket = new Socket(this.serverAddress, this.serverPort);
            //initialize output end
            this.out = socket.getOutputStream();
            this.in  = socket.getInputStream ();
        }catch(Exception e){
            throw new ConnectionException("Failed to connect to the server",e);
        }
        
    }

    public void disconnect() throws ConnectionException {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
            }
        } catch (Exception e) {
            throw new ConnectionException("Failed to disconnect from the server", e);
        }
    }

    private void write(String jsonString) throws CommunicationException {
        try {
            byte[] jsonBytes = jsonString.getBytes("UTF-8");

            ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
            lengthBuffer.putInt(jsonBytes.length);
            out.write(lengthBuffer.array(), 0, Integer.BYTES);

            out.write(jsonBytes);
            out.flush();
        } catch (Exception e) {
            throw new CommunicationException("Failed to send data to the server", e);
        }
    }

    private String read() throws CommunicationException {
        try {
            ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
            int bytesRead = in.read(lengthBuffer.array(), 0, Integer.BYTES);
            if (bytesRead != Integer.BYTES) {
                throw new CommunicationException("Failed to read the length of the incoming data", null);
            }

            int length = lengthBuffer.getInt();

            byte[] jsonBytes = new byte[length];
            bytesRead = 0;
            while (bytesRead < length) {
                int result = in.read(jsonBytes, bytesRead, length - bytesRead);
                if (result == -1) {
                    throw new CommunicationException("Connection closed before all data was received", null);
                }
                bytesRead += result;
            }

            return new String(jsonBytes, "UTF-8");

        } catch (Exception e) {
            throw new CommunicationException("Failed to receive data from the server", e);
        }
    }

    private void sendRequest(Request request) throws CommunicationException {
        String jsonString = gson.toJson(request, RequestT);
        write(jsonString);
    }

    private Response getResponse() throws CommunicationException, ResponseParsingException {
        String jsonString = read();
        try {
            return gson.fromJson(jsonString, ResponseT);
        } catch (Exception e) {
            throw new ResponseParsingException("Failed to parse response from server", e);
        }
    }

    private APIResponse HandleUserOperation(String username, String password, Method override) throws CommunicationException, ResponseParsingException {
        Request request = new Request(override, username.trim());
        sendRequest(request);

        Response response = getResponse();

        if(response.getStatus() == Response.Status.FAILURE){
            return new APIResponse(response.getError());
        }

        //inserisce la password

        request.setData(password.trim());
        sendRequest(request);
        
        return new APIResponse(lib.api.Status.OK);
    }

    /*
     * Sembra un duplicato di UserLogin ma più avanti inserirò la crittografia
     * della password e il codice cambierà
     */
    /* 
    public APIResponse UserRegister(String username, String password) throws CommunicationException, ResponseParsingException {
        // Example implementation of a user registration request
        Request request = new Request(Method.REGISTER, username.trim());
        sendRequest(request);

        Response response = getResponse();

        if(getResponse().getStatus() == Status.FAILURE){
            return new APIResponse(response.getError());
        }

        //inserisce la password

        request.setData(password.trim());
        sendRequest(request);
        
        return new APIResponse(getResponse().getError());

    }

    public APIResponse UserLogin(String username, String password) throws CommunicationException, ResponseParsingException {
        // Example implementation of a user login request
        Request request = new Request(Method.LOGIN, username.trim());
        sendRequest(request);

        Response response = getResponse();

        if(getResponse().getStatus() == Status.FAILURE){
            return new APIResponse(response.getError());
        }

        //inserisce la password

        request.setData(password.trim());
        sendRequest(request);
        
        return new APIResponse(getResponse().getError());
    }

    public APIResponse LogoutEverywhere() throws CommunicationException, ResponseParsingException {
        sendRequest(new Request(Method.EXT_LOGOUT, null));

        return new APIResponse(getResponse().getError());
    }*/


    public APIResponse UserLogin(String username, String password) throws CommunicationException, ResponseParsingException {
        return HandleUserOperation(username, password, Method.LOGIN);
    }

    public APIResponse UserRegister(String username, String password) throws CommunicationException, ResponseParsingException {
        return HandleUserOperation(username, password, Method.REGISTER);
    }

    public APIResponse LogoutEverywhere(String username, String password) throws CommunicationException, ResponseParsingException {
        return HandleUserOperation(username, password, Method.EXT_LOGOUT);
    }

    public APIResponse ShowUserBadge() throws CommunicationException, ResponseParsingException {
        sendRequest(new Request(Method.SHOW_BADGE, null));

        Response response = getResponse();

        return response.getStatus() == Response.Status.FAILURE ? 
                                        new APIResponse(response.getError()) : 
                                        new APIResponse(Status.OK, new String[]{(String) response.getData()});
        
    }

    public APIResponse UserLogout() throws CommunicationException, ResponseParsingException {
        sendRequest(new Request(Method.LOGOUT, null));

        return new APIResponse(getResponse().getError());
    }

    public APIResponse InsertReview(String City, String Hotel, Score RevScore) throws CommunicationException, ResponseParsingException {
        /*API call to insert a review */
        /*
         * First message to insert the City name and the hotel name
         * 
         * Prepara il pacchetto con payload [citta, Hotel] e casting a oggetto
         */
        Request request = new Request(Method.REVIEW, new String[]{City,Hotel});
        sendRequest(request);
        Response response = getResponse();
        if(response.getStatus() == Response.Status.FAILURE){
            return new APIResponse(response.getError());
        }
        
        /*Invio seconda richiesta inserendo il punteggio dell'hotel */
        request = new Request(Method.REVIEW, gson.toJson(RevScore,ScoreT));
        //meglio di usare un if statement con Status.OK tanto NO_ERR viene mappato in Status.OK
        return new APIResponse(getResponse().getError());

    }

    public APIResponse HotelSearch(String City, String Hotel) throws CommunicationException, ResponseParsingException {
        /*API call to search for a hotel */
        Request request = new Request(Method.SEARCH_HOTEL, new String[]{City,Hotel});
        sendRequest(request);
        Response response = getResponse();
        return response.getStatus() == Response.Status.FAILURE ? 
                                        new APIResponse(response.getError()) : 
                                        new APIResponse(Status.OK, new String[]{(String) response.getData()});  
    }

    //public APIResponse HotelsFetch
    public APIResponse HotelsFetch(String City) throws CommunicationException, ResponseParsingException, NullPointerException {
        if(City == null) throw new NullPointerException();
        /*API call to fetch hotels */
        Request request = new Request(Method.SEARCH_ALL, City);
        sendRequest(request);
        Response response = getResponse();

        if(response.getStatus() == Response.Status.FAILURE){
            return new APIResponse(response.getError());
        }
        return new APIResponse( response.getStatus() == Response.Status.SUCCESS ? 
                                Status.FETCH_DONE : Status.FETCH_LEFT,
                                (Object) gson.fromJson((String) response.getData(), HotelDTOListT));
    }

    /*Il metodo può essere chiamato solo se il metodo precedentemente invocato è HotelsFetch(City)*/
    
    public APIResponse HotelsFetch() throws CommunicationException, ResponseParsingException, InvalidMethodInvocation {
        //API call to fetch all hotels
        Request request = new Request(Method.SEARCH_ALL, null);
        sendRequest(request);
        Response response = getResponse();

        // search all può dare solo FAILURE a causa di
        // * BAD-SESSION in questo caso restituisce ServerError
        // * INVALID REQUEST in questo caso lancia InvalidMethodInvocation
        
        if( response.getStatus() == Response.Status.FAILURE ){
            if(response.getError() == Response.Error.BAD_SESSION)
                return new APIResponse(Status.SERVER_ERROR);

            else throw new InvalidMethodInvocation("Invoked HotelsFetch() and last method was not HotelsFetch(City) or HotelsFetchAll(City) fetched all hotels");
        }
        //non mi interessa controllare success o await input
        return new APIResponse( response.getStatus() == Response.Status.SUCCESS ? 
                                Status.FETCH_DONE : Status.FETCH_LEFT,
                                (Object) gson.fromJson((String) response.getData(), HotelDTOListT));
    }
    

    public APIResponse HotelsFetchAll(String City) throws CommunicationException, ResponseParsingException, NullPointerException {
        /*API call to fetch all hotels */
        /*Calls internally HotelsFetch(String || void) */
        APIResponse response = HotelsFetch(City);
        //se la prima risposta da errore allora restituisco errore
        if(response.getStatus() == Status.NO_SUCH_CITY) return response;
        ArrayList<HotelDTO> hotels = new ArrayList<HotelDTO>();

        try{
            while(response.getStatus() == Status.FETCH_LEFT || response.getStatus() == Status.FETCH_DONE){
                for(HotelDTO hotel : response.getHotelList())
                    hotels.add(hotel);
                    
                if(response.getStatus() == Status.FETCH_DONE)
                    break;
                response = HotelsFetch();
            }
        } catch(Exception e){
            //potrebbe essere Communication Exception o ResponseParsingException
            /*A me non interessa più di tanto perche se il primo ciclo while viene preso posso restituire 
             * FETCH_PARTIAL. Il primo ciclo while va sempre bene altrimenti non sarei qui
             */
        }

        return new APIResponse(Status.FETCH_PARTIAL, hotels.toArray(new HotelDTO[0]));
    }

    

}



    

