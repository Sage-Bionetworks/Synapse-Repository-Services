package org.sagebionetworks.schema.worker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.schema.EntitySchemaValidator;
import org.sagebionetworks.repo.manager.schema.ObjectSchemaValidator;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class SchemaValidationWorker implements BatchChangeMessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(SchemaValidationWorker.class);
	Map<ObjectType, ObjectSchemaValidator> validators;
	WorkerLogger workerLogger;

	@Autowired
	public SchemaValidationWorker(WorkerLogger workerLogger, EntitySchemaValidator entitySchemaManager) {
		super();
		this.workerLogger = workerLogger;
		this.validators = new LinkedHashMap<ObjectType, ObjectSchemaValidator>(1);
		this.validators.put(ObjectType.ENTITY, entitySchemaManager);
	}

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> messages)
			throws RecoverableMessageException, Exception {
		ValidateArgument.required(messages, "messages");
		messages.stream().filter(c -> validators.containsKey(c.getObjectType())).forEach(c -> {
			try {
				validators.get(c.getObjectType()).validateObject(c.getObjectId());
			} catch (Throwable e) {
				LOG.error("Failed:",e);
				boolean willRetry = false;
				workerLogger.logWorkerFailure(SchemaValidationWorker.class, c, e, willRetry);
			}
		});
	}
	
}
