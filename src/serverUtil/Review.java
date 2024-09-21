package serverUtil;

import lib.share.struct.Score;
import java.time.LocalDateTime;



/*
 * Rappresenta una recensione di un Hotel
 */
public class Review {

    protected int HotelId;              // Identificativo dell'hotel recensito
    protected String Username;          // Nome utente del recensore
    protected int UserExp;              // Esperienza dell'utente (ad esempio, livello o punti)
    protected Score ReviewScore;        // Punteggio della recensione
    protected LocalDateTime Added;      // Data e ora della recensione
    protected LocalDateTime Counted;

    /**
     * Costruttore per la classe Review.
     *
     * @param HotelId   Identificativo dell'hotel recensito.
     * @param username  Nome utente del recensore.
     * @param userExp   Esperienza dell'utente.
     * @param time      Data e ora della recensione.
     * @param revScore  Punteggio della recensione.
     */
    public Review(int HotelId, String username, int userExp, LocalDateTime time, Score revScore) {
        this.HotelId        = HotelId;
        this.Username       = username;
        this.UserExp        = userExp;
        this.ReviewScore    = revScore;
        this.Added          = time;
        this.Counted    = null;
    }

    /**
     * Ottiene l'identificativo dell'hotel recensito.
     *
     * @return L'identificativo dell'hotel.
     */
    public Integer getHotelId() {
        return HotelId;
    }

    /**
     * Ottiene il nome utente del recensore.
     *
     * @return Il nome utente del recensore.
     */
    public String getUsername() {
        return Username;
    }

    /**
     * Ottiene l'esperienza dell'utente.
     *
     * @return L'esperienza dell'utente.
     */
    public int getUserExp() {
        return UserExp;
    }

    /**
     * Ottiene la data e l'ora della recensione.
     *
     * @return La data e ora della recensione.
     */
    public LocalDateTime getAdded() {
        return Added;
    }

    /*
     * Setter del Timestamp di conteggio della recensione
     * Viene settato dal RankManager quando la recensione 
     * viene conteggiata
     */
    protected void setCounted(LocalDateTime time){
        Counted = time;
    }

    protected void SetCounted(){
        Counted = LocalDateTime.now();
    }

    /**
     * Ottiene il punteggio della recensione.
     *
     * @return Il punteggio della recensione.
     */
    public Score getReviewScore() {
        return ReviewScore;
    }
}
