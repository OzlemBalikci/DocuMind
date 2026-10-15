package com.documind.document.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

	@Bean
	MinioClient minioClient(
			@Value("${app.minio.endpoint}") String endpoint,
			@Value("${app.minio.access-key}") String accessKey,
			@Value("${app.minio.secret-key}") String secretKey) {
		return MinioClient.builder()
				.endpoint(endpoint)
				.credentials(accessKey, secretKey)
				.build();
	}

	@Bean
	ApplicationRunner documentBucket(MinioClient minioClient, @Value("${app.minio.bucket}") String bucket) {
		return args -> {
			try {
				boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
				if (!exists) {
					minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
				}
			} catch (Exception ex) {
				log.warn("MinIO bucket '{}' was not created: {}", bucket, ex.getMessage());
			}
		};
	}
}
