package sendeverything.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.service.IpfsClusterService;

import java.io.IOException;
import java.security.Principal;

@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class FileUpIpfsController {
    @Autowired
    private final IpfsClusterService ipfsClusterService;

    public FileUpIpfsController(IpfsClusterService ipfsClusterService) {
        this.ipfsClusterService = ipfsClusterService;
    }


    @PostMapping("/uploadFileToIpfs")
    public ResponseEntity<?> uploadFileToIpfs(@RequestParam("fileChunk") MultipartFile fileChunk)
                                               throws IOException {
        ipfsClusterService.addFileToCluster(fileChunk);
        return ResponseEntity.ok().build();
    }
}