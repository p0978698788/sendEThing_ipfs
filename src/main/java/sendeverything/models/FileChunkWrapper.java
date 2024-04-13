package sendeverything.models;

import lombok.Data;

@Data
public class FileChunkWrapper {
    private int chunkNumber;
    private byte[] data;

    public FileChunkWrapper(int chunkNumber, byte[] data) {
        this.chunkNumber = chunkNumber;
        this.data = data;
    }
}
