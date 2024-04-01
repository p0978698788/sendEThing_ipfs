package sendeverything.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;

@Configuration
public class S3Config {

//    @Value("${filebase.accessKeyId}")
    private String accessKeyId="9D950C2D6B3E6F122289";

//    @Value("${filebase.secretAccessKey}")
    private String secretAccessKey="d24dhf1z9Fj6jd6Cn7A2u4u62Xnjh3Z09RvUQsxl";

//    @Value("${filebase.endpoint}")
    private String endpoint="https://s3.filebase.com";

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // Filebase 使用的是 US-EAST-1 区域，根据你的情况调整
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
