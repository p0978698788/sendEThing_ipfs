package sendeverything.models;

import lombok.Data;

@Data
public class FileChunkData {
    private final int chunkNumber;
    private final byte[] data;

    public FileChunkData(int chunkNumber, byte[] data) {
        this.chunkNumber = chunkNumber;
        this.data = data;
    }
}
