package sendeverything.service;

import sendeverything.models.DatabaseFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;
import sendeverything.models.room.DBRoomFile;

import javax.imageio.ImageIO;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Component
public class CodeGenerator {
    public static Blob generateQRCodeImage(String text, int width, int height) throws WriterException, IOException, SQLException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Convert BufferedImage to byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageInByte = baos.toByteArray();

        // Convert byte[] to Blob
        Blob blob = new SerialBlob(imageInByte);

        return blob;
    }

    public Blob generateAndStoreQRCode(int width, int height, DatabaseFile dbFile) throws WriterException, IOException, SQLException {
        UUID uuid = UUID.randomUUID();
        // 生成 QR 码
        String fileDownloadUri = "http:/localhost:8080/downloadFileByCode/"+dbFile.getVerificationCode();

        // 创建并保存 QRCode 实体

        return generateQRCodeImage(fileDownloadUri, width, height);
    }
    public Blob generateAndStoreQRCode(int width, int height, DBRoomFile dbFile) throws WriterException, IOException, SQLException {
        // 生成 QR 码
        String fileDownloadUri = "http:/localhost:8080/downloadRoomFileByCode/"+dbFile.getVerificationCode();
        // 创建并保存 QRCode 实体

        return generateQRCodeImage(fileDownloadUri, width, height);
    }

    public String blobToBase64(Blob blob) throws SQLException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try (InputStream in = blob.getBinaryStream()) {
            int n = 0;
            while ((n = in.read(buf)) >= 0) {
                baos.write(buf, 0, n);
            }
        }
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }


    public static String generateDownloadCode() {
        Random random = new Random();
        int downloadCode = random.nextInt(900000) + 100000; // This will generate a random number between 100000 and 999999
        return String.valueOf(downloadCode);
    }

}
