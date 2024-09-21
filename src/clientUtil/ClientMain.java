package clientUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import lib.client.api.APIException;
import lib.client.api.HotelierAPI;
import lib.share.etc.ConfigLoader;

/*
 * Class che implementa singleton
 */
public class ClientMain {
    private static final String CONFIG_DEFAULT = "config/client.properties";
    private static  LinkedBlockingQueue<String> UDPQueue;
    public static   UDPListener UDPSubscriber;
    private static  HotelierAPI EntryPoint;
    
    public static void main(String[] args){
        try{
            //lettura da file di configurazione e inizializzazione delle strutture dati
            init();
        } catch(Exception e){
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        TuiHandler cli = TuiHandler.getInstance();
        TuiHandler.setEntryPoint(EntryPoint);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new TerminationHandler()));

        cli.run();
        new TerminationHandler().run();
        
    }

    public static String fetchNotification(){
        String toReturn = null;
        try{
            toReturn = UDPQueue.poll();
        } catch(NullPointerException e){
        }
        return toReturn == null ? "" :"✉️ Notification: " + toReturn;
    }

    //Metodi privati per la validazione di Host e porta
    private static void validatePort(int port,String label) throws MalformedParametersException {
        if (port <= 1024 || port > 65535) {
            throw new MalformedParametersException("Il parametro "+label+" è mancante o non valido. Seleziona un valore tra [1024, 65535].");
        }
    }
    
    private static void validateHost(String host,String label) throws MalformedParametersException {
        try {
            InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new MalformedParametersException("Il parametro "+label+" non risolve in un indirizzo host valido.");
        }
    }

    private static void init() throws FileNotFoundException, IOException, MalformedParametersException, APIException{

        ConfigLoader    config = new ConfigLoader(CONFIG_DEFAULT);
        String          UDPAddr = config.getStringAttribute("udp_addr",null);
        validateHost(UDPAddr, "udp_addr");

        String          logFile = config.getStringAttribute("udp_log_path",null);
        if(logFile == null) throw new MalformedParametersException("Il parametro 'logFile' è mancante.");

        String          Host = config.getStringAttribute("host",null);
        validateHost(Host,"host");

        int             UDPPort = config.getIntAttribute("udp_port",-1);
        int             Port = config.getIntAttribute("port",-1);

        validatePort(UDPPort,"udp_port");
        validatePort(Port,"port");

        UDPQueue = new LinkedBlockingQueue<>();
        UDPSubscriber = new UDPListener(UDPAddr,UDPPort,UDPQueue,logFile);

        EntryPoint = new HotelierAPI(Host,Port);
        EntryPoint.connect();
    }

    private static class TerminationHandler implements Runnable{
        public void run(){
            //
            TuiHandler.terminate();
            try{
                EntryPoint.disconnect();
            }catch(APIException e){
                System.out.println("Error when disconnecting from server");
            }
            UDPSubscriber.stopUDPlistening();
            UDPSubscriber.Join();
        }
    }
    
}