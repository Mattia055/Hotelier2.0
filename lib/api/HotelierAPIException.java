package lib.api;

public class HotelierAPIException extends Exception {
    public HotelierAPIException(String message) {
        super(message);
    }

    public HotelierAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ConnectionException extends HotelierAPIException {
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

class CommunicationException extends HotelierAPIException {
    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ResponseParsingException extends HotelierAPIException {
    public ResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
