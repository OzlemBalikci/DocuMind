package com.documind.document.storage;

import java.io.InputStream;

public interface IObjectStorage {

	void put(String storagePath, InputStream body, long size, String contentType);

	InputStream get(String storagePath);

	void delete(String storagePath);
}
