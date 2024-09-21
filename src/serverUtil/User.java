package serverUtil;

/**
 * Rappresenta un utente nel sistema con attributi come nome utente, impronta digitale, esperienza e grado.
 */
public class User {

    // Attributi dell'utente
    protected String username;
    private String fingerprint;
    protected String salt;
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
            Badge retval = null;
            for (Badge badge : values()) {
                if (experience >= badge.getMinExperience()) {
                    retval = badge;
                }
            }
            return retval == null ? REC : retval;
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
    public User(String username, String fingerprint,String salt) {
        this.username = username;
        this.fingerprint = fingerprint;
        this.salt = salt;
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
     * @return password
     */
    public String getFingerprint() {
        return fingerprint;
    }

    protected void setFingerprint(String fingerprint){
        this.fingerprint = fingerprint;
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
    protected void setExperience(int experience) throws IllegalArgumentException{
        if (experience < 0 ) {
            throw new IllegalArgumentException("experience in [0,"+MAX_EXP+"]");
        }
        this.experience = experience;
        this.rank = Badge.getRankForExperience(experience);
    }

    /*
     * Aggiunge esperienza all'utente.
     * 
     * Metodo sincronizzato per per sicurezza
     */
    synchronized public void addExperience(int experience) throws IllegalArgumentException{
        int newExperience = this.experience + experience;
        if(newExperience <0) setExperience(0);
        else if(newExperience > MAX_EXP) setExperience(MAX_EXP);
        else setExperience(newExperience);
    }

    /**
     * Restituisce il grado dell'utente.
     *
     * @return Il grado.
     */
    public Badge getBadge() {
        return rank;
    }

    //Controlla se la password dell'utente è corretta
    public boolean passwordTest(String compare){
        return fingerprint.equals(compare);
    }
}
