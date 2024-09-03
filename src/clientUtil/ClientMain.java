package clientUtil;

import lib.client.api.HotelierAPI;
import lib.client.api.APIException;
import lib.client.api.APIResponse;

public class ClientMain {
    public static void main(String[] args){
        HotelierAPI api = new HotelierAPI("localhost",7284);
        try{
        api.connect();
            APIResponse response = api.HotelSearch("Firenze", "Hotel Firenze 1");
            System.out.println(response.toString());
            response = api.HotelsFetchAll("Firenze");
            System.out.println(response.toString());
        } catch(APIException e){
            e.printStackTrace();
        }
        try{
            System.out.println("\n\nSUCCESS");
        Thread.sleep(100000);
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        return ;
    }
    

}