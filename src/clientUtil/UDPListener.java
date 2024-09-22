package clientUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPListener extends Thread {
    private LinkedBlockingQueue<String> udpMessages;
    private String UDPaddress;
    private int UDPport;
    private InetAddress group;
    private AtomicBoolean running;      // Flag di esecuzione
    private MulticastSocket multicastSocket;
    private String errorLogPath;
    private PrintWriter logWriter;
    private boolean hasErrors = false;
    private int timeout = 0;
    private boolean closeOnTimeout = false;


    public UDPListener(String address, int port, LinkedBlockingQueue<String> queue, String logPath,boolean closeOnTimeout,long timeout) 
    throws IOException
    {
        this.udpMessages = queue;
        this.UDPaddress = address;                  
        this.UDPport = port;                        
        this.running = new AtomicBoolean(true);     // Inizializza la flag di running
        this.errorLogPath = logPath;  
        this.multicastSocket = null;  
        this.timeout = (int)timeout;
        this.closeOnTimeout = closeOnTimeout;            
    }

    @Override
    public void run() {
        try{
            this.multicastSocket = new MulticastSocket(UDPport);
            group = InetAddress.getByName(UDPaddress);
            multicastSocket.joinGroup(group);
            if(!closeOnTimeout){
                multicastSocket.setSoTimeout(timeout); 
            }
            byte[] buffer = new byte[2048];

            // event loop
            while (running.get()) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try {
                    // Riceve pacchetti UDP [Bloccante]
                    multicastSocket.receive(dp);    //si risveglia dopo 5 secondi
                    String s = new String(dp.getData(), 0, dp.getLength(), "UTF-8");
                    // Inserisce il messaggio nella coda condivisa
                    udpMessages.put(s);
                } catch (SocketTimeoutException | SocketException e){
                    //  ThreadShuttingDown
                }
                catch (Exception e) {
                    logError(e);
                }
            }
            if(!multicastSocket.isClosed() && closeOnTimeout){
                multicastSocket.leaveGroup(group);
                multicastSocket.close();
            }

        }
         catch (Exception e) {
            logError(e);

        } finally {
            try{
                deleteLogFile();
            } catch(IOException e){
            }
        }
    }

    public void stopUDPlistening(boolean force) {
        running.set(false); // Setta la flag per l'uscita dal loop
        try{
            if(force || !closeOnTimeout){
                multicastSocket.close();
            }
        }catch(NullPointerException | ClassCastException e){

        }
    }
    //polymorphic method
    public void stopUDPlistening() {
        stopUDPlistening(false);
    }

    public void startUDPlistening() {
        try{
            this.start();
        }catch(IllegalThreadStateException e){
            //Il thread è già in esecuzione
            logError(e);
            
        }
    }

    public void Join() {
        running.set(false);
        try {
            this.join();
        } catch (InterruptedException e) {
        
        }
    }

    public boolean isFileCreated() {
        return logWriter != null;
    }

    private void logError(Throwable e) {
        if(logWriter == null){
            try{
                logWriter = new PrintWriter(new FileWriter(errorLogPath, true),true); // Modalità append e autoflush
            } catch(IOException ex){}
        }   
        if(e != null)
            e.printStackTrace(logWriter); 
        hasErrors = true;
        return;
    
    }

    private void deleteLogFile() throws IOException{
        if(logWriter == null)
            return;
        else if(hasErrors == false){
            logWriter.close();
            Files.delete(Paths.get(errorLogPath));
        }
    }

}
