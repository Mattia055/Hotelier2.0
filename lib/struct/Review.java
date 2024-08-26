package lib.struct;

import java.time.LocalDateTime;

/**
 * Rappresenta una recensione per un hotel.
 */
public class Review {

    private int HotelId;         // Identificativo dell'hotel recensito
    private String Username;     // Nome utente del recensore
    private int UserExp;         // Esperienza dell'utente (ad esempio, livello o punti)
    private Score ReviewScore;   // Punteggio della recensione
    private LocalDateTime Time;  // Data e ora della recensione

    /**
     * Costruttore per la classe Review.
     *
     * @param HotelId Identificativo dell'hotel recensito.
     * @param username Nome utente del recensore.
     * @param userExp Esperienza dell'utente.
     * @param time Data e ora della recensione.
     * @param revScore Punteggio della recensione.
     */
    public Review(int HotelId, String username, int userExp, LocalDateTime time, Score revScore) {
        this.HotelId = HotelId;
        this.Username = username;
        this.UserExp = userExp;
        this.ReviewScore = revScore;
        this.Time = time;
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
    public LocalDateTime getTime() {
        return Time;
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
