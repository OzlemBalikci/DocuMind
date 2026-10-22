package com.documind.document.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.documents")
public class DocumentProperties {

	private DataSize maxFileSize = DataSize.ofMegabytes(2);
}
