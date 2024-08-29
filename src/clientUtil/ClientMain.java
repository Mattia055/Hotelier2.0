package clientUtil;

import lib.client.api.HotelierAPI;
import lib.client.api.APIException;
import lib.client.api.APIResponse;

public class ClientMain {
    public static void main(String[] args){
        HotelierAPI api = new HotelierAPI("localhost",7284);
        try{
        api.connect();
        for(int i = 0; i < 100000; i++){
            APIResponse response = api.UserRegister("Mattia055", "CIAO");
            response = api.UserLogin("Mattia055", "CIAO");
            System.out.println(response.toString());
            response = api.UserLogout();
            System.out.println(response.toString());
        }
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
