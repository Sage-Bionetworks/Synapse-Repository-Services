package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adapter for a {@link TypedMessageDrivenRunner} that is driven by a message converted to the type T
 * 
 * @param <T>
 */
public class TypedMessageDrivenRunnerAdapter<T> implements MessageDrivenRunner {
	
	private static final String TOPIC_ARN_FIELD = "TopicArn";
	private static final String MESSAGE_FIELD = "Message";
	
	private TypedMessageDrivenRunner<T> runner;
	private ObjectMapper objectMapper;
	
	public TypedMessageDrivenRunnerAdapter(TypedMessageDrivenRunner<T> runner) {
		this.runner = runner;
	}
	
	@Autowired
	public void configure(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	@Override
	public void run(ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException, Exception {
			
			final T convertedMessage = convertMessage(message);
			
			runner.run(progressCallback, convertedMessage);
	}
	
	private T convertMessage(Message message) throws JsonProcessingException {
		final String messageBody = extractMessageBody(message);
		return objectMapper.readValue(messageBody, runner.getObjectClass());
	}
	
	private String extractMessageBody(Message message) throws JsonProcessingException {
		final String messageBody = message.getBody();
		final JsonNode root = objectMapper.readTree(messageBody);

		// If a message comes from a topic, the body of the message is in the "Message" property as a plain string
		if (root.has(TOPIC_ARN_FIELD) && root.has(MESSAGE_FIELD)) {
			return root.get(MESSAGE_FIELD).textValue();
		}
		
		return messageBody;
	}

}
