package lib.api;

import java.util.EnumMap;

import lib.packet.Response.Error;

/**
 * Represents a response from an API operation.
 */
public class APIResponse {

    public enum Status {
        OK,
        NO_SUCH_USER,
        BAD_PASSWORD,
        USER_EXISTS,
        MUST_LOGIN,
        NO_SUCH_CITY,
        NO_SUCH_HOTEL,
        ALREADY_LOGGED,
        NOT_LOGGED,
        SCORE_GLOBAL,
        SCORE_POSITION,
        SCORE_CLEANING,
        SCORE_PRICE,
        SCORE_SERVICE,
        SERVER_ERROR;  
    }

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
    protected APIResponse(Error Error,String message, String[] data) {
        this.status = statusMapping.get(Error);
        if(this.status == null){
            //API error
            throw new IllegalArgumentException("Error not mapped to a status");
        }
        this.message = status == Status.OK? "The operation was successful" :
                                            Error.getMnemonic() + ": " + message;
        this.data = data;
    }

    protected APIResponse(Error Error, String[] data) {
        this(Error, null, data);
    }

    protected APIResponse(Error Error) {
        this(Error, null, null);
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

    @Override
    public String toString() {
        return "APIResponse{" +
                "status =" + status +
                ", message ='" + message + '\'' +
                ", data =" + data +
                '}';
    }
}

