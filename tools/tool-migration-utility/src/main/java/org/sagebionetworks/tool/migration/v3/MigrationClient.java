package org.sagebionetworks.tool.migration.v3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * The V3 migration client.
 * @author jmhill
 *
 */
public class MigrationClient {
	
	static private Log log = LogFactory.getLog(MigrationClient.class);

	SynapseClientFactory factory;
	/**
	 * New migration client.
	 * @param factory
	 */
	public MigrationClient(SynapseClientFactory factory) {
		if(factory == null) throw new IllegalArgumentException("Factory cannot be null");
		this.factory = factory;
	}
	
	/**
	 * Migrate all data from the source repository to the destination.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public void migrateFromSourceToDestination() throws Exception {
		migrate(false);
	}
	
	/**
	 * Re-synchronize all data from source to destination. 
	 * @throws JSONObjectAdapterException 
	 * @throws SynapseException 
	 */
	public void resynchFromSourceToDestination() throws Exception {
		migrate(true);
	}

	/**
	 * Migrate all data from the source to destination.
	 * 
	 * @param finalSynchronize - If finalSynchronize is set to true then source repository will be placed in read-only mode during the migration and left in read-only
	 * after migration finishes successfully (failures will result in the source returning to read-write).
	 * If finalSynchronize is set to false, the source repository will remain in READ_WRITE mode during the migration process.
	 * @throws Exception 
	 */
	public void migrate(boolean finalSynchronize) throws Exception {
		// First set the destination stack status to down
		setDestinationStatus(StatusEnum.DOWN, "Staging is down for data migration");
		if(finalSynchronize){
			// This is the final synchronize so place the source into read-only mode.
			setSourceStatus(StatusEnum.READ_ONLY, "Synapse is in read-only mode for maintenance");
		}
		try{
			

			// After migration is complete, re-enable staging
			setDestinationStatus(StatusEnum.READ_WRITE, "Synapse is ready for read/write");
		}catch (Exception e){
			// If an error occurs the source server must be returned to read-write
			if(finalSynchronize){
				// This is the final synchronize so place the source into read-only mode.
				log.error("Migration failed on a final synchronize, so the source stack will be set back to read/write");
				setSourceStatus(StatusEnum.READ_WRITE, "Synapse returned to read/write.");
			}
			log.error("Migration failed", e);
			throw e;
		}
	}
	
	/**
	 * 
	 * @param status
	 * @param message
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void setDestinationStatus(StatusEnum status, String message) throws SynapseException, JSONObjectAdapterException {
		setStatus(this.factory.createNewDestinationClient(), status, message);
	}
	
	/**
	 * 
	 * @param status
	 * @param message
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void setSourceStatus(StatusEnum status, String message) throws SynapseException, JSONObjectAdapterException {
		setStatus(this.factory.createNewSourceClient(), status, message);
	}
	
	/**
	 * 
	 * @param client
	 * @param status
	 * @param message
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	private static void setStatus(SynapseAdministrationInt client, StatusEnum status, String message) throws JSONObjectAdapterException, SynapseException{
		StackStatus destStatus = client.getCurrentStackStatus();
		destStatus.setStatus(status);
		destStatus.setCurrentMessage(message);
		destStatus = client.updateCurrentStackStatus(destStatus);
	}
}
