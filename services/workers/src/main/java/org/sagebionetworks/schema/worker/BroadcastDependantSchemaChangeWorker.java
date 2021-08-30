package org.sagebionetworks.schema.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class BroadcastDependantSchemaChangeWorker implements ChangeMessageDrivenRunner {
	
	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		jsonSchemaManager.sendValidationIndexNotificationsForDependants(message.getObjectId());
	}

}
