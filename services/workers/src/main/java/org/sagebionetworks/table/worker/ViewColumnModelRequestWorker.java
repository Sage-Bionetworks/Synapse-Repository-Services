package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class ViewColumnModelRequestWorker implements AsyncJobRunner<ViewColumnModelRequest, ViewColumnModelResponse> {
	
	private static final Logger LOG = LogManager.getLogger(ViewColumnModelRequestWorker.class);
	
	private TableIndexConnectionFactory connectionFactory;
	
	@Autowired
	public ViewColumnModelRequestWorker(TableIndexConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}
	
	@Override
	public Class<ViewColumnModelRequest> getRequestType() {
		return  ViewColumnModelRequest.class;
	}
	
	@Override
	public Class<ViewColumnModelResponse> getResponseType() {
		return ViewColumnModelResponse.class;
	}

	@Override
	public ViewColumnModelResponse run(String jobId, UserInfo user, ViewColumnModelRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		try {
			String jobMessage = getStartingJobMessage(request);
			
			jobProgressCallback.updateProgress(jobMessage, 0L, 100L);
			
			return processRequest(request);
			
		}  catch (Throwable e) {
			LOG.error("ViewColumnModelRequest Job failed:", e);
			throw e;
		}
	}
	
	private ViewColumnModelResponse processRequest(ViewColumnModelRequest request) {
		
		ViewScope viewScope = request.getViewScope();
		String nextPageToken = request.getNextPageToken();
		// By default we exclude derived keys
		boolean excludeDerivedKeys = request.getIncludeDerivedAnnotations() == null ? true : !request.getIncludeDerivedAnnotations();

		TableIndexManager indexManager = connectionFactory.connectToFirstIndex();
		
		ColumnModelPage page = indexManager.getPossibleColumnModelsForScope(viewScope, nextPageToken, excludeDerivedKeys);
		
		ViewColumnModelResponse response = new ViewColumnModelResponse();
		
		response.setResults(page.getResults());
		response.setNextPageToken(page.getNextPageToken());
		
		return response;
	}
	
	private String getStartingJobMessage(ViewColumnModelRequest request) {
		ValidateArgument.required(request.getViewScope(), "The view scope");
		
		ViewScope viewScope = request.getViewScope();

		return String.format("Processing ViewColumnModelRequest job (EntiyViewType: %s, Scope Size: %s)...", viewScope.getViewEntityType(), viewScope.getScope() == null ? 0 : viewScope.getScope().size());
		
	}

}
