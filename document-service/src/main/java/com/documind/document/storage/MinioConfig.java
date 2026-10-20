package com.documind.document.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
			if (!exists) {
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
			}
		};
	}
}
