package sendeverything.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import sendeverything.models.room.BoardType;

@Data
public class BoardRequest {
    @NotBlank
    BoardType boardType;
}
