package serverUtil;

/**
 * Rappresenta un utente nel sistema con attributi come nome utente, impronta digitale, esperienza e grado.
 */
public class User {

    // Attributi dell'utente
    protected String username;
    private String fingerprint;
    private int experience;
    private Badge rank;

    /**
     * Enum che rappresenta i vari gradi di un utente con i punteggi minimi richiesti per ciascun grado.
     */
    public static enum Badge {
        REC     (0, "Recensore"),
        REC_ESP (500, "Recensore Esperto"),
        CON     (2000, "Contributore"),
        CON_ESP (5000, "Contributore Esperto"),
        CON_SUP (MAX_EXP, "Contributore Super");

        private final int minExperience;
        private final String description;

        Badge(int minExperience, String description) {
            this.minExperience = minExperience;
            this.description = description;
        }

        public int getMinExperience() {
            return minExperience;
        }

        @Override
        public String toString() {
            return description;
        }

        /**
         * Restituisce il grado corrispondente all'esperienza fornita.
         *
         * @param experience L'esperienza dell'utente.
         * @return Il grado corrispondente.
         */
        public static Badge getRankForExperience(int experience) {
            for (Badge badge : values()) {
                if (experience >= badge.getMinExperience()) {
                    return badge;
                }
            }
            return REC; // Default grade if none matches
        }
    }

    // Costanti per l'esperienza iniziale e il grado iniziale
    private static final int    INIT_EXP = 100;
    private static final Badge  INIT_BADGE = Badge.REC;
    public static final int     MAX_EXP = 10000;

    /**
     * Costruisce un nuovo oggetto User con nome utente e impronta digitale specificati.
     * L'esperienza iniziale è impostata a 100 e il grado iniziale è REC.
     *
     * @param username Il nome utente dell'utente.
     * @param fingerprint L'impronta digitale dell'utente.
     */
    public User(String username, String fingerprint) {
        this.username = username;
        this.fingerprint = fingerprint;
        this.experience = INIT_EXP;
        this.rank = INIT_BADGE;
    }

    /**
     * Restituisce il nome utente dell'utente.
     *
     * @return Il nome utente.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce l'impronta digitale dell'utente.
     *
     * @return L'impronta digitale.
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Restituisce il livello di esperienza dell'utente.
     *
     * @return Il livello di esperienza.
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Imposta il livello di esperienza dell'utente.
     *
     * @param experience Il nuovo livello di esperienza.
     * @throws IllegalArgumentException Se l'esperienza è negativa o supera il valore massimo.
     */
    public void setExperience(int experience) {
        if (experience < 0 || experience > MAX_EXP) {
            throw new IllegalArgumentException("L'esperienza deve essere compresa tra 0 e " + MAX_EXP);
        }
        this.experience = experience;
        this.rank = Badge.getRankForExperience(experience);
    }

    /**
     * Restituisce il grado dell'utente.
     *
     * @return Il grado.
     */
    public Badge getBadge() {
        return rank;
    }

    public boolean passwordTest(String compare){
        System.out.println("Fingerprint: " + fingerprint+ " Compare: " + compare);
        //stampa la lunghezza delle due stringhe
        System.out.println("Fingerprint length: " + fingerprint.length() + " Compare length: " + compare.length());
        return fingerprint.equals(compare);
    }
}
