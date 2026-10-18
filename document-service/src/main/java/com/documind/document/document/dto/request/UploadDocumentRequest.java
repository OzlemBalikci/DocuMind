package com.documind.document.document.dto.request;

import jakarta.validation.constraints.Size;

public record UploadDocumentRequest(
        @Size(max = 255) String title
) {
}
