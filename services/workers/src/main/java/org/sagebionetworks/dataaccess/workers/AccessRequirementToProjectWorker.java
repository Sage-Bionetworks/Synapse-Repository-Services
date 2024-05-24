package org.sagebionetworks.dataaccess.workers;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementToProjectWorker implements BatchChangeMessageDrivenRunner {

	private AccessRequirementManager manager;

	@Autowired
	public AccessRequirementToProjectWorker(AccessRequirementManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> messages)
			throws RecoverableMessageException, Exception {

		List<String> entitiesCreatedOrUpdated = messages.stream().filter(m -> ObjectType.ENTITY.equals(m.getObjectType()))
				.filter(m -> ChangeType.CREATE.equals(m.getChangeType()) || ChangeType.UPDATE.equals(m.getChangeType()))
				.map(m -> m.getObjectId()).collect(Collectors.toList());
		
		manager.mapAccessRequirementsToProject(entitiesCreatedOrUpdated);

	}

}
