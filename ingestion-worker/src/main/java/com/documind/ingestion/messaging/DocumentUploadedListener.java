package com.documind.ingestion.messaging;

import com.documind.common.messaging.DocumentQueues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Slf4j
@Configuration
public class DocumentUploadedListener {

	@Bean
	Queue documentUploadedQueue() {
		return new Queue(DocumentQueues.DOCUMENT_UPLOADED, true);
	}

	@Component
	static class Handler {

		@RabbitListener(queues = DocumentQueues.DOCUMENT_UPLOADED)
		public void onUploaded(String body) {
			log.info("Received document.uploaded {}", body);
		}
	}
}
