package org.sagebionetworks.table.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The worker listens for changes on entities and if they are table/views sync the schema of all the materialized views that depends on them
 */
public class MaterializedViewSourceUpdateWorker implements ChangeMessageDrivenRunner {
	
	private MaterializedViewManager manager;

	@Autowired
	public MaterializedViewSourceUpdateWorker(MaterializedViewManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message) throws RecoverableMessageException, Exception {
		
		if (ObjectType.ENTITY != message.getObjectType()) {
			throw new IllegalStateException("Expected ENTITY type, got: " + message.getObjectType());
		}
		
		IdAndVersion idAndVersion = IdAndVersion.newBuilder()
			.setId(KeyFactory.stringToKey(message.getObjectId()))
			.setVersion(message.getObjectVersion())
			.build();
			
		manager.refreshDependentMaterializedViews(idAndVersion);
	}

}
