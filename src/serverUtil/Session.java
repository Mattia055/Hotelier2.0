package serverUtil;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import lib.share.packet.Request.Method;

public class Session {
    /*I buffer vengono gestiti internamente dalla sessione
     * Quando si può leggere direttamente da una socket con Session.readFrom(SocketChannel)
     * se il buffer è n
     * 
     */
    private static final BufferPool pool;
    private static final int MAX_SIZE;

    static{
        pool = new BufferPool(ServerContext.ALLOC_TRESHOLD, ServerContext.BUFFER_POOL_SIZE);
        MAX_SIZE = ServerContext.PACKET_LENGTH;
    }

    private     ByteBuffer  Buffer;
    private     ByteBuffer  SizeBuffer;
    protected   String      Message;
    protected   String      Username;
    protected   Object      SessionData;
    protected   Method      LastMethod;
    private     boolean     PendingBufferInit;
    private     boolean     PendingMessageCollection;

    public Session() {
        this.Username                   = null;
        this.SessionData                = null;
        this.LastMethod                 = null;
        this.Message                    = null;
        this.Buffer                     = null;
        this.PendingBufferInit          = false;
        this.PendingMessageCollection   = false;
        //alloco il buffer per la lunghezza del pacchetto
        this.SizeBuffer     = ByteBuffer.allocate(Integer.BYTES);
    }

    public String getMessage() {
        PendingMessageCollection = false;
        return Message;
    }

    public void setMessage(String message) {
        this.Message = message;
    }

    public Object getData() {
        return SessionData;
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
        this.SessionData = data;
    }

    public void clearData(){
        this.SessionData = null;
    }

    public void flush(){
        this.Username = null;
        this.SessionData = null;
        this.LastMethod = null;
    }

    public boolean readFrom(SocketChannel client) throws Exception{
        if(PendingMessageCollection){
            return true;
        }
        if(SizeBuffer.hasRemaining()){
            if(client.read(SizeBuffer) == -1)
                throw new ClosedChannelException();
            else if(SizeBuffer.hasRemaining())
                return false;
            SizeBuffer.flip();
            int length = SizeBuffer.getInt();
            if(length > MAX_SIZE)
                throw new Exception("Packet too big");
            Buffer = pool.get(length);
            Buffer.clear();
            Buffer.limit(length);
        }

        if(!Buffer.hasRemaining()){
            return true;
        }

        if(client.read(Buffer) == -1)
            throw new ClosedChannelException();

        else if(!Buffer.hasRemaining()){
            SizeBuffer.clear();
            getMessageFromBuffer();
            return true;
        }

        else return false;

    }

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
    /*Dal buffer scrive sul messaggio
     * Non modifica il buffer
    */
    private void getMessageFromBuffer() {
        // Save the current position and limit of the buffer
        PendingBufferInit = false;
        // Set the limit to the current position and flip the buffer for reading
        Buffer.flip();
        // Decode the buffer content to string
        Message = StandardCharsets.UTF_8.decode(Buffer).toString();
        // Restore the original position and limit of the buffer
    }


    /*dal messaggio alloca un nuovo buffer*/
    /*Non modifica il messaggio*/
    /*
     * La flag boolean è presente perche normalmente a scrittura
     * terminata il buffer viene rilasciato, se si tenta di riscrivere
     * si deve risettare 
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
