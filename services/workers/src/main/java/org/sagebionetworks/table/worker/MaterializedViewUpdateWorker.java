package org.sagebionetworks.table.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * This worker will build/rebuild the index for materialized views.
 *
 */
public class MaterializedViewUpdateWorker implements ChangeMessageDrivenRunner {

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {

		// @TODO add real code.
		System.out.println(message.toString());
		
	}

}
