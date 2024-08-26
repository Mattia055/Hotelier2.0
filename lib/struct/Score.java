package lib.struct;

public class Score implements Cloneable{
    protected static final double MAX_GRADE = 5;
    protected static final double MIN_GRADE = 1; 

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

    public static Score Placeholder(){
        return new Score(MIN_GRADE,MIN_GRADE,MIN_GRADE,MIN_GRADE,MIN_GRADE);
    }

    /*
    * controllo univoco per ogni campo
    */

    public boolean isValidPosition(){
        return Position >= MIN_GRADE && Position <= MAX_GRADE;
    }

    public boolean isValidCleaning(){
        return Cleaning >= MIN_GRADE && Cleaning <= MAX_GRADE;
    }

    public boolean isValidService(){
        return Service >= MIN_GRADE && Service <= MAX_GRADE;
    }

    public boolean isValidPrice(){
        return Price >= MIN_GRADE && Price <= MAX_GRADE;
    }

    public boolean isValidGlobal(){
        return Global >= MIN_GRADE && Global <= MAX_GRADE;
    }

    public static boolean isValid(double... Scores){
        for(double Score : Scores){
            if(Score > MAX_GRADE || Score < MIN_GRADE)
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

    public void setService(double Service){
        this.Service = Service;
    }

    public void setPrice(double Price){
        this.Price = Price;
    }   
    
    public void setPosition(double Position){
        this.Position = Position;
    }

    public void setCleaning(double Cleaning){
        this.Cleaning = Cleaning;
    }

    public void setGlobal(double Global){
        this.Global = Global;
    }

    public static double getMin(){
        return (double) MIN_GRADE;
    }

    public static double getMax(){
        return  MAX_GRADE;
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
    public Score clone() {
        try {
            return (Score) super.clone(); //Dato che tutti i dati sono primitivi una shallow copy è sufficiente
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    

}