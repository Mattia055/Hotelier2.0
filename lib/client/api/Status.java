package lib.client.api;

import lib.share.packet.Response.Error;

public enum Status {
        OK              ("The operation was successful"),
        NO_SUCH_USER    (Error.NO_SUCH_USER.getMnemonic()),
        BAD_PASSWORD    (Error.BAD_PASSWD.getMnemonic()),
        USER_EXISTS     (Error.USER_EXISTS.getMnemonic()),
        MUST_LOGIN      (Error.MUST_LOGIN.getMnemonic()),
        NO_SUCH_CITY    (Error.NO_SUCH_CITY.getMnemonic()),
        NO_SUCH_HOTEL   (Error.NO_SUCH_HOTEL.getMnemonic()),
        ALREADY_LOGGED  (Error.ALREADY_LOGGED.getMnemonic()),
        NOT_LOGGED      (Error.NOT_LOGGED.getMnemonic()),
        SCORE_GLOBAL    (Error.SCORE_GLOBAL.getMnemonic()),
        SCORE_POSITION  (Error.SCORE_POSITION.getMnemonic()),
        SCORE_CLEANING  (Error.SCORE_CLEANING.getMnemonic()),
        SCORE_PRICE     (Error.SCORE_PRICE.getMnemonic()),
        SCORE_SERVICE   (Error.SCORE_SERVICE.getMnemonic()),
        SERVER_ERROR    ("An error occurred on the server"),
        FETCH_DONE      ("[HotelsFetch(String City ) | HotelsFetch()] Fetched all Hotels"),
        FETCH_LEFT      ("[HotelsFetch(String City ) | HotelsFetch()] Hotels left to fetch"),
        FETCH_PARTIAL   ("[HotelsFetchAll(String City)] Partial fetch of Hotels due to Server Error");
        


        private final String StatusPhrase;

        Status(String StatusPhrase) {
            this.StatusPhrase = StatusPhrase;
        }

        public String getPhrase(){
                return StatusPhrase;
        }
        
}
