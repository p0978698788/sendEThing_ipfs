package sendeverything.service.room;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.sql.exec.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import sendeverything.models.User;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.DBRoomFileChunk;
import sendeverything.models.room.Room;
import sendeverything.repository.DBRoomFileChunkRepository;
import sendeverything.repository.DBRoomFileRepository;
import sendeverything.repository.RoomRepository;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @Author: luowenxing
 * @Date: 2021/5/31 21:30
 */
@Component
public class RoomIPFSUtils {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DBRoomFileChunkRepository dbRoomFileChunkRepository;
    @Autowired
    private DBRoomFileRepository dbRoomFileRepository;

//    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 根据需要调整线程池大小

    public RoomIPFSUtils( RoomRepository roomRepository,DBRoomFileChunkRepository dbRoomFileChunkRepository,  DBRoomFileRepository dbRoomFileRepository) {
        this.roomRepository = roomRepository;
        this.dbRoomFileChunkRepository = dbRoomFileChunkRepository;
        this.dbRoomFileRepository = dbRoomFileRepository;
    }

    /**
     * ipfs的服务器地址和端口,替换成自己的ip，port
     */
    private final IPFS IPFS = new IPFS("/ip4/127.0.0.1/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/140.130.33.153/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/169.254.183.111/tcp/5001");



    public DBRoomFile findByFileId(String fileId) {
        return dbRoomFileRepository.findByFileId(fileId).orElse(null);
    }





    public  synchronized DBRoomFile storeFile(String fileId, String outputFileName, Optional<User> user,Long filesize,String description,String roomCode){
        // 这里不需要再次检查文件是否存在，因为这已在调用此方法之前检查过
        Room room=roomRepository.findByRoomCode(roomCode);
        LocalDateTime createTime = LocalDateTime.now();
        DBRoomFile dbFile = new DBRoomFile(outputFileName, fileId, createTime);
        dbFile.setFileId(fileId); // 设置 ID
        dbFile.setVerificationCode(generateUniqueVerificationCode());
        dbFile.setFileSize(filesize);
        dbFile.setRoom(room);
        dbFile.setDescription(description);
        user.ifPresent(dbFile::setUser);
        dbRoomFileRepository.save(dbFile);
        return dbFile;
    }


    public  void uploadPart(int chunkNumber, DBRoomFile dbFile, String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
        String cid= upload(fileChunk.getBytes());
        DBRoomFileChunk fileChunkEntity = new DBRoomFileChunk(chunkId, dbFile, totalChunks, chunkNumber,cid);
        // 创建上传分片请求
        System.out.println(chunkNumber + " uploaded successfully");
        dbRoomFileChunkRepository.save(fileChunkEntity);
        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
    }
    public  String upload(byte[] data) throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode addResult = IPFS.add(file).get(0);
        return addResult.hash.toString();
    }

    public  Optional<DBRoomFile> getFileByFileId(String fileId) {
        return dbRoomFileRepository.findByFileId(fileId);
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
    public void writeToResponseStreamConcurrently(DBRoomFile dbFile, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException, java.util.concurrent.ExecutionException {
        List<DBRoomFileChunk> chunks = dbRoomFileChunkRepository.findByDbRoomFileOrderByChunkNumberAsc(dbFile);
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 可调整线程池大小

        // 使用 Callable 而不是 Runnable，以便可以返回结果
        List<Callable<byte[]>> tasks = new ArrayList<>();

        for (DBRoomFileChunk chunk : chunks) {
            tasks.add(() -> downloadChunk(chunk.getCid()));
            System.out.println(chunk.getId());
        }

        List<Future<byte[]>> futures = executorService.invokeAll(tasks);

        // 按原始顺序写入响应流
        for (Future<byte[]> future : futures) {
            response.getOutputStream().write(future.get());
        }

        executorService.shutdown(); // 关闭线程池
    }


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
        return dbRoomFileRepository.existsById(Long.valueOf(code));
    }

    public  DBRoomFile findByVerificationCode(String verificationCode) {
        return dbRoomFileRepository.findByVerificationCode(verificationCode).orElse(null);
    }

    public void unpinAndCollectGarbage(DBRoomFile dbFile) throws IOException {
        for (DBRoomFileChunk chunk : dbFile.getDbRoomFileChunks()) {
            String cid = chunk.getCid();
            IPFS.pin.rm(Multihash.fromBase58(cid));
            System.out.println("Unpinned CID: " + cid);
        }

        IPFS.repo.gc();
        dbRoomFileRepository.delete(dbFile);
        System.out.println("Garbage collection executed.");
    }
}




