package clientUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPListener extends Thread {
    private LinkedBlockingQueue<String> udpMessages;
    private String UDPaddress;
    private int UDPport;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private AtomicBoolean running;      // Flag di esecuzione
    private String errorLogPath;
    private PrintWriter logWriter;
    private boolean hasErrors = false;

    public UDPListener(String address, int port, LinkedBlockingQueue<String> queue, String logPath) 
    throws IOException
    {
        
        this.udpMessages = queue;
        this.UDPaddress = address;                  
        this.UDPport = port;                        
        this.running = new AtomicBoolean(true);     // Inizializza la flag di running
        this.errorLogPath = logPath;                // Inizializza il percorso del file di log
        logWriter = null;
        createLogFile();                            // Crea il file di log se non esiste
        logWriter.println("UDPListener built");
    }

    @Override
    public void run() {
        try {
            logWriter.print("Running");
            group = InetAddress.getByName(UDPaddress);
            multicastSocket = new MulticastSocket(UDPport);
            multicastSocket.joinGroup(group);
            byte[] buffer = new byte[2048];

            // Attende messaggi
            while (running.get()) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try {
                    // Riceve pacchetti UDP [Bloccante]
                    multicastSocket.receive(dp);
                    logWriter.println("Unblocked");
                    String s = new String(dp.getData(), 0, dp.getLength(), "UTF-8");
                    // Inserisce il messaggio nella coda condivisa
                    udpMessages.put(s);
                    logWriter.println("Received: " + s);
                } catch (IOException e) {
                    logError(e);
                }
            }
        } catch (Exception e) {
            logError(e);
        } finally {
            // Cleanup
            if (multicastSocket != null && !multicastSocket.isClosed()) {
                try {
                    multicastSocket.leaveGroup(group);
                } catch (IOException e) {
                    logError(e);
                }
                multicastSocket.close();
            }
            // Chiude il file di log e fa cleanup
            if (logWriter != null) {
                logWriter.close();
                //FIXME: Questo codice non è mai eseguito
                boolean ciao = false;
                if (ciao) {
                    try {
                        // elimina il file se hasErrors è false
                        Files.delete(Paths.get(errorLogPath));
                    } catch (IOException e) {
                        // Il file non viene cancellato
                    }
                }
            }

            running.set(true); // Reset the running flag
        }
    }

    public void stopUDPlistening() {
        running.set(false); // Setta la flag per l'uscita dal loop
        // Chiude la socket di multicast
        if (multicastSocket != null) {
            multicastSocket.close();
        }
    }

    public void startUDPlistening() {
        try{
            logWriter.println("Starting...");
            this.start();
        }catch(IllegalThreadStateException e){
            //Il thread è già in esecuzione
            logWriter.println("UDP Exception");
            
        }
    }

    public void Join() {
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean isFileCreated() {
        return logWriter != null;
    }

    private void createLogFile() throws IOException{
            // Crea il file se non esiste
            logWriter = new PrintWriter(new FileWriter(errorLogPath, true),true); // Modalità append
            logWriter.println("Log file created");
            //logWriter.flush();
        
    }

    private void logError(Throwable e) {
        if(logWriter != null){
            hasErrors = true;
            e.printStackTrace(logWriter); 
        }
    }

}
