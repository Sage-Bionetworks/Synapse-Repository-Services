package org.sagebionetworks.worker;

import org.json.JSONObject;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Adapter for a {@link TypedMessageDrivenRunner} that is driven by a message converted to the a {@link JSONEntity} of type T
 * 
 * @param <T>
 */
public class JsonEntityDrivenRunnerAdapter<T extends JSONEntity> implements MessageDrivenRunner {
	
	private final TypedMessageDrivenRunner<T> runner;

	public JsonEntityDrivenRunnerAdapter(TypedMessageDrivenRunner<T> runner) {
		this.runner = runner;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		
		JSONObject jsonObject = MessageUtils.extractMessageBodyAsJSONObject(message);
		
		T convertedEntity = EntityFactory.createEntityFromJSONObject(jsonObject, runner.getObjectClass());
		
		runner.run(progressCallback, message, convertedEntity);
	}

}
