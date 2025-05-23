package lib.client.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.share.packet.Request;
import lib.share.packet.Response;
import lib.share.packet.Request.Method;
import lib.share.security.HashUtils;
import lib.share.struct.HotelDTO;
import lib.share.struct.Score;
import lib.share.typeAdapter.ResponseTypeAdapter;

public class HotelierAPI {

    private String          serverAddress;
    private int             serverPort;
    private Gson            gson;
    private Socket          socket;
    private OutputStream    out;
    private InputStream     in;
    private boolean fetch_init = false;
    
    public HotelierAPI(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.gson = new  GsonBuilder()
                        .serializeNulls()
                        //.registerTypeAdapter(Request.class,new RequestTypeAdapter())
                        .registerTypeAdapter(Response.class,new ResponseTypeAdapter())
                        .create();
    }

    public void connect() throws ConnectionException {
        try{
            this.socket = new Socket(InetAddress.getByName(this.serverAddress), this.serverPort);
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
            //lettura bloccante [lungezza pacchetto]
            int bytesRead = in.read(lengthBuffer.array(), 0, Integer.BYTES);
            if (bytesRead != Integer.BYTES) {
                throw new CommunicationException("Failed to read the length of the incoming data", null);
            }
            int length = lengthBuffer.getInt();

            //alloca un byteArray per il pacchetto
            byte[] jsonBytes = new byte[length];
            bytesRead = 0;
            while (bytesRead < length) {
                //fa letture bloccanti
                int result = in.read(jsonBytes, bytesRead, length - bytesRead);
                if (result == -1) {
                    throw new CommunicationException("Connection closed before all data was received", null);
                }
                //se ha finito di leggere tutti i bytes esce dal ciclo
                bytesRead += result;
            }
            return new String(jsonBytes, "UTF-8");

        } catch (Exception e) {
            throw new CommunicationException("Failed to receive data from the server", e);
        }
    }


    private void sendRequest(Request request) throws CommunicationException {
        String jsonString = gson.toJson(request, Request.class);
        write(jsonString);
    }

    private Response getResponse() throws CommunicationException, ResponseParsingException {
        String jsonString = read();
        try {
            return gson.fromJson(jsonString, Response.class);
        } catch (Exception e) {
            throw new ResponseParsingException("Failed to parse response from server", e);
        }
    }

    public APIResponse HandleIsLogged() throws CommunicationException, ResponseParsingException {
        Request request = new Request(Method.IS_LOGGED,null);
        sendRequest(request);
        Response response = getResponse();
        return response.getStatus() == Response.Status.FAILURE ? 
                                        new APIResponse(response.getError()) : 
                                        new APIResponse(Status.OK);
    }

    private APIResponse HandleUserOperation(String username, String password, Method override) throws CommunicationException, ResponseParsingException {
        if(username == null || username.trim().isEmpty())
            return new APIResponse(Status.INVALID_PARAMETER, "The username is invalid: Allowed characters: (A-Z, a-z), (0-9), (_), (-)");
        else if(password == null || password.trim().isEmpty())
            return new APIResponse(Status.INVALID_PARAMETER, "Password cannot be empty");
        Request request = new Request(override, username.trim());
        sendRequest(request);

        Response response = getResponse();

        if(response.getStatus() == Response.Status.FAILURE){
            return new APIResponse(response.getError());
        }

        String salt = response.getData().toString();
        password = HashUtils.computeSHA256Hash(password, salt);

        //inserisce la password

        request.setData(password);
        sendRequest(request);
        response = getResponse();
        return new APIResponse(response.getError());
    }

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
                                        new APIResponse(Status.OK, (response.getData()));
        
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
        request = new Request(Method.REVIEW, RevScore);
        //meglio di usare un if statement con Status.OK tanto NO_ERR viene mappato in Status.OK
        sendRequest(request);        
        return new APIResponse(getResponse().getError());

    }

    public APIResponse HotelSearch(String City, String Hotel) throws CommunicationException, ResponseParsingException {
        /*API call to search for a hotel */
        Request request = new Request(Method.SEARCH_HOTEL, new String[]{City,Hotel});
        sendRequest(request);
        Response response = getResponse();
        return response.getStatus() == Response.Status.FAILURE ? 
                                        new APIResponse(response.getError()) : 
                                        new APIResponse(Status.OK, (HotelDTO) response.getData()  );  
    }

    public APIResponse HotelPeek(String City, String Hotel) throws CommunicationException, ResponseParsingException {
        /*API call to search for a hotel */
        Request request = new Request(Method.SEARCH_HOTEL, new String[]{City,Hotel});
        sendRequest(request);
        Response response = getResponse();
        return response.getStatus() == Response.Status.FAILURE ? 
                                        new APIResponse(response.getError()) : 
                                        new APIResponse(Status.OK);  
    }

    //public APIResponse HotelsFetch
    public APIResponse HotelsFetch(String City) throws CommunicationException, ResponseParsingException, NullPointerException {
        if(City != null) fetch_init = true;
        
        /*API call to fetch hotels */
        Request request = new Request(Method.SEARCH_ALL, City);
        sendRequest(request);
        Response response = getResponse();
        APIResponse apiResponse = new APIResponse(Status.OK);
        Response.Status res = response.getStatus();
        if(res == Response.Status.FAILURE){
           return new APIResponse(response.getError());
        }
        else if(res == Response.Status.SUCCESS){
            fetch_init = false;
            apiResponse.setStatus(Status.FETCH_DONE);
        }
        else{
            apiResponse.setStatus(Status.FETCH_LEFT);
        }
        apiResponse.setData(response.getData());

        return apiResponse;
    }

    /*Il metodo può essere chiamato solo se il metodo precedentemente invocato è HotelsFetch(City)*/
    
    public APIResponse HotelsFetch() throws CommunicationException, ResponseParsingException, InvalidMethodInvocation {
        if(!fetch_init) throw new InvalidMethodInvocation("Invoked HotelsFetch() and last method was not HotelsFetch(City) or HotelsFetchAll(City) fetched all hotels");
        return HotelsFetch(null);
    }

    public APIResponse HotelsFetchAll(String City) throws CommunicationException, ResponseParsingException, NullPointerException {
        APIResponse response = HotelsFetch(City);
        if(response.getStatus() == Status.NO_SUCH_CITY) return response;
        ArrayList<HotelDTO> hotels = new ArrayList<HotelDTO>();
        boolean success = false;
        try{
            while(response.getStatus() == Status.FETCH_LEFT){
                for(HotelDTO hotel : response.getHotelList()){
                    hotels.add(hotel);
                }
                response = HotelsFetch();
            }

            if(response.getStatus() == Status.FETCH_DONE){
                for(HotelDTO hotel : response.getHotelList()){
                    hotels.add(hotel);
                }
                success = true;
            }

        } catch(Exception e){
            e.printStackTrace();
        }

        return new APIResponse(success ? Status.FETCH_DONE : Status.FETCH_PARTIAL, hotels.toArray(new HotelDTO[0]));
        //Alla fine aggiungo il gruppo degli Hotel rimanenti
    }


    

}



    

