package com.documind.document.messaging;

import com.documind.common.messaging.DocumentQueues;
import com.documind.common.messaging.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class DocumentEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public void publishUploaded(long documentId, long userId) {
		try {
			String body = objectMapper.writeValueAsString(new DocumentUploadedEvent(documentId, userId));
			rabbitTemplate.convertAndSend(DocumentQueues.DOCUMENT_UPLOADED, body);
		} catch (JacksonException ex) {
			throw new IllegalStateException("Could not publish document.uploaded", ex);
		}
	}
}
