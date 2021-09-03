package org.sagebionetworks.schema.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidationSchemaIndexChangeWorker implements ChangeMessageDrivenRunner {

	@Autowired
	JsonSchemaManager jsonSchemaManager;
	
	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		jsonSchemaManager.createOrUpdateValidationSchemaIndex(message.getObjectId());
	}
}
