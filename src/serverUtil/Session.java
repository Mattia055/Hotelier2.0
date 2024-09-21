package serverUtil;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import lib.share.packet.Request.Method;

/*
 * Memorizza lo stato della connessione con un client
 * 
 * Mantiene un buffer per letture/scritture per la gestione
 * di operazioni non bloccanti
 */
public class Session {
    /*
     * Inizializza un buffer pool per il caching dei buffer
     */
    private static final BufferPool pool;       
    private static final int        MAX_SIZE;   //dimensione massima del pacchetto

    //blocco static per i campi statici
    static{
        pool = new BufferPool(ServerContext.ALLOC_TRESHOLD, ServerContext.BUFFER_POOL_SIZE);
        MAX_SIZE = ServerContext.PACKET_LENGTH;
    }

    private     ByteBuffer  Buffer;
    private     ByteBuffer  SizeBuffer;
    protected   String      Message;
    protected   String      Username;
    protected   Object      Data;
    protected   Method      LastMethod;
    private     boolean     PendingBufferInit;
    private     boolean     PendingMessageCollection;

    public Session() {
        this.Username                   = null;
        this.Data                       = null;
        this.LastMethod                 = null;
        this.Message                    = null;
        this.Buffer                     = null;
        //flag di sicurezza che evitano la sovrascrittura del buffer
        this.PendingBufferInit          = false;
        this.PendingMessageCollection   = false;
        //buffer per la dimensione del pacchetto
        this.SizeBuffer     = ByteBuffer.allocate(Integer.BYTES);
    }
    /*
     * Getters | Setters
     */
    public String getMessage() {
        PendingMessageCollection = false;
        return Message;
    }

    public void setMessage(String message) {
        this.Message = message;
    }

    public Object getData() {
        return Data;
    }  

    public boolean isLogged(){
        return Username != null;
    }

    public void setLogin(String Username){
        this.Username = Username;
    }

    public void setMethod(Method method){
        this.LastMethod = method;
    }

    public Method getMethod(){
        return LastMethod;
    }

    public void clearMethod(){
        this.LastMethod = null;
    }


    public void setData(Object data){
        this.Data = data;
    }

    public void clearData(){
        this.Data = null;
    }

    public void flush(){
        this.Username = null;
        this.Data = null;
        this.LastMethod = null;
    }

    /*
     * Legge da un socket channel assumendo che venga inviato come primo
     * elemento la dimensione del pacchetto restituendo true se la lettura 
     * è completata
     */
    public boolean readFrom(SocketChannel client) throws Exception{
        /*
         * Non permette nuove letture sullo stesso buffer
         * finche il messaggio non viene estratto almeno 
         * una volta
         */
        if(PendingMessageCollection){
            return true;
        }
        
        //legge la dimensione del pacchetto
        if(SizeBuffer.hasRemaining()){
            if(client.read(SizeBuffer) == -1)
                throw new ClosedChannelException();
            else if(SizeBuffer.hasRemaining())
                return false;
            SizeBuffer.flip();
            int length = SizeBuffer.getInt();
            if(length > MAX_SIZE)
                throw new Exception("Packet too big");
            //riceve un uffer e setta il limit alla lunghezza del pacchetto
            Buffer = pool.get(length);
            Buffer.clear();
            Buffer.limit(length);
        }

        //analogo a sopra
        if(Buffer.hasRemaining()){
            if(client.read(Buffer) == -1)
            throw new ClosedChannelException();

            else if(!Buffer.hasRemaining()){
                SizeBuffer.clear();
                getMessageFromBuffer();
            }
            else return false;
        }
        return true;

    }

    //analoga a ReadFrom
    public boolean writeTo(SocketChannel client) throws Exception {
        if(!PendingBufferInit){
            setBufferFromMessage();
            PendingBufferInit = true;
        }
        
        if(SizeBuffer.hasRemaining()){
            if(client.write(SizeBuffer) == -1)
                throw new ClosedChannelException();
    
            else if(SizeBuffer.hasRemaining())
                return false;
            SizeBuffer.clear();
        }

        if(Buffer.hasRemaining()) {
            if(client.write(Buffer) == -1)
                throw new ClosedChannelException();

            else if(Buffer.hasRemaining())
                return false;
        }

        return true;

    }
    //Setta il campo Message a partire dal buffer
    private void getMessageFromBuffer() {
        // resetta la flag di sicurezza
        PendingBufferInit = false;
        Buffer.flip();
        Message = StandardCharsets.UTF_8.decode(Buffer).toString();
    }


    /*
     * Richiede un nuovo buffer al BufferPool e ci scrive dentro il 
     * messaggio. Setta SizeBuffer alla dimensione del messaggio.
     * Il buffer viene poi preparato per la scrittura
     */
    private void setBufferFromMessage(){
        byte[] MessageBytes = Message.getBytes(StandardCharsets.UTF_8);
        SizeBuffer.putInt(MessageBytes.length);
        SizeBuffer.flip();
        Buffer = pool.get(MessageBytes.length);
        Buffer.put(MessageBytes);
        Buffer.limit(MessageBytes.length);
        Buffer.flip();
    }

}
