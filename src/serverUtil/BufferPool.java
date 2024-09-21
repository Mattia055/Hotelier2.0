package serverUtil;

import java.util.LinkedHashMap;
import java.nio.ByteBuffer;

public class BufferPool extends LinkedHashMap<Integer, ByteBuffer> {
    private final int tresHold; // Massima differenza tra la dimensione del buffer richiesta e quella effettiva
    private final int maxSize;  // Massimo numero di buffer da mantenere in memoria

    public BufferPool(int tresHold, int maxSize) {
        super(maxSize, 0.75f, true); // Inizializzata con modalità di accesso
        this.tresHold = tresHold;
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<Integer, ByteBuffer> eldest) {
        return size() > maxSize;
    }

    @Override
    public ByteBuffer get(Object key) {
        Integer keyInteger = (Integer) key;
        ByteBuffer buffer = super.get(keyInteger);
        if (buffer == null) {
            //arrotonda la dimensione del buffer richiesto al threshold per eccesso
            int roundedSize = roundUpSize(keyInteger);
            buffer = ByteBuffer.allocate(roundedSize);
            //inserisce nella mappa per markare il timestamp di utilizzo
            put(keyInteger, buffer);
        }
        buffer.clear();
        return buffer;
    }

    private int roundUpSize(int size) {
        return ((size + tresHold - 1) / tresHold) * tresHold;
    }
    
}
