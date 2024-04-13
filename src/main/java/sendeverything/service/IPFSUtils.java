package sendeverything.service;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.sql.exec.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.User;
import sendeverything.repository.DatabaseFileRepository;
import sendeverything.repository.FileChunkProjection;
import sendeverything.repository.FileChunkRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;


/**
 * @Author: luowenxing
 * @Date: 2021/5/31 21:30
 */
@Component
public class IPFSUtils {



    @Autowired
    private  DatabaseFileRepository dbFileRepository;
    @Autowired
    private  FileChunkRepository fileChunkRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
//    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 根据需要调整线程池大小


    public IPFSUtils(DatabaseFileRepository dbFileRepository,FileChunkRepository fileChunkRepository) {
        this.dbFileRepository = dbFileRepository;
        this.fileChunkRepository = fileChunkRepository;
    }
    /**
     * ipfs的服务器地址和端口,替换成自己的ip，port
     */
    private final IPFS IPFS = new IPFS("/ip4/127.0.0.1/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/140.130.33.153/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/169.254.183.111/tcp/5001");



    public DatabaseFile findByFileId(String fileId) {
        return dbFileRepository.findByFileId(fileId).orElse(null);
    }




//    public static synchronized DatabaseFile storeFile(String fileId, String outputFileName, Optional<User> user){
//        DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
//        System.out.println("dbFile: " + dbFile);
//        if (dbFile == null) {
//            // 如果不存在，創建並保存 DatabaseFile
//            dbFile = new DatabaseFile(outputFileName, fileId, Instant.now());
//            dbFile.setFileId(fileId); // 設置 ID
//            dbFile.setVerificationCode(generateUniqueVerificationCode());
//            user.ifPresent(dbFile::setUser);
//            dbFileRepository.save(dbFile);
//        }
//        return dbFile;
//    }

    public  synchronized DatabaseFile storeFile(String fileId, String outputFileName, Optional<User> user,Long filesize){
        // 这里不需要再次检查文件是否存在，因为这已在调用此方法之前检查过
        DatabaseFile dbFile = new DatabaseFile(outputFileName, fileId, Instant.now());
        dbFile.setFileId(fileId); // 设置 ID
        dbFile.setVerificationCode(generateUniqueVerificationCode());
        dbFile.setFileSize(filesize);
        user.ifPresent(dbFile::setUser);
        dbFileRepository.save(dbFile);
        return dbFile;
    }


    public  void uploadPart(int chunkNumber, DatabaseFile dbFile, String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
        String cid= upload(fileChunk.getBytes());
        FileChunk fileChunkEntity = new FileChunk(chunkId, dbFile, totalChunks, chunkNumber,cid);
        // 创建上传分片请求
        System.out.println(chunkNumber + " uploaded successfully");
        fileChunkRepository.save(fileChunkEntity);
        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
    }
    public  String upload(byte[] data) throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode addResult = IPFS.add(file).get(0);
        return addResult.hash.toString();
    }

    public  Optional<DatabaseFile> getFileByFileId(String fileId) {
        return dbFileRepository.findByFileId(fileId);
    }



    public  byte[] download(String hash) {
        byte[] data = null;
        try {
            data = IPFS.cat(Multihash.fromBase58(hash));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public  void download(String hash, String destFile) {
        byte[] data = null;
        try {
            data = IPFS.cat(Multihash.fromBase58(hash));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data != null && data.length > 0) {
            File file = new File(destFile);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    public byte[] downloadChunk(String cid) throws IOException {
        // 假设 IPFS 已经在构造函数中被初始化
        Multihash filePointer = Multihash.fromBase58(cid);
        return IPFS.cat(filePointer);
    }


    public void writeToResponseStreamInBatches(DatabaseFile dbFile, HttpServletResponse response) throws IOException {
        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
        int batchSize = 10;  // 每批处理5个分片
        List<byte[]> batch = new ArrayList<>(batchSize);

        for (FileChunk chunk : chunks) {
            // 从IPFS下载分片
            byte[] data = downloadChunk(chunk.getCid());
            batch.add(data);

            // 检查批量大小，如果达到，则写入响应流
            if (batch.size() == batchSize) {
                writeBatchToResponse(batch, response);
                batch.clear(); // 清空批量缓存，以便下一批处理
            }
        }

        // 处理剩余的分片（如果有）
        if (!batch.isEmpty()) {
            writeBatchToResponse(batch, response);
        }

        response.getOutputStream().flush();  // 确保所有内容都已经写入
    }

    private void writeBatchToResponse(List<byte[]> batch, HttpServletResponse response) throws IOException {
        for (byte[] data : batch) {
            response.getOutputStream().write(data);
        }
    }







    //流式下載
    public void writeToResponseStreamConcurrently(DatabaseFile dbFile, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException, java.util.concurrent.ExecutionException {
        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 可调整线程池大小

        List<Callable<byte[]>> tasks = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            int chunkNumber = chunk.getChunkNumber(); // 获取分片编号
            tasks.add(() -> {
                byte[] data = downloadChunk(chunk.getCid());
                System.out.println("Chunk " + chunkNumber + " downloaded.");
                return data;
            });
        }

        List<Future<byte[]>> futures = executorService.invokeAll(tasks);

        for (Future<byte[]> future : futures) {
            byte[] data = future.get(); // 这里可能抛出异常，表示任务执行失败
            // 这里不再打印分片编号，因为它们是按顺序处理的
            response.getOutputStream().write(data);
            // 适当的位置释放内存
            Arrays.fill(data, (byte) 0); // 如果适用，可以清除数据
        }

        executorService.shutdown(); // 关闭线程池
    }







//    public void writeToResponseStreamConcurrently3(DatabaseFile dbFile, HttpServletResponse response) throws IOException, java.util.concurrent.ExecutionException, InterruptedException {
//        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
//        int batchSize = 5; // 假设每批次处理10个分片
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//
//        for (int i = 0; i < chunks.size(); i += batchSize) {
//            List<FileChunk> batch = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
//            List<Future<byte[]>> futures = new ArrayList<>();
//
//            for (FileChunk chunk : batch) {
//                Callable<byte[]> task = () -> {
//                    byte[] data = downloadChunk(chunk.getCid());
//                    System.out.println("Chunk " + chunk.getChunkNumber() + " downloaded.");
//                    return data;
//                };
//                futures.add(executorService.submit(task));
//            }
//
//            // 在每个批次内，等待所有分片下载完成
//            for (Future<byte[]> future : futures) {
//                try {
//                    byte[] data = future.get();
//                    response.getOutputStream().write(data);
//                    Arrays.fill(data, (byte) 0);
//                } catch (ExecutionException | InterruptedException e) {
//                    System.err.println("Error processing chunk: " + e.getMessage());
//                    e.printStackTrace();  // 或者更合适的异常处理逻辑
//                }
//            }
//        }
//
//        executorService.shutdown();
//    }



    public void writeToResponseStreamConcurrently3(DatabaseFile dbFile, HttpServletResponse response) throws IOException {
        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
        int totalChunks = chunks.size();
        int batchSize = 5;  // 假设每批次处理5个分片
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        int chunksDownloaded = 0;

        try {
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<FileChunk> batch = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
                List<Future<byte[]>> futures = new ArrayList<>();

                for (FileChunk chunk : batch) {
                    Callable<byte[]> task = () -> {
                        byte[] data = downloadChunk(chunk.getCid());
                        System.out.println("Chunk " + chunk.getChunkNumber() + " downloaded.");
                        return data;
                    };
                    futures.add(executorService.submit(task));
                }

                for (Future<byte[]> future : futures) {
                    try {
                        byte[] data = future.get();
                        response.getOutputStream().write(data);
                        chunksDownloaded++;
                        double progress = (double) chunksDownloaded / totalChunks * 100;
                        // 发送进度信息
                        messagingTemplate.convertAndSend("/topic/downloadProgress", progress);

                        Arrays.fill(data, (byte) 0); // 清理敏感数据
                    } catch (ExecutionException | InterruptedException e) {
                        System.err.println("Error processing chunk: " + e.getMessage());
                        e.printStackTrace();
                    } catch (java.util.concurrent.ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
            executorService.shutdown();
        }
    }


    //    public void writeToResponseStreamConcurrently(DatabaseFile dbFile, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
//
//        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
//        List<Callable<Void>> tasks = new ArrayList<>();
//
//        for (FileChunk chunk : chunks) {
//            tasks.add(() -> {
//                byte[] data = downloadChunk(chunk.getCid());
//                synchronized (response) {
//                    response.getOutputStream().write(data);
//                }
//                return null;
//            });
//        }
//
//        executorService.invokeAll(tasks);
//    }
    private  String generateUniqueVerificationCode() {
        Random random = new Random();
        String verificationCode;
        do {
            int code = random.nextInt(900000) + 100000;
            verificationCode = String.valueOf(code);
        } while (isCodeExists(verificationCode));

        return verificationCode;
    }
    private  boolean isCodeExists(String code) {
        return dbFileRepository.existsById(code);
    }

    public  DatabaseFile findByVerificationCode(String verificationCode) {
        return dbFileRepository.findByVerificationCode(verificationCode).orElse(null);
    }

    public void unpinAndCollectGarbage(DatabaseFile dbFile) throws IOException {
        for (FileChunk chunk : dbFile.getFileChunks()) {
            String cid = chunk.getCid();
            System.out.println(cid);
            IPFS.pin.rm(Multihash.fromBase58(cid));
            System.out.println("Unpinned CID: " + cid);
        }

        IPFS.repo.gc();
        dbFileRepository.delete(dbFile);
        System.out.println("Garbage collection executed.");
    }
}




