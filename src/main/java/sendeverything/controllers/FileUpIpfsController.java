//package sendeverything.controllers;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import sendeverything.models.DatabaseFile;
//import sendeverything.models.FileChunk;
//import sendeverything.models.User;
//import sendeverything.repository.DatabaseFileRepository;
//import sendeverything.repository.FileChunkRepository;
//import sendeverything.repository.UserRepository;
//import sendeverything.service.CodeGenerator;
//import sendeverything.service.IPFSUtils;
//
//
//import java.io.IOException;
//import java.security.Principal;
//import java.util.Optional;
//
//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
//@RestController
//@RequestMapping("/api/auth")
//public class FileUpIpfsController {
//
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private FileChunkRepository fileChunkRepository;
//    @Autowired
//    private CodeGenerator codeGenerator;
//    @Autowired
//    private DatabaseFileRepository dbFileRepository;
//
//
//    @PostMapping("/uploadChunk1")
//    public ResponseEntity<?> uploadChunk(@RequestParam("fileChunk") MultipartFile fileChunk,
//                                         @RequestParam("chunkNumber") int chunkNumber,
//                                         @RequestParam("totalChunks") int totalChunks,
//                                         @RequestParam("fileId") String fileId,
//                                         @RequestParam("chunkId") String chunkId,
//                                         @RequestParam("size") Long fileSize,
//                                         @RequestParam("outputFileName") String outputFileName,
//                                         Principal principal) throws IOException {
//        System.out.println("Principal: " + principal);
//        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
//
//        // 先尝试查找文件，避免不必要的同步操作
//        DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
//
//        if (dbFile == null) {
//            synchronized (IPFSUtils.class) {
//                // 再次检查确保没有其他线程已经创建了文件
//                dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
//                if (dbFile == null) {
//                    dbFile = ipfsClusterService.storeFile(fileId, outputFileName, optionalUser,fileSize);
//                }
//            }
//        }
//
//        System.out.println("Uploading chunk " + chunkNumber + " of file " + fileId);
//        FileChunk dbfileChunk = fileChunkRepository.findByChunkIdAndDatabaseFile_FileId(chunkId, fileId).orElse(null);
//        if (dbfileChunk == null) {
//            ipfsClusterService.uploadPart(chunkNumber, dbFile, chunkId, fileChunk, totalChunks);
//
//            return ResponseEntity.ok("Chunk " + chunkNumber + " uploaded successfully");
//        } else {
//            System.out.println("Chunk " + chunkNumber + " already uploaded");
//            return ResponseEntity.ok("Chunk " + chunkNumber + " already uploaded");
//        }
//    }
//
//
//
//
//}