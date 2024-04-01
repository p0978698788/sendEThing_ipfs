package sendeverything.service;

import sendeverything.models.*;
import sendeverything.repository.DatabaseFileRepository;
import sendeverything.repository.FileChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.io.*;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    // 存储文件分片的映射，键为文件ID，值为文件分片列表

    private final S3Client s3Client;
    private final DatabaseFileRepository dbFileRepository;
    private final FileChunkRepository fileChunkRepository;
    private final String accessKeyId="9D950C2D6B3E6F122289";


    private final String secretAccessKey="d24dhf1z9Fj6jd6Cn7A2u4u62Xnjh3Z09RvUQsxl";


    private final String bucketName = "filebase-springtest";
    @Autowired
    public FileStorageService(S3Client s3Client, DatabaseFileRepository dbFileRepository, FileChunkRepository fileChunkRepository) {
        this.s3Client = s3Client;
        this.dbFileRepository = dbFileRepository;
        this.fileChunkRepository = fileChunkRepository;

    }
//    private final ReentrantLock lock = new ReentrantLock();





//    public List<String> findFileNamesByUserIdOrderByTimestampDesc(Long userId) {
//        return dbFileRepository.findFileNamesByUserIdOrderByTimestampDesc(userId);
//    }

    public List<FileNameAndVerifyCodeProjection> findAllFileNamesAndVerifyCodes() {
        return dbFileRepository.findAllProjectedBy();
    }


    public String initializeUpload(String bucketName, String key) {
        // // 创建Multipart Upload请求
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        //// 发送请求并获取响应
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
        //从响应中获取uploadId
        // 存储uploadId以便之后引用

        return response.uploadId();
    }


//    public DatabaseFile storeFile(String fileId, String outputFileName, Optional<User> user) {
//        lock.lock(); // 在进入临界区前加锁
//        try {
//            DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
//            if (dbFile == null) {
//                // 如果dbFile为null，则执行初始化操作
//                String baseFolder = user.map(u -> "users/" + u.getEmail()).orElse("anonymous");
//                String objectKey = baseFolder + "/" + fileId + "/" + outputFileName;
//                String uploadID = initializeUpload(bucketName, objectKey);
//
//                dbFile = new DatabaseFile();
//                // 设置dbFile的属性
//                dbFile.setFileId(fileId);
//                dbFile.setFileName(outputFileName);
//                dbFile.setVerificationCode(generateUniqueVerificationCode());
//                dbFile.setShortUrl(generateUniqueShortURL());
//                dbFile.setKey(objectKey);
//                dbFile.setUploadId(uploadID);
//                user.ifPresent(dbFile::setUser);
//
//                dbFileRepository.save(dbFile);
//            }
//            return dbFile;
//        } finally {
//            lock.unlock(); // 释放锁
//        }
//    }

    public synchronized DatabaseFile storeFile(String fileId,String outputFileName, Optional<User> user){
        DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
        System.out.println("dbFile: " + dbFile);
        String baseFolder = user.map(u -> "users/" + u.getEmail())
                .orElse("anonymous");
        String objectKey = baseFolder + "/" + fileId + "/" + outputFileName;

        if (dbFile == null) {
            String uploadID=initializeUpload(bucketName, objectKey);
            // 如果不存在，創建並保存 DatabaseFile
            dbFile = new DatabaseFile(outputFileName, fileId, Instant.now());
            dbFile.setFileId(fileId); // 設置 ID
            dbFile.setVerificationCode(generateUniqueVerificationCode());
            dbFile.setShortUrl(generateUniqueShortURL());
            dbFile.setKey(objectKey);
            dbFile.setUploadId(uploadID);
            user.ifPresent(dbFile::setUser);
            dbFileRepository.save(dbFile);
        }
        return dbFile;
    }




    public void uploadPart(int chunkNumber ,DatabaseFile dbFile,String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
        // 获取之前存储的uploadId
        String uploadId = dbFile.getUploadId();
        // 确认uploadId存在
        if (uploadId == null) {
            throw new IllegalStateException("Upload has not been initialized.");
        }
        // 创建上传分片请求
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(dbFile.getKey())
                .uploadId(uploadId)
                .partNumber(chunkNumber)
                .build();
        // 上传分片
        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(fileChunk.getBytes()));
        String eTag = uploadPartResponse.eTag();

        s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(fileChunk.getBytes()));
        System.out.println(chunkNumber + " uploaded successfully");
        FileChunk fileChunkEntity = new FileChunk(chunkId, dbFile, totalChunks, chunkNumber,eTag);
        fileChunkRepository.save(fileChunkEntity);
        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
    }

    public void completeUploadChunks(DatabaseFile dbFile , String fileId) {
        List<FileChunk> chunks = dbFile.getFileChunks();

        // 根据chunkNumber排序并准备完成上传的分片信息
        List<CompletedPart> parts = chunks.stream()
                .sorted(Comparator.comparingInt(FileChunk::getChunkNumber))
                .map(chunk -> CompletedPart.builder()
                        .partNumber(chunk.getChunkNumber())
                        .eTag(chunk.getCid())
                        .build())
                .toList();

        // 获取之前存储的uploadId

         // 构造完成上传请求的分片信息
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(parts)
                .build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(dbFile.getKey())
                .uploadId(dbFile.getUploadId())
                .multipartUpload(completedMultipartUpload)
                .build();
        // // 发送完成上传请求
        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        // 清理：删除保存的uploadId
    }

    public URL generatePresignedDownloadUrlByVerificationCode(String verificationCode, Duration duration) {
        // 使用验证码找到对应的文件
        DatabaseFile dbFile = dbFileRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        // 假设您保存objectKey在DatabaseFile实体中
        String objectKey = dbFile.getKey();

        // 调用之前的方法生成预签名URL
        return generatePresignedDownloadUrl(objectKey, duration);
    }

    public URL generatePresignedDownloadUrl(String objectKey, Duration duration) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKeyId, secretAccessKey))
                .endpointOverride(URI.create("https://s3.filebase.com"))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            PresignedRequest presignedRequest = presigner.presignGetObject(b -> b.getObjectRequest(getObjectRequest)
                    .signatureDuration(duration));

            return presignedRequest.url();
        }
    }





//    private URL generatePresignedDownloadUrl(String objectKey, Duration duration) {
//        // 创建S3Presigner对象
//        S3Presigner presigner = S3Presigner.builder()
//                .region(Region.US_EAST_1) // 替换为您的S3桶所在的区域
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//
//        // 设置预签名请求的参数
//        PresignedGetObjectRequest presignedGetObjectRequest =
//                presigner.presignGetObject(builder -> builder
//                        .signatureDuration(duration) // 设置URL有效期
//                        .getObjectRequest(getObjectBuilder -> getObjectBuilder
//                                .bucket(bucketName) // 您的S3桶名称
//                                .key(objectKey) // 文件的objectKey
//                        )
//                );
//
//        // 关闭presigner以释放资源
//        presigner.close();
//
//        // 返回预签名URL
//        return presignedGetObjectRequest.url();
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

    public Optional<DatabaseFile> getFileByFileId(String fileId) {
        return dbFileRepository.findByFileId(fileId);
    }

    }




