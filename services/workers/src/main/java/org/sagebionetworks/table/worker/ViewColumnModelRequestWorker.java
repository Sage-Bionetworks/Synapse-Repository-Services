package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class ViewColumnModelRequestWorker implements MessageDrivenRunner {
	
	private static final Logger LOG = LogManager.getLogger(ViewColumnModelRequestWorker.class);
	
	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	private TableIndexConnectionFactory connectionFactory;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		
		try {
			ViewColumnModelRequest request = AsynchJobUtils.extractRequestBody(status, ViewColumnModelRequest.class);
			
			String jobMessage = getStartingJobMessage(request);
			
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, jobMessage);
			
			ViewColumnModelResponse response = processRequest(request);
			
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		}  catch (Throwable e) {
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			LOG.error("ViewColumnModelRequest Job failed:", e);
		}
		
	}
	
	private ViewColumnModelResponse processRequest(ViewColumnModelRequest request) {
		
		ViewScope viewScope = request.getViewScope();
		String nextPageToken = request.getNextPageToken();

		TableIndexManager indexManager = connectionFactory.connectToFirstIndex();
		
		ColumnModelPage page = indexManager.getPossibleColumnModelsForScope(viewScope, nextPageToken);
		
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
