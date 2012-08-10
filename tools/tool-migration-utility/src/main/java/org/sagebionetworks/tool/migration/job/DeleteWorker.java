package org.sagebionetworks.tool.migration.job;

import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * A worker that will execute a single delete entity job.
 * 
 * @author John
 * 
 */
public class DeleteWorker implements Callable<WorkerResult> {

	static private Log log = LogFactory.getLog(DeleteWorker.class);
	
	Configuration configuration = null;
	ClientFactory clientFactory = null;
	Set<String> entites = null;
	MigratableObjectType objectType = null;
	BasicProgress progress = null;
	
	/**
	 * Create a new delete worker
	 * @param clientFactory
	 * @param entites
	 */
	public DeleteWorker(Configuration configuration, ClientFactory clientFactory, Set<String> entites, MigratableObjectType objectType, BasicProgress progress) {
		super();
		this.configuration = configuration;
		this.clientFactory = clientFactory;
		this.entites = entites;
		this.objectType= objectType;
		this.progress = progress;
	}


	@Override
	public WorkerResult call() throws Exception {
		try {
			// We only delete from the destination.
			SynapseAdministration client = clientFactory.createNewDestinationClient(configuration);
			for(String entityId: this.entites){
				try{
					MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
					mod.setId(entityId);
					mod.setType(objectType);
					client.deleteObject(mod);
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
			progress.setDone();
			return new WorkerResult(this.entites.size(), WorkerResult.JobStatus.SUCCEEDED);
		} catch (Exception e) {
			// done
			progress.setDone();
			// Log any errors
			log.error("CreateUpdateWorker Failed to run job: "+ entites.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}

}
