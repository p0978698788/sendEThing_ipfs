package sendeverything.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import sendeverything.exception.FileNotFoundException;
import sendeverything.exception.FileStorageException;
import sendeverything.models.DatabaseFile;
import sendeverything.models.User;
import sendeverything.repository.DatabaseFileRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DatabaseFileService {

    @Autowired
    private DatabaseFileRepository dbFileRepository;



    public DatabaseFile storeMultipleFilesAsZip(MultipartFile[] files, Optional<User> optionalUser) throws IOException, SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        for (MultipartFile file : files) {
            ZipEntry zipEntry = new ZipEntry(Objects.requireNonNull(file.getOriginalFilename()));
            zos.putNextEntry(zipEntry);
            zos.write(file.getBytes());
            zos.closeEntry();
        }

        zos.close();
        byte[] bytes = baos.toByteArray();
        Blob blob = new SerialBlob(bytes);
        DatabaseFile dbFile = new DatabaseFile("uploadedFiles.zip", "application/zip", Instant.now());
        dbFile.setVerificationCode(generateUniqueVerificationCode());
        dbFile.setShortUrl(generateUniqueShortURL());
        optionalUser.ifPresent(dbFile::setUser);


        return dbFileRepository.save(dbFile);
    }
    public DatabaseFile storeFile(MultipartFile file, Optional<User> optionalUser) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if (fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Blob blob = new SerialBlob(file.getBytes());
            DatabaseFile dbFile = new DatabaseFile(fileName, file.getContentType(), Instant.now());
            dbFile.setVerificationCode(generateUniqueVerificationCode());
            dbFile.setShortUrl(generateUniqueShortURL());
            // 如果用户存在，则关联用户
            optionalUser.ifPresent(dbFile::setUser);


            return dbFileRepository.save(dbFile);
        } catch (IOException | SQLException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public DatabaseFile getFile(String filename) {
        System.out.println("getFile: " + filename);
        return dbFileRepository.findByFileName(filename)
                .orElseThrow(() -> new FileNotFoundException("File not found with filename " + filename));
    }


    public DatabaseFile getFileByVerificationCode(String verificationCode) {
        return dbFileRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new FileNotFoundException("File not found with verification code " + verificationCode));
    }


//    public List<String> getAllData(User user) {
//        List<DatabaseFile> files = user != null ?
//                dbFileRepository.findAllByUserOrderByTimestampDesc(user) :
//                Collections.emptyList();
//        return files.stream()
//                .map(file -> {
//                    try {
//                        long fileSizeInBytes = file.getData().length();
//                        long fileSizeInKB = fileSizeInBytes / 1024;
//                        String sizeUnit;
//
//                        if(fileSizeInKB > 1024) {
//                            double fileSizeInMB = fileSizeInKB / 1024.0;
//                            sizeUnit = "MB";
//                            return String.format("%.2f", fileSizeInMB) + " " + sizeUnit;
//                        }
//                        return fileSizeInKB + " KB";
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                        return null;
//                    }
//                })
//                .collect(Collectors.toList());
//    }

    private String generateUniqueVerificationCode() {
        Random random = new Random();
        String verificationCode;
        do {
            int code = random.nextInt(900000) + 100000;
            verificationCode = String.valueOf(code);
        } while (isCodeExists(verificationCode));

        return verificationCode;
    }


    private boolean isCodeExists(String code) {
        return dbFileRepository.existsById(code);
    }

    private String generateUniqueShortURL() {
        // 示例：生成一个随机的 6 位字符串
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder shortId = new StringBuilder(6);
        for (int i = 0; i < 8; i++) {
            shortId.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortId.toString();
    }
    public DatabaseFile getFileByShortUrl(String shortUrl) {
        return dbFileRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new FileNotFoundException("File not found with short URL " + shortUrl));
    }


    @Scheduled(cron = "1 1 * * * *") // 每小时执行一次
    public void deleteExpiredAnonymousFiles() {
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        List<DatabaseFile> expiredFiles = dbFileRepository
                .findByUserIdIsNullAndTimestampBefore(oneDayAgo);
        dbFileRepository.deleteAll(expiredFiles);
    }

    public void deleteFile(String fileId) {
        // 檢查檔案是否存在
        DatabaseFile file = dbFileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));

        // 刪除檔案
        dbFileRepository.deleteById(fileId);
    }

}
