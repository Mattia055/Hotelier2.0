package serverUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ClientBuffer {
    private Integer     capacity;
    private int         limit;
    private ByteBuffer  data;
    private boolean     full;

    //penso di potermi arrangiatre anche senza limit e full;

    private final int Max_Capacity;
    private static final int MAX_DEFAULT = 8192;

    public ClientBuffer(int max){
        this.capacity = null;
        this.data = null;
        this.full = false;
        this.Max_Capacity = max;
    }

    public ClientBuffer(){
        this(MAX_DEFAULT);
    }


    protected void initializeRead(SocketChannel client) throws Exception{
        //inizializza il buffer alla dimensione del primo intero letto
        ByteBuffer size = ByteBuffer.allocate(Integer.BYTES);

        try {
            int bytes_read = client.read(size);

            if(bytes_read == -1){
                throw new ClosedChannelException();
            }

            if(bytes_read != Integer.BYTES){
                throw new Exception("Error reading the size of the request");   
            }

            size.flip();

            this.capacity = size.getInt();
            this.limit = 0;

            if(capacity > Max_Capacity){
                throw new Exception("Request too big");
            }

            this.data = ByteBuffer.allocate(this.capacity);
        } catch(NotYetConnectedException e){
            //non dovrebbe accadere
            e.printStackTrace();
        } catch(IOException e){
            throw new Exception("Error when elaborating request");
        }

    }

    public boolean isFull(){
        return full;
    }

    public boolean isEmpty(){
        return full;
    }

    public int capacity(){
        return capacity;
    }

    public int leftToRead(){
        return capacity - limit;
    }

    public boolean readFrom(SocketChannel client) throws Exception{
        if(capacity == null) 
            initializeRead(client);

        //dopo che si è inizializzato si inizia a leggere il contenuto della richiesta
        if(!full){

            int size = client.read(data);

            if(size == -1){
                throw new ClosedChannelException();
            }

            //dopo che si è letto si aggiunge la quantità letta a quella totale
            limit += size;

            //se si è letto tutto il buffer si setta ready a true
            if(limit == capacity){
                full = true;
            }

        }

        return full;

    }

    public ByteBuffer getData(){
        data.position(0);
        return data;
    }

    public String getStringData() {
        // Create a copy of the buffer to avoid altering the original position
        ByteBuffer bufferCopy = data.duplicate();
        
        // Prepare the buffer for reading from the start
        bufferCopy.position(0);
        
        // Convert ByteBuffer content to a String using UTF-8 charset
        Charset charset = StandardCharsets.UTF_8;
        return charset.decode(bufferCopy).toString();
    }

    public void reset(){
        capacity = null;
        limit = 0;
        data = null;
        full = false;
    }

    public void wrapMessage(byte[] message){
        reset();
        ByteBuffer payload = ByteBuffer.wrap(message);
        this.capacity = null;
        this.data = payload;
        this.limit = 0;
        this.full = false;
    }

    
    protected void initializeWrite(SocketChannel client)throws Exception{
        //viene chiamato quando si vuole scrivere. Setta la capacità del buffer a 0 scrivendo sulla socket 
        //la dimensione del payload

        //write payload size in socketchannel;

        ByteBuffer size = ByteBuffer.allocate(Integer.BYTES);
        this.capacity = data.capacity();

        client.write(size.putInt(this.capacity));

    }

    //alternativamente potevo usare 
    public boolean writeTo(SocketChannel client) throws Exception{
        if(capacity == null){
            initializeWrite(client);
        }
        if(!full){
            int size = client.write(data);

            if(size == -1){
                throw new ClosedChannelException();
            }

            limit += size;
            
            if(limit == capacity){
                full = true;
            }

        }

        return full;
    }

}
