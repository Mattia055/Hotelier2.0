package serverUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ClientBuffer {
    private ByteBuffer buffer;
    private ByteBuffer sizeBuffer;
    
    private static final int MAX_DEFAULT = 2048;
    private final int size;

    public ClientBuffer(int max){
        buffer = ByteBuffer.allocateDirect(max);
        sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        size = max;
    }

    public ClientBuffer(){
        this(MAX_DEFAULT);
    }

    /*
     * Function to read from a socket channel, reads up to "toRead" bytes
     * calls readInit if toRead is 0 returns true if finished reading;
     * buffer has always remaining since im preallocating that so im using toRead
     */
    public boolean readFrom(SocketChannel client) throws Exception {
        if(sizeBuffer.hasRemaining()){
            if(client.read(sizeBuffer) == -1)   
                throw new ClosedChannelException();
            else if(sizeBuffer.hasRemaining())   
                return false;
            sizeBuffer.flip();
            int length = sizeBuffer.getInt();
            if(length > size)   
                throw new Exception("Packet too big");
            buffer.clear();
            buffer.limit(length);
        }
    
        if(client.read(buffer) == -1)
            throw new ClosedChannelException();
        else if(!buffer.hasRemaining()){
            buffer.flip();
            /*
             * IL CLEAR DEL BUFFER E' IMPORTANTE CHE STIA QUI
             */
            sizeBuffer.clear();
            return true;
        }
        return false;
    }

    /*
     * Function that gets data from the buffer and converts it into a string
     */
    public String getString(){
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        buffer.clear();
        return new String(data);
    }

    /*
     * Function to write to a socket channel, first writes 4 bytes for 
     * packet length with "writeInit" 
     * Write init returns true when sizeBuffer is empty else returns false
     * 
     * after that attempts to write till the buffer is empty
     */
    
    public boolean writeTo(SocketChannel client) throws IOException {
        //inserisce la lunghezza del pacchetto
        if(sizeBuffer.hasRemaining()){
            if(client.write(sizeBuffer) == -1){
                throw new ClosedChannelException();
            }
            else if(sizeBuffer.hasRemaining()){
                return false;
            }
            sizeBuffer.clear();
        }
        //inserisce il pacchetto
        if(client.write(buffer)== -1) throw new ClosedChannelException();
        else if(buffer.hasRemaining()) return false;
        buffer.clear();
        return true;
    }

    public void wrapString(String data){
        byte[] dataBytes = data.getBytes();
        sizeBuffer.clear();
        sizeBuffer.putInt(dataBytes.length);
        sizeBuffer.flip();
        buffer.put(dataBytes);
        buffer.limit(dataBytes.length);
        buffer.flip();
    }

}

