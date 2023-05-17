package org.sagebionetworks.schema.worker;

import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreateJsonSchemaWorker implements AsyncJobRunner<CreateSchemaRequest, CreateSchemaResponse> {

	private JsonSchemaManager schemaManager;
	
	@Autowired
	public CreateJsonSchemaWorker(JsonSchemaManager schemaManager) {
		this.schemaManager = schemaManager;
	}
	
	@Override
	public Class<CreateSchemaRequest> getRequestType() {
		return CreateSchemaRequest.class;
	}
	
	@Override
	public Class<CreateSchemaResponse> getResponseType() {
		return CreateSchemaResponse.class;
	}
	
	@Override
	public CreateSchemaResponse run(String jobId, UserInfo user, CreateSchemaRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		jobProgressCallback.updateProgress("Starting job...", 0L, 100L);
		return schemaManager.createJsonSchema(user, request);
	}

}
