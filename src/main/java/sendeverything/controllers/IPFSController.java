package sendeverything.controllers;

import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.User;
import sendeverything.payload.response.FileNameResponse;
import sendeverything.payload.response.FileResponse;
import sendeverything.repository.DatabaseFileRepository;
import sendeverything.repository.FileChunkRepository;
import sendeverything.repository.UserRepository;
import sendeverything.service.CodeGenerator;
import sendeverything.service.IPFSUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class IPFSController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileChunkRepository fileChunkRepository;
    @Autowired
    private CodeGenerator codeGenerator;
    @Autowired
    private DatabaseFileRepository dbFileRepository;
    @Autowired
    private IPFSUtils IPFSUtils;



//    @PostMapping("/uploadChunk")
//    public ResponseEntity<?> uploadChunk(@RequestParam("fileChunk") MultipartFile fileChunk,
//                                         @RequestParam("chunkNumber") int chunkNumber,
//                                         @RequestParam("totalChunks") int totalChunks,
//                                         @RequestParam("fileId") String fileId,
//                                         @RequestParam("chunkId") String chunkId,
//                                         @RequestParam("size") String fileSize,
//                                         @RequestParam("outputFileName") String outputFileName,
//                                         Principal principal) throws IOException {
//        System.out.println("Principal: " + principal);
//        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
//
//
//        DatabaseFile dbFile= IPFSUtils.storeFile(fileId,outputFileName,optionalUser);
//
//
//        System.out.println("Uploading chunk " + chunkNumber + " of file " + fileId);
////        FileChunk dbfileChunk = fileChunkRepository.findByChunkId(chunkId).orElse(null);
//
//        FileChunk dbfileChunk = fileChunkRepository.findByChunkIdAndDatabaseFile_FileId(chunkId,fileId).orElse(null);
//        if (dbfileChunk == null) {
//            IPFSUtils.uploadPart(chunkNumber ,dbFile,chunkId, fileChunk,totalChunks);
//
//            return ResponseEntity.ok("Chunk " + chunkNumber + " uploaded successfully");
//        }else {
//            System.out.println("Chunk " + chunkNumber + " already uploaded");
//            return ResponseEntity.ok("Chunk " + chunkNumber + " already uploaded");
//        }
//    }



    @PostMapping("/uploadChunk")
    public ResponseEntity<?> uploadChunk(@RequestParam("fileChunk") MultipartFile fileChunk,
                                         @RequestParam("chunkNumber") int chunkNumber,
                                         @RequestParam("totalChunks") int totalChunks,
                                         @RequestParam("fileId") String fileId,
                                         @RequestParam("chunkId") String chunkId,
                                         @RequestParam("size") Long fileSize,
                                         @RequestParam("outputFileName") String outputFileName,
                                         Principal principal) throws IOException {
        System.out.println("Principal: " + principal);
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();

        // 先尝试查找文件，避免不必要的同步操作
        DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);

        if (dbFile == null) {
            synchronized (IPFSUtils.class) {
                // 再次检查确保没有其他线程已经创建了文件
                dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
                if (dbFile == null) {
                    dbFile = IPFSUtils.storeFile(fileId, outputFileName, optionalUser,fileSize);
                }
            }
        }

        System.out.println("Uploading chunk " + chunkNumber + " of file " + fileId);
        FileChunk dbfileChunk = fileChunkRepository.findByChunkIdAndDatabaseFile_FileId(chunkId, fileId).orElse(null);
        if (dbfileChunk == null) {
            IPFSUtils.uploadPart(chunkNumber, dbFile, chunkId, fileChunk, totalChunks);

            return ResponseEntity.ok("Chunk " + chunkNumber + " uploaded successfully");
        } else {
            System.out.println("Chunk " + chunkNumber + " already uploaded");
            return ResponseEntity.ok("Chunk " + chunkNumber + " already uploaded");
        }
    }

    @PostMapping("/completeUpload")
    public FileResponse completeUpload(
            @RequestParam("outputFileName") String outputFileName,
            @RequestParam("fileId") String fileId
    ) throws IOException, SQLException, WriterException {
        Optional<DatabaseFile> dbFileOptional = IPFSUtils.getFileByFileId(fileId);
        // 确保找到了文件
        if (dbFileOptional.isEmpty()) {
            // 文件未找到，处理错误情况，例如抛出异常或返回错误响应
            throw new FileNotFoundException("File not found with fileId: " + fileId);
        }

        DatabaseFile dbFile = dbFileOptional.get();
        List<FileChunk> sortedChunks = dbFile.getFileChunks().stream()
                .sorted(Comparator.comparingInt(FileChunk::getChunkNumber))
                .toList();

        System.out.println("Sorted dbFile chunks: " + sortedChunks);

        Blob qrCodeBlob = codeGenerator.generateAndStoreQRCode(350, 350, dbFile);
        String qrCodeBase64 = codeGenerator.blobToBase64(qrCodeBlob);
//        List<FileNameAndVerifyCodeProjection> fileNames = fileService.findAllFileNamesAndVerifyCodes();
//        for (FileNameAndVerifyCodeProjection fileName : fileNames) {
//            System.out.println("fileName: " + fileName.getFileName());
//            System.out.println("verificationCode: " + fileName.getVerificationCode());
//        }

//        System.out.println("fileNames: " + fileNames);

        return new FileResponse(dbFile.getVerificationCode(), qrCodeBase64);
    }



    @GetMapping("/downloadFileByCode/{verificationCode}")
    public ResponseEntity<?> downloadFile(@PathVariable String verificationCode, HttpServletResponse response) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(dbFile.getFileName(), StandardCharsets.UTF_8);
            System.out.println("dbFile: " + dbFile.getFileChunks());
//
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(dbFile.getFileSize()));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_TYPE,"application/octet-stream");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);





            IPFSUtils.writeToResponseStreamConcurrently3(dbFile, response);
            System.out.println(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }






//    @GetMapping("/download/{hash}")
//    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String hash) {
//        try {
//
//            DatabaseFile dbFile = IPFSUtils.findByCid(hash);
//            byte[] data = IPFSUtils.download(hash);
//            if (data != null) {
//                ByteArrayResource resource = new ByteArrayResource(data);
//                assert dbFile != null;
//                return ResponseEntity.ok()
//                        .contentLength(data.length)
//                        .header("Content-Disposition", "attachment; filename=\"" + dbFile.getFileName() + "\"")
//                        .body(resource);
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.internalServerError().build();
//        }
//    }
    }


    @GetMapping("/cleanupByCode/{verificationCode}")
    public ResponseEntity<?> cleanupResources(@PathVariable String verificationCode) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }

            System.out.println("Cleaning up resources for dbFile: " + dbFile.getFileChunks());
            IPFSUtils.unpinAndCollectGarbage(dbFile);

            return ResponseEntity.ok().body("Cleanup successful for file with verification code: " + verificationCode);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/getFileNameByCode/{verificationCode}")
    public ResponseEntity<?> getFileNameByCode(@PathVariable String verificationCode) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }
            FileResponse fileNameResponse = new FileResponse(dbFile.getFileName());

            return ResponseEntity.ok().body(fileNameResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}















//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
//@RestController
//@RequestMapping("/api/auth")
//public class IpfsController {
//
//    private final IpfsService ipfsService;
//
//    public IpfsController(IpfsService ipfsService) {
//        this.ipfsService = ipfsService;
//    }
//
//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
//        try {
//            byte[] data = file.getBytes();
//            String cid = ipfsService.addFile(data);
//            return new ResponseEntity<>(cid, HttpStatus.OK);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return new ResponseEntity<>("Error uploading file to IPFS", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @GetMapping("/retrieve")
//    public ResponseEntity<byte[]> retrieveFile(@RequestParam("cid") String cid) {
//        try {
//            byte[] data = ipfsService.getFile(cid);
//            HttpHeaders headers = new HttpHeaders();
//
//            headers.setContentDispositionFormData("attachment", cid);
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//            return ResponseEntity.ok()
//                    .headers(headers)
//                    .body(data);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error retrieving file from IPFS".getBytes());
//        }
//    }
//
//}