package sendeverything.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MultipartUploadService {
    private final S3Client s3Client;
    // 用于存储每个文件上传的uploadId，通常你可能需要一个更持久的存储方案
//    private final Map<String, String> uploadIdMap = new HashMap<>();
//    // // 初始化Multipart Upload并返回uploadId
//    public String initializeUpload(String bucketName, String key) {
//        // // 创建Multipart Upload请求
//        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .build();
//        //// 发送请求并获取响应
//        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
//        //从响应中获取uploadId
//        String uploadId = response.uploadId();
//        // 存储uploadId以便之后引用
//        uploadIdMap.put(key, uploadId);
//        return uploadId;
//    }
//    //// 上传一个分片到S3
//    public void uploadPart(String bucketName, String key, int partNumber, byte[] data) {
//        // 获取之前存储的uploadId
//        String uploadId = uploadIdMap.get(key);
//        // 确认uploadId存在
//        if (uploadId == null) {
//            throw new IllegalStateException("Upload has not been initialized.");
//        }
//        // 创建上传分片请求
//        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .uploadId(uploadId)
//                .partNumber(partNumber)
//                .build();
//        // 上传分片
//        s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(data));
//        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
//    }
//     // 完成Multipart Upload，合并所有上传的分片
//    public void completeUpload(String bucketName, String key, List<CompletedPart> parts) {
//        //// 获取之前存储的uploadId
//        String uploadId = uploadIdMap.get(key);
//        // // 构造完成上传请求的分片信息
//        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
//                .parts(parts)
//                .build();
//        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .uploadId(uploadId)
//                .multipartUpload(completedMultipartUpload)
//                .build();
//        // // 发送完成上传请求
//        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
//        // 清理：删除保存的uploadId
//        uploadIdMap.remove(key);
//    }
}
