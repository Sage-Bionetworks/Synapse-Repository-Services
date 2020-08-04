package org.sagebionetworks.schema.worker;

import java.util.List;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.schema.EntitySchemaManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntitySchemaValidationWorker implements BatchChangeMessageDrivenRunner {

	EntitySchemaManager entitySchemaManager;
	WorkerLogger workerLogger;

	@Autowired
	public EntitySchemaValidationWorker(EntitySchemaManager entitySchemaManager, WorkerLogger workerLogger) {
		super();
		this.entitySchemaManager = entitySchemaManager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> messages)
			throws RecoverableMessageException, Exception {
		ValidateArgument.required(messages, "messages");
		messages.stream().filter(c -> ObjectType.ENTITY.equals(c.getObjectType())).forEach(c -> {
			try {
				entitySchemaManager.validateEntityAgainstBoundSchema(c.getObjectId());
			} catch (Throwable e) {
				boolean willRetry = false;
				workerLogger.logWorkerFailure(EntitySchemaValidationWorker.class, c, e, willRetry);
			}
		});

	}

}
