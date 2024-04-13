package sendeverything.payload.response;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;



@Data
public class FileResponse{
    private String fileName;
    private String fileDownloadUri;
    private String fileType;
    private long size;
    private String DownloadCode;
    private String QRCodeImg;


    public FileResponse(String DownloadCode, String QRCodeImg) {
        this.DownloadCode = DownloadCode;
        this.QRCodeImg = QRCodeImg;

    }
    public FileResponse(String fileName) {
        this.fileName = fileName;

    }



}
