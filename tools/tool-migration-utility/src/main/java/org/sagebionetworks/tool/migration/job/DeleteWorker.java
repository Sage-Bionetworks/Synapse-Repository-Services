package org.sagebionetworks.tool.migration.job;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;

/**
 * A worker that will execute a single delete entity job.
 * 
 * @author John
 * 
 */
public class DeleteWorker implements Callable<WorkerResult> {

	static private Log log = LogFactory.getLog(DeleteWorker.class);
	
	ClientFactory clientFactory = null;
	Set<String> entites = null;

	/**
	 * Create a new delete worker
	 * @param clientFactory
	 * @param entites
	 */
	public DeleteWorker(ClientFactory clientFactory, Set<String> entites) {
		super();
		this.clientFactory = clientFactory;
		this.entites = entites;
	}


	@Override
	public WorkerResult call() throws Exception {
		try {
			// We only delete from the destination.
			Synapse client = clientFactory.createNewDestinationClient();
			for(String entityId: this.entites){
				try{
					Entity toDelete = client.getEntityById(entityId);
					client.deleteEntity(toDelete);
				}catch (SynapseNotFoundException e){
					// There is nothing to do if the entity does not exist
				}
			}			
			return new WorkerResult(this.entites.size(), WorkerResult.JobStatus.SUCCEDED);
		} catch (Exception e) {
			// Log any errors
			log.error("CreateUpdateWorker Failed to run job: "+ entites.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}

}
