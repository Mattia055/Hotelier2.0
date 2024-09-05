package clientUtil;

import lib.client.cli.CliHandler;
import lib.client.api.APIException;
import lib.client.api.HotelierAPI;

public class ClientMain {
    public static void main(String[] args){
        HotelierAPI EntryPoint = new HotelierAPI("localhost",7284);
        CliHandler cli = CliHandler.getInstance();
        CliHandler.setEntryPoint(EntryPoint);
        try{
            EntryPoint.connect();
        }catch(APIException e){
            System.out.println("Cannot establish connection with server on localhost:7284");
            System.exit(-1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(( ) -> CliHandler.terminate()));

        cli.run();
    }
    
}