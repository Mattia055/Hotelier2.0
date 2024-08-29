package lib.client.api;

public class APIException extends Exception {
    public APIException(String message) {
        super(message);
    }

    public APIException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ConnectionException extends APIException {
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

class CommunicationException extends APIException {
    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ResponseParsingException extends APIException {
    public ResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

class InvalidMethodInvocation extends APIException {
    public InvalidMethodInvocation(String message) {
        super(message);
    }
}
