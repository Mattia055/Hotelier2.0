package serverUtil;

import java.util.LinkedHashMap;
import java.nio.ByteBuffer;

public class BufferPool extends LinkedHashMap<Integer, ByteBuffer> {
    private final int tresHold; // The rounding threshold for buffer sizes
    private final int maxSize;  // Maximum number of buffers to keep in the cache

    public BufferPool(int tresHold, int maxSize) {
        super(maxSize, 0.75f, true); // Initialize with access order
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
            int roundedSize = roundUpSize(keyInteger);
            buffer = ByteBuffer.allocate(roundedSize);
            put(keyInteger, buffer);
        }
        buffer.clear();
        return buffer;
    }

    private int roundUpSize(int size) {
        return ((size + tresHold - 1) / tresHold) * tresHold;
    }
    
}
