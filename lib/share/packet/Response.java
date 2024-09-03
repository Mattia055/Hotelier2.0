package lib.share.packet;
import lib.share.struct.Score;

/**
 * Represents a response from the server, including status and error codes.
 */
public class Response {

    /**
     * Enum representing the possible status codes for a response.
     */
    public static enum Status {
        READY,        // Indicates that server is ready for requests  
        SUCCESS,      // Indicates that the operation was successful
        FAILURE,      // Indicates that the operation failed
        AWAIT_INPUT,  // Indicates that additional input is required
        AWAIT_ELAB    // Indicates that the data in payload is significant  
    }

    /**
     * Enum representing various error codes that can be included in a response.
     * Each error code has a corresponding mnemonic message.
     */
    public enum Error {

        NO_SUCH_USER        ("The user you are trying to access does not exist"),
        BAD_PASSWD          ("The password you entered is incorrect"),
        USER_EXISTS         ("A user with this username already exists"),
        MUST_LOGIN          ("You need to log in to perform this action"),
        NO_SUCH_CITY        ("The specified city could not be found"),
        NO_SUCH_HOTEL       ("The specified hotel could not be found"),
        INVALID_REQUEST     ("The provided request does not meet the required format or constraints"),
        NO_ERR              ("No error occurred. Everything is functioning as expected."),
        ALREADY_LOGGED      ("User has already logged in from another device"),
        NOT_LOGGED          ("User is not logged in"),
        SECUR_ALREADY_SET   ("Probe error used to see if user setted his security questions"),
        BAD_SESSION         ("The session is not valid or has expired"),
        SCORE_GLOBAL        ("The global score needs to be between "    + Score.getMin() + " and " + Score.getMax()),
        SCORE_POSITION      ("The position score needs to be between "  + Score.getMin() + " and " + Score.getMax()),
        SCORE_CLEANING      ("The cleaning score needs to be between "  + Score.getMin() + " and " + Score.getMax()),
        SCORE_PRICE         ("The price score needs to be between "     + Score.getMin() + " and " + Score.getMax()),
        SCORE_SERVICE       ("The service score needs to be between "   + Score.getMin() + " and " + Score.getMax()),
        INVALID_PARAMETER   ("Parameter invalid"),
        SERVER_ERROR        ("An error occurred on the server. Your request could not be processed.");

        private final String mnemonic;

        /**
         * Constructs an Error with a specific mnemonic message.
         *
         * @param mnemonic The mnemonic message associated with the error.
         */
        Error(String mnemonic) {
            this.mnemonic = mnemonic;
        }

        /**
         * Returns the mnemonic message for this error code.
         *
         * @return The mnemonic message.
         */
        public String getMnemonic() {
            return mnemonic;
        }
    }

    private Status status;         // The status of the response
    private Error error;           // The error code, if any
    private Object payload;        // Additional data for the response

    /**
     * Constructs a Response with the specified status and error code.
     * 
     * @param status The status of the response.
     * @param error The error code associated with the response.
     * @param Data The additional data to include with the response.
     */
    public Response(Status status, Error error, Object Data) {
        if (status == Status.FAILURE && error == Error.NO_ERR) {
            throw new IllegalArgumentException("Response Status.FAIL and no error set");
        }

        this.status = status;
        this.error = error;
        this.payload = (error != Error.NO_ERR) ? null : Data;
    }

    public Response(Status status) {
        this(status, Error.NO_ERR, null);
    }

    public Response(Status status, Object Data) {
        this(status, Error.NO_ERR, Data);
    }

    public Response(Error error) {
        this(Status.FAILURE, error, null);
    }

    public void update(Status status, Error error, Object Data) {
        if (status != Status.FAILURE && error == Error.NO_ERR) {
            throw new IllegalArgumentException("Response Status.FAIL and no error set");
        }

        this.status = status;
        this.error = error;
        this.payload = (error != Error.NO_ERR) ? null : Data;
    }

    public void update(Status status, Object Data) {
        update(status, Error.NO_ERR, Data);
    }

    public void update(Error error) {
        update(Status.FAILURE, error, null);
    }

    public void setStatus(Status newStatus) {
        if (newStatus == Status.FAILURE && error == Error.NO_ERR) {
            throw new IllegalArgumentException("Illegal assignment of Status.FAIL without assigning Error Code");
        }
        this.status = newStatus;
    }

    public void setError(Error error) {
        if (error == Error.NO_ERR) {
            throw new IllegalArgumentException("Illegal assignment of NO_ERR");
        }
        this.status = Status.FAILURE;
        this.error = error;
    }

    public Status getStatus() {
        return status;
    }

    public Error getError() {
        return error;
    }

    public Object getData() {
        return payload;
    }

}
