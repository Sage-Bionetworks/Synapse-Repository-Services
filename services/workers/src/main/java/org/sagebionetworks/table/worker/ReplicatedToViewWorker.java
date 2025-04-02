package org.sagebionetworks.table.worker;

import org.sagebionetworks.repo.manager.table.ReplicationToViewManager;
import org.sagebionetworks.repo.model.table.ReplicatedEvent;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class ReplicatedToViewWorker implements TypedMessageDrivenRunner<ReplicatedEvent>  {
	
	private final ReplicationToViewManager manager;
	

	public ReplicatedToViewWorker(ReplicationToViewManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public Class<ReplicatedEvent> getObjectClass() {
		return ReplicatedEvent.class;
	}


	@Override
	public void run(ProgressCallback progressCallback, Message message, ReplicatedEvent event)
			throws RecoverableMessageException, Exception {
		manager.objectReplicated(event);
	}

}
