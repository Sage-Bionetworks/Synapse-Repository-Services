package org.sagebionetworks.table.worker;

import org.sagebionetworks.repo.manager.table.ReplicationToViewManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class ReplicatedToViewConsumerWorker implements ProgressingRunner {

	private final ReplicationToViewManager manager;

	public ReplicatedToViewConsumerWorker(ReplicationToViewManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		manager.consumeVisibleViewUpdates();
	}
	
}
