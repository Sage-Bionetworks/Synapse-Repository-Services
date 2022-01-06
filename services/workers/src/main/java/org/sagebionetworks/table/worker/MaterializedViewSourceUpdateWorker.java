package org.sagebionetworks.table.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker listens for table status changes, if available will trigger a re-build of all the materialized views that depend on the tables
 */
public class MaterializedViewSourceUpdateWorker implements MessageDrivenRunner {
	
	private MaterializedViewManager manager;

	@Autowired
	public MaterializedViewSourceUpdateWorker(MaterializedViewManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
					
		// manager.refreshDependentMaterializedViews(idAndVersion);
	}

}
