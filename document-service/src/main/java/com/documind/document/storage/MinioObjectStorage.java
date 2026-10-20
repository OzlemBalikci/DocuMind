package com.documind.document.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MinioObjectStorage implements IObjectStorage {

	private final MinioClient minioClient;
	private final String bucket;

	public MinioObjectStorage(MinioClient minioClient, @Value("${app.minio.bucket}") String bucket) {
		this.minioClient = minioClient;
		this.bucket = bucket;
	}

	@Override
	public void put(String storagePath, InputStream body, long size, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(storagePath)
					.stream(body, size, -1)
					.contentType(contentType)
					.build());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not store object " + storagePath, ex);
		}
	}

	@Override
	public InputStream get(String storagePath) {
		try {
			return minioClient.getObject(GetObjectArgs.builder()
					.bucket(bucket)
					.object(storagePath)
					.build());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not read object " + storagePath, ex);
		}
	}

	@Override
	public void delete(String storagePath) {
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(bucket)
					.object(storagePath)
					.build());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not delete object " + storagePath, ex);
		}
	}
}
