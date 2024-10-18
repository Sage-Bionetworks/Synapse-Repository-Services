package org.sagebionetworks.limits.workers;

import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitManager;
import org.sagebionetworks.repo.model.limits.ProjectStorageDataEvent;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class ProjectStorageDataRefreshWorker implements TypedMessageDrivenRunner<ProjectStorageDataEvent> {
	
	private ProjectStorageLimitManager manager;
	
	public ProjectStorageDataRefreshWorker(ProjectStorageLimitManager manager) {
		this.manager = manager;
	}

	@Override
	public Class<ProjectStorageDataEvent> getObjectClass() {
		return ProjectStorageDataEvent.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, ProjectStorageDataEvent event)
			throws RecoverableMessageException, Exception {
		manager.refreshProjectStorageData(event.getProjectId());
	}
	
}
