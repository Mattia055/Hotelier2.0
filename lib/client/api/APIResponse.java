package lib.client.api;

import java.util.ArrayList;
import java.util.EnumMap;



import lib.share.packet.Response.Error;
import lib.share.struct.HotelDTO;

/**
 * Represents a response from an API operation.
 */
public class APIResponse {

    private static final EnumMap<Error,Status> statusMapping = new EnumMap<>(Error.class);

    static{
        statusMapping.put(Error.NO_SUCH_USER,   Status.NO_SUCH_USER     );
        statusMapping.put(Error.BAD_PASSWD,     Status.BAD_PASSWORD     );
        statusMapping.put(Error.USER_EXISTS,    Status.USER_EXISTS      );
        statusMapping.put(Error.MUST_LOGIN,     Status.MUST_LOGIN       );
        statusMapping.put(Error.NO_SUCH_CITY,   Status.NO_SUCH_CITY     );
        statusMapping.put(Error.NO_SUCH_HOTEL,  Status.NO_SUCH_HOTEL    );
        statusMapping.put(Error.ALREADY_LOGGED, Status.ALREADY_LOGGED   );
        statusMapping.put(Error.NOT_LOGGED,     Status.NOT_LOGGED       );
        statusMapping.put(Error.SCORE_GLOBAL,   Status.SCORE_GLOBAL     );
        statusMapping.put(Error.SCORE_POSITION, Status.SCORE_POSITION   );
        statusMapping.put(Error.SCORE_CLEANING, Status.SCORE_CLEANING   );
        statusMapping.put(Error.SCORE_PRICE,    Status.SCORE_PRICE      );
        statusMapping.put(Error.SCORE_SERVICE,  Status.SCORE_SERVICE    );
        statusMapping.put(Error.NO_ERR,         Status.OK               );
        statusMapping.put(Error.BAD_SESSION,    Status.SERVER_ERROR     );
    }

    private Status status;
    private String message;
    private Object data;

    /**
     * Constructs a new APIResponse.
     * 
     * @param status  The status of the API response.
     * @param message A message describing the response.
     * @param data    Any additional data related to the response.
     */

    protected APIResponse(Status status, String message, Object data) {
        this.status = status;
        this.message = status.getPhrase() + " : " + message;
        this.data = data;

    }    

    protected APIResponse(Error Error,String message, Object data) {
        this.status = statusMapping.get(Error);
        if(this.status == null){
            //API error
            throw new IllegalArgumentException("Error not mapped to a status");
        }
        
        this.data = data;
    }

    protected APIResponse(Error Error, Object data) {
        this(Error, null, data);
    }

    protected APIResponse(Status status, String message) {
        this(status, message, null);
    }

    protected APIResponse(Status status, Object data) {
        this(status, null, data);
    }

    protected APIResponse(Status status) {
        this(status, null, null);
    }

    protected APIResponse(Error Error) {
        this(Error, null, null);
    }

    protected void setMessage(){
        this.message = status.getPhrase() + " : " + message;
    }

    /**
     * Gets the status of the API response.
     * 
     * @return The status of the API response.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of the API response.
     * 
     * @param status The status to set.
     */
    protected void setStatus(Status status) {
        this.status = status;
        setMessage();
    }

    protected void setStatus(Error error){
        this.status = statusMapping.get(error);
        setMessage();
    }

    /**
     * Gets the message of the API response.
     * 
     * @return The message of the API response.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message of the API response.
     * 
     * @param message The message to set.
     */
    protected void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the data of the API response.
     * 
     * @return The data of the API response.
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the data of the API response.
     * 
     * @param data The data to set.
     */
    protected void setData(Object data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<HotelDTO> getHotelList() {
        try{
            return (ArrayList<HotelDTO>) data;
        } catch (ClassCastException e){
            return null;
        }

    }

    public String getString() {
        try{
            return (String) data;

        } catch (ClassCastException e){
            return null;
        }
    }


    /*Da cambiare
     * Aggiungere string builder per effettuare il casting del data Object in base allo status della richiesta
     * 
    */
    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("APIResponse{\n" +
                "\tstatus = " + status.name() +
                ",\n\tmessage = " + message +
                ",\n\tdata = ");

        if(data instanceof String)
            sb.append((String) data);
        else if(data instanceof HotelDTO[]){
            HotelDTO[] hotels = (HotelDTO[]) data;
            for (HotelDTO hotel : hotels) {
                sb.append(hotel.toString() + "\n");
            }
        }
        else if(data instanceof HotelDTO){
            sb.append(((HotelDTO) data).toString());
        }
        else sb.append("null\n}");
        return sb.toString();
    }
}

