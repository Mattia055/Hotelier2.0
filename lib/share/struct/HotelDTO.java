package lib.share.struct;

import java.util.Arrays;

/*Data Transfer Object
 * 
 * Analogo all'Hotel ma senza campi che non devono essere trasferiti
 */

public class HotelDTO{

    //public int         id; esempio di campo che preferirei non trasferire
    public String      name;
    public String      description;
    public String      city;
    public String      phone;
    public String[]    services;
    public double      rank;
    public Score       rating;
    public int         rank_position = -1;


    public HotelDTO (String name, String description, String city, String phone, String[] services, double rank,Score rating,int rank_position) {
        //this.id         = id;
        this.name       = name;
        this.description= description;
        this.city       = city;
        this.phone      = phone;
        this.services   = services;
        this.rank       = rank;
        this.rating     = rating;
        this.rank_position = rank_position;
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

    public String[] getServices(){
        return services;
    }

    public Score getRating(){
        return rating;
    }

    public double getRank(){
        return rank;
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
        if(rank_position != -1){
            str.append("Rank Position: " + rank_position + "\n");
        if(rank != -1){
            str.append("\nRank: " + rank + "\n");
        }
        }
        if (rating != null) {
            str.append("\nRating\n" + rating.toString());
        } else {
            str.append("\nRating: N/A\n");
        }
        
        return str.toString();
    }

}