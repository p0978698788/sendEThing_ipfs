package sendeverything.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileListResponse {
    private List<?> fileName;
    private List<?> fileData;
}
