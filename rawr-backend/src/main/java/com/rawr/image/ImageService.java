package com.rawr.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public ImageService(S3Client s3Client,
                        @Value("${rawr.s3.bucket}") String bucket,
                        @Value("${rawr.s3.region}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    public String upload(MultipartFile file) throws IOException {
        String key = "images/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
