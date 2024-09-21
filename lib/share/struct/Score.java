package lib.share.struct;

public class Score implements Cloneable{
    //punteggi massimi e minimi
    public static final double max = 5;
    public static final double min = 0; 

    private double Position;
    private double Cleaning;
    private double Service;
    private double Price;
    private double Global;

    public Score(double Global, double Position, double Cleaning, double Service, double Price){
        this.Position   = Position;
        this.Cleaning   = Cleaning;
        this.Service    = Service;
        this.Price      = Price;
        this.Global     = Global;
        
    }

    //restituisce un placeHolder per i punteggi
    public static Score Placeholder(){
        return new Score(min,min,min,min,min);
    }

    /*
     * Verifica se i punteggi sono validi
     */

    public boolean isValidPosition(){
        return Position >= min && Position <= max;
    }

    public boolean isValidCleaning(){
        return Cleaning >= min && Cleaning <= max;
    }

    public boolean isValidService(){
        return Service >= min && Service <= max;
    }

    public boolean isValidPrice(){
        return Price >= min && Price <= max;
    }

    public boolean isValidGlobal(){
        return Global >= min && Global <= max;
    }

    /*
     * Verifica se i punteggi sono validi
     * 
     * Metodo non utilizzato
     */
    public static boolean isValid(double... Scores){
        for(double Score : Scores){
            if(Score > max || Score < min)
                return false;
        }
        return true;
    }

    public boolean isValid(){
        return isValid(Position,Cleaning,Service,Price,Global);
    }

    public double getGlobal(){
        return Global;
    }

    public double getCleaning(){
        return Cleaning;
    }

    public double getPosition(){
        return Position;
    }

    public double getPrice(){
        return Price;
    }

    public double getService(){
        return Service;
    }

    /*
     * Setter con arrotondamento alla seconda cifra decimale
     */
    public void setService(double Service){
        this.Service = Math.round(Service * 100.0) / 100.0;
    }

    public void setPrice(double Price){
        this.Price = Math.round(Price * 100.0) / 100.0;
    }   
    
    public void setPosition(double Position){
        this.Position = Math.round(Position * 100.0) / 100.0;
    }

    public void setCleaning(double Cleaning){
        this.Cleaning = Math.round(Cleaning * 100.0) / 100.0;
    }

    public void setGlobal(double Global){
        this.Global = Math.round(Global * 100.0) / 100.0;
    }

    public static double getMin(){
        return (double) min;
    }

    public static double getMax(){
        return  max;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("");
        str.append("Global: " + Global + "\n");
        str.append("Position: " + Position + "\n");
        str.append("Cleaning: " + Cleaning + "\n");
        str.append("Service: " + Service + "\n");
        str.append("Price: " + Price);

        return str.toString();
    }

    public double getMean(){
        return ((Position + Cleaning + Global + Service + Price) / 5);
    }

    @Override
    /*
     * Metodo di clonazione
     */
    public Score clone() {
        try {
            return (Score) super.clone(); //Dato che tutti i dati sono primitivi esegue una shallow copy
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    

}