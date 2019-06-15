package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;

public class ReplicationMessageManagerAsynchImpl implements ReplicationMessageManagerAsynch {
	
	@Autowired
	ExecutorService replicationMessageThreadPool;
	
	@Autowired
	ReplicationMessageManager replicationMessageManager;

	@Override
	public Future<Void> pushContainerIdsToReconciliationQueue(List<Long> scopeIds) {
		// Submit the call the thread pool
		return replicationMessageThreadPool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				replicationMessageManager.pushContainerIdsToReconciliationQueue(scopeIds);
				return null;
			}
		});
	}

}
