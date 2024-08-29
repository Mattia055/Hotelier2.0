package lib.share.struct;

import java.util.Arrays;

/*Data Transfer Object */

public class HotelDTO{

    //private int         id; esempio di campo che preferirei non trasferire
    private String      name;
    private String      description;
    private String      city;
    private String      phone;
    private String[]    services;
    //this gets modified when ranking manager updates the score
    private Score       rating;

    public HotelDTO (String name, String description, String city, String phone, String[] services, Score rating) {
        //this.id         = id;
        this.name       = name;
        this.description= description;
        this.city       = city;
        this.phone      = phone;
        this.services   = services;
        this.rating     = rating;
    }
    /*
    public int getID(){
        return id;
    }*/

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

    public void setRating(Score newRating){
        rating = newRating;
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

}