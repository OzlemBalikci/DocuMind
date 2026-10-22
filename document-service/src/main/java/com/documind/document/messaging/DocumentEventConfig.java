package com.documind.document.messaging;

import com.documind.common.messaging.DocumentQueues;
import com.documind.common.messaging.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnBean(RabbitTemplate.class)
@RequiredArgsConstructor
public class DocumentEventConfig {

	private final ObjectMapper objectMapper;

	@Bean
	Queue documentUploadedQueue() {
		return new Queue(DocumentQueues.DOCUMENT_UPLOADED, true);
	}

	@Bean
	DocumentEventPublisher documentEventPublisher(RabbitTemplate rabbitTemplate) {
		return new DocumentEventPublisher(rabbitTemplate, objectMapper);
	}
}
