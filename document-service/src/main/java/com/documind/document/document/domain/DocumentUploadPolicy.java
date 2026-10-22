package com.documind.document.document.domain;

import com.documind.backend.common.exception.FileTooLargeException;
import com.documind.backend.common.exception.UnsupportedFileTypeException;
import com.documind.document.common.config.DocumentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentUploadPolicy {

	static final Set<String> ALLOWED_TYPES = Set.of(
			"application/pdf",
			"text/plain",
			"text/markdown");

	private static final Map<String, String> BY_EXTENSION = Map.of(
			"pdf", "application/pdf",
			"txt", "text/plain",
			"md", "text/markdown",
			"markdown", "text/markdown");

	private final DocumentProperties properties;

	public String requireContentType(String rawType, String filename) {
		String declared = mediaType(rawType);
		if (ALLOWED_TYPES.contains(declared)) {
			return declared;
		}
		String fromName = BY_EXTENSION.get(extension(filename));
		if (fromName != null && (declared == null || declared.equals("application/octet-stream"))) {
			return fromName;
		}
		throw new UnsupportedFileTypeException();
	}

	public void requireSize(long size) {
		if (size <= 0) {
			throw new InvalidDocumentFileException("The uploaded file is empty");
		}
		if (size > properties.getMaxFileSize().toBytes()) {
			throw new FileTooLargeException();
		}
	}

	public String originalFilename(String raw) {
		String base = basename(raw);
		if (base.isBlank()) {
			return "document";
		}
		return base.length() <= 255 ? base : base.substring(0, 255);
	}

	public String title(String requested, String originalFilename) {
		String value = requested == null || requested.isBlank() ? originalFilename : requested.trim();
		if (value.isBlank() || value.length() > 255) {
			throw new InvalidDocumentFileException("Title must be at most 255 characters");
		}
		return value;
	}

	public String storagePath(UUID ownerId, String originalFilename) {
		String name = originalFilename.replaceAll("[^A-Za-z0-9._-]", "_");
		if (name.isBlank() || name.equals(".") || name.equals("..")) {
			name = "document";
		}
		String prefix = ownerId + "/" + UUID.randomUUID() + "/";
		int room = 500 - prefix.length();
		if (name.length() > room) {
			name = name.substring(0, Math.max(room, 1));
		}
		return prefix + name;
	}

	public String checksum(MultipartFile file) {
		try (InputStream in = file.getInputStream()) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) >= 0) {
				digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not supported", ex);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not read upload", ex);
		}
	}

	private static String mediaType(String rawType) {
		if (rawType == null || rawType.isBlank()) {
			return null;
		}
		String type = rawType.toLowerCase(Locale.ROOT);
		int separator = type.indexOf(';');
		if (separator >= 0) {
			type = type.substring(0, separator);
		}
		return type.trim();
	}

	private static String extension(String filename) {
		String base = basename(filename).toLowerCase(Locale.ROOT);
		int dot = base.lastIndexOf('.');
		if (dot < 0 || dot == base.length() - 1) {
			return "";
		}
		return base.substring(dot + 1);
	}

	private static String basename(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String name = raw.replace('\\', '/').trim();
		int slash = name.lastIndexOf('/');
		return slash >= 0 ? name.substring(slash + 1).trim() : name;
	}
}
