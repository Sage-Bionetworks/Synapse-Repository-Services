package org.sagebionetworks.schema.worker;

import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetValidationSchemaWorker implements AsyncJobRunner<GetValidationSchemaRequest, GetValidationSchemaResponse> {

	@Autowired
	private JsonSchemaManager schemaManager;

	@Override
	public Class<GetValidationSchemaRequest> getRequestType() {
		return GetValidationSchemaRequest.class;
	}

	@Override
	public Class<GetValidationSchemaResponse> getResponseType() {
		return GetValidationSchemaResponse.class;
	}

	@Override
	public GetValidationSchemaResponse run(String jobId, UserInfo user, GetValidationSchemaRequest request,
			AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		jobProgressCallback.updateProgress("Starting job...", 0L, 100L);
		
		JsonSchema validationSchema = schemaManager.getValidationSchema(request.get$id());
		
		GetValidationSchemaResponse response = new GetValidationSchemaResponse()
			.setValidationSchema(validationSchema);
		
		return response;
	}

}
