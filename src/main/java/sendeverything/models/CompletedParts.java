package sendeverything.models;

import lombok.Data;

@Data
public class CompletedParts {
    private int partNumber;

    private String eTag;

    public CompletedParts(int partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

}
