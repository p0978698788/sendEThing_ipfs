//package sendeverything.service;
//
//import io.ipfs.api.IPFS;
//import io.ipfs.api.MerkleNode;
//import io.ipfs.api.NamedStreamable;
//import io.ipfs.multihash.Multihash;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.apache.http.HttpEntity;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import org.hibernate.sql.exec.ExecutionException;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import org.springframework.boot.configurationprocessor.json.JSONException;
//import org.springframework.boot.configurationprocessor.json.JSONObject;
//import org.springframework.core.io.ByteArrayResource;
//
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.multipart.MultipartFile;
//import sendeverything.models.DatabaseFile;
//import sendeverything.models.FileChunk;
//import sendeverything.models.User;
//import sendeverything.repository.DatabaseFileRepository;
//import sendeverything.repository.FileChunkRepository;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.*;
//
//@Service
//public class IpfsClusterService {
//    @Autowired
//    private DatabaseFileRepository dbFileRepository;
//    @Autowired
//    private FileChunkRepository fileChunkRepository;
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    private final io.ipfs.api.IPFS IPFS = new IPFS("/ip4/127.0.0.1/tcp/5001");
//
//    public IpfsClusterService(DatabaseFileRepository dbFileRepository,FileChunkRepository fileChunkRepository) {
//        this.dbFileRepository = dbFileRepository;
//        this.fileChunkRepository = fileChunkRepository;
//    }
//    private final String clusterApiUrl = "http://140.130.33.153:9094";
//
//
//    public  synchronized DatabaseFile storeFile(String fileId, String outputFileName, Optional<User> user, Long filesize){
//        // 这里不需要再次检查文件是否存在，因为这已在调用此方法之前检查过
//        LocalDateTime createTime = LocalDateTime.now();
//        DatabaseFile dbFile = new DatabaseFile(outputFileName, fileId, createTime);
//        dbFile.setFileId(fileId); // 设置 ID
//        dbFile.setVerificationCode(generateUniqueVerificationCode());
//        dbFile.setFileSize(filesize);
//        user.ifPresent(dbFile::setUser);
//        dbFileRepository.save(dbFile);
//        return dbFile;
//    }
//
//    public File convertToFile(MultipartFile multipartFile) throws IOException {
//        File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
//        try (InputStream is = multipartFile.getInputStream();
//             FileOutputStream fos = new FileOutputStream(file)) {
//            int read;
//            byte[] bytes = new byte[1024];
//            while ((read = is.read(bytes)) != -1) {
//                fos.write(bytes, 0, read);
//            }return file;
//        }}
//
//
//    public  void uploadPart(int chunkNumber, DatabaseFile dbFile, String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
//        String cid= uploadFileToCluster(chunkNumber, dbFile, fileChunk, totalChunks);
//        FileChunk fileChunkEntity = new FileChunk(chunkId, dbFile, totalChunks, chunkNumber,cid);
//        // 创建上传分片请求
//        System.out.println(chunkNumber + " uploaded successfully");
//        fileChunkRepository.save(fileChunkEntity);
//        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
//    }
//
//
//    public String uploadFileToCluster(int chunkNumber, DatabaseFile dbFile, MultipartFile fileChunk, int totalChunks) throws IOException {
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        HttpPost post = new HttpPost(clusterApiUrl + "/add");
//        File file = convertToFile(fileChunk);
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        String fileName = dbFile.getFileName() + totalChunks + "_" + chunkNumber;
//        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, fileName);
//        post.setEntity(builder.build());
//
//        try (CloseableHttpResponse response = httpClient.execute(post)) {
//            HttpEntity entity = response.getEntity();
//            String responseString = EntityUtils.toString(entity);
//            JSONObject jsonResponse = new JSONObject(responseString);
//            return jsonResponse.getString("cid"); // 假設響應中 CID 的鍵是 'Hash'
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        } finally {
//            httpClient.close();
//        }
//    }
//    private  String generateUniqueVerificationCode() {
//        Random random = new Random();
//        String verificationCode;
//        do {
//            int code = random.nextInt(900000) + 100000;
//            verificationCode = String.valueOf(code);
//        } while (isCodeExists(verificationCode));
//
//        return verificationCode;
//    }
//    private  boolean isCodeExists(String code) {
//        return dbFileRepository.existsById(code);
//    }
//    public  Optional<DatabaseFile> getFileByFileId(String fileId) {
//        return dbFileRepository.findByFileId(fileId);
//    }
//
//
//    public byte[] downloadChunk(String cid) throws IOException {
//        // 假设 IPFS 已经在构造函数中被初始化
//        Multihash filePointer = Multihash.fromBase58(cid);
//        return IPFS.cat(filePointer);
//    }
//
//
//    public void writeToResponseStreamConcurrently3(DatabaseFile dbFile, HttpServletResponse response, String uuid) throws IOException {
//        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
//        int totalChunks = chunks.size();
//        int batchSize = 5;
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        int chunksDownloaded = 0;
//
//        try {
//            for (int i = 0; i < chunks.size(); i += batchSize) {
//                List<FileChunk> batch = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
//                List<Future<byte[]>> futures = new ArrayList<>();
//
//                for (FileChunk chunk : batch) {
//                    Callable<byte[]> task = () -> {
//                        byte[] data = downloadChunk(chunk.getCid());
//                        System.out.println("Chunk " + chunk.getChunkNumber() + " downloaded.");
//                        return data;
//                    };
//                    futures.add(executorService.submit(task));
//                }
//
//                for (Future<byte[]> future : futures) {
//                    try {
//                        byte[] data = future.get();
//                        response.getOutputStream().write(data);
//                        chunksDownloaded++;
//                        double progress = (double) chunksDownloaded / totalChunks * 100;
////                        updateDownloadProgress(uuid, progress);
//                        Arrays.fill(data, (byte) 0);
//                    } catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException e) {
//                        System.err.println("Error processing chunk: " + e.getMessage());
//                        if (e.getCause() instanceof IOException) {
//                            throw (IOException) e.getCause(); // 抛出IOException来处理客户端断开连接的情况
//                        }
//                        e.printStackTrace();
//                    }
//                }
//            }
//        } catch (IOException e) {
//            System.err.println("Client aborted connection: " + e.getMessage());
//            // 处理客户端中断连接的情况，例如可以选择不再写入响应
//        } finally {
//            executorService.shutdown();
//            try {
//                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                    executorService.shutdownNow();
//                }
//            } catch (InterruptedException ex) {
//                executorService.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//}