package lib.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lib.packet.Request;
import lib.packet.Response;
import lib.packet.Request.Method;
import lib.packet.Response.Status;
import lib.packet.Response.Error;

public class HotelierAPI {


    private String          serverAddress;
    private int             serverPort;
    private Gson            gson;
    private Socket          socket;
    private OutputStream    out;
    private InputStream     in;
    private static final Type RequestT;
    private static final Type ResponseT;

    static{
        RequestT    = new TypeToken<Request>(){}.getType();
        ResponseT   = new TypeToken<Response>(){}.getType();

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

        if(getResponse().getStatus() == Status.FAILURE){
            return new APIResponse(response.getError());
        }

        //inserisce la password

        request.setData(password.trim());
        sendRequest(request);
        
        return new APIResponse(getResponse().getError());
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

    public APIResponse UserLogout() throws CommunicationException, ResponseParsingException {
        sendRequest(new Request(Method.LOGOUT, null));

        return new APIResponse(getResponse().getError());
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

        if(response.getStatus() == Status.FAILURE){
            return new APIResponse(response.getError());
        }

        return new APIResponse(response.getError(), new String[]{(String) response.getData()});
        
    }

}



    

