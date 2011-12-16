package org.sagebionetworks.tool.migration.job;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

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
	BasicProgress progress = null;

	/**
	 * Create a new delete worker
	 * @param clientFactory
	 * @param entites
	 */
	public DeleteWorker(ClientFactory clientFactory, Set<String> entites, BasicProgress progress) {
		super();
		this.clientFactory = clientFactory;
		this.entites = entites;
		this.progress = progress;
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
				}catch(SynapseServiceException e){
					if(e.getCause() instanceof SocketTimeoutException){
						// Deletes can take a long to complete so we just continue when it happens
						Thread.sleep(2000);
					}else{
						throw e;
					}
				}
				progress.setCurrent(progress.getCurrent()+1);
				Thread.sleep(1000);
			}
			// done
			progress.setCurrent(progress.getTotal());
			return new WorkerResult(this.entites.size(), WorkerResult.JobStatus.SUCCEDED);
		} catch (Exception e) {
			// done
			progress.setCurrent(progress.getTotal());
			// Log any errors
			log.error("CreateUpdateWorker Failed to run job: "+ entites.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}

}
