package serverUtil;

import lib.share.struct.HotelDTO;
import lib.share.struct.Score;

import java.util.Arrays;

public class Hotel{

    protected   int         id; 
    protected   String      name;
    private     String      description;
    protected   String      city;
    private     String      phone;
    private     String[]    services;
    protected double      rank;
    protected Score       rating;
    protected int         rank_position;

    public Hotel (int id, String name, String description, String city, String phone, String[] services, double rank,Score rating,int rank_position) {
        this.id             = id;
        this.name           = name;
        this.description    = description;
        this.city           = city;
        this.phone          = phone;
        this.services       = services;
        this.rank           = rank;
        this.rating         = rating;
        this.rank_position  = rank_position;
    }

    public int getID(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getDescription(){
        return description;
    }

    public String getCity(){
        return city;
    }

    public String getPhone(){
        return phone;
    }

    public int getRankPosition(){
        return rank_position;
    }

    public String[] getServices(){
        return services;
    }

    public double getRank(){
        return rank;
    }

    public Score getRating(){
        return rating;
    }

    //aggiorna anche il rank
    public void setRating(Score newRating){
        rating = newRating;
        //arrotonda alla prima cifra decimale
        rank = Math.round(rating.getMean() * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("");
        
        str.append("Name: " + (name != null ? name : "N/A") + "\n");
        str.append("Description: " + (description != null ? description : "N/A") + "\n");
        str.append("City: " + (city != null ? city : "N/A") + "\n");
        str.append("Phone: " + (phone != null ? phone : "N/A") + "\n");
        
        if (services != null) {
            str.append("Services: " + Arrays.toString(services) + "\n");
        } else {
            str.append("Services: N/A\n");
        }
        
        if (rating != null) {
            str.append("\nRating\n" + rating.toString());
        } else {
            str.append("\nRating: N/A\n");
        }
        
        return str.toString();
    }

    public HotelDTO toDTO(){
        try{
        return new HotelDTO(name, description, city, phone, 
                            services != null? services.clone():services, 
                            rank,rating != null? rating.clone():Score.Placeholder(),
                            rank_position);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

}