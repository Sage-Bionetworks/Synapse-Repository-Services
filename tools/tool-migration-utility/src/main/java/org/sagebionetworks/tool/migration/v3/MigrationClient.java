package org.sagebionetworks.tool.migration.v3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataReader;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter;

/**
 * The V3 migration client.
 * @author jmhill
 *
 */
public class MigrationClient {
	
	static private Log log = LogFactory.getLog(MigrationClient.class);

	SynapseClientFactory factory;
	ExecutorService threadPool;
	/**
	 * New migration client.
	 * @param factory
	 */
	public MigrationClient(SynapseClientFactory factory) {
		if(factory == null) throw new IllegalArgumentException("Factory cannot be null");
		this.factory = factory;
		threadPool = Executors.newFixedThreadPool(1);
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
	
	/**
	 * Migrate all types
	 * @param batchSize
	 * @throws Exception
	 */
	public void migrateAllTypes(long batchSize, long timeout) throws Exception {
		setDestinationStatus(StatusEnum.READ_WRITE, "Synapse is ready for read/write");
		SynapseAdministrationInt source = factory.createNewSourceClient();
		SynapseAdministrationInt destination = factory.createNewDestinationClient();
		// Get the counts for all type from both the source and destination
		MigrationTypeCounts startSourceCounts = source.getTypeCounts();
		MigrationTypeCounts startDestCounts = destination.getTypeCounts();
		log.info("Source start counts:");
		printCount(startSourceCounts.getList());
		log.info("Destination start counts:");
		printCount(startDestCounts.getList());
		// Get the primary types
		List<MigrationType> primaryTypes = source.getPrimaryTypes().getList();
		
		// first we need to calculate the deltas
		List<DeltaData> deltaList = new LinkedList<DeltaData>();
		for(MigrationType type: primaryTypes){
			DeltaData dd = calculateDeltaForType(type, batchSize);
			deltaList.add(dd);
		}
		// Now we need to delete any data in reverse order
		for(int i=deltaList.size()-1; i >= 0; i--){
			DeltaData dd = deltaList.get(i);
			long count =  dd.getCounts().getDelete();
			if(count > 0){
				deleteFromDestination(dd.getType(), dd.getDeleteTemp(), count, batchSize);
			}
		}
		// Now do all adds in the original order
		for(int i=0; i<deltaList.size(); i++){
			DeltaData dd = deltaList.get(i);
			long count = dd.getCounts().getCreate();
			if(count > 0){
				createUpdateInDestination(dd.getType(), dd.getCreateTemp(), count, batchSize, timeout);
			}
		}
		// Now do all updates in the original order
		for(int i=0; i<deltaList.size(); i++){
			DeltaData dd = deltaList.get(i);
			long count = dd.getCounts().getUpdate();
			if(count > 0){
				createUpdateInDestination(dd.getType(), dd.getUpdateTemp(), count, batchSize, timeout);
			}
		}
		// Print the final counts
		MigrationTypeCounts endSourceCounts = source.getTypeCounts();
		MigrationTypeCounts endDestCounts = destination.getTypeCounts();
		log.info("Source end counts:");
		printCount(endSourceCounts.getList());
		log.info("Destination end counts:");
		printCount(endDestCounts.getList());
	}
	
	/**
	 * Create or update
	 * @param type
	 * @param createUpdateTemp
	 * @param create
	 * @param batchSize
	 * @throws Exception
	 */
	private void createUpdateInDestination(MigrationType type, File createUpdateTemp, long count, long batchSize, long timeout) throws Exception {
		BufferedRowMetadataReader reader = new BufferedRowMetadataReader(new FileReader(createUpdateTemp));
		try{
			BasicProgress progress = new BasicProgress();
			CreateUpdateWorker worker = new CreateUpdateWorker(type, count, reader,progress,factory.createNewDestinationClient(), factory.createNewSourceClient(), batchSize, timeout);
			Future<Long> future = this.threadPool.submit(worker);
			while(!future.isDone()){
				// Log the progress
				log.info("Creating/updating data for type: "+type.name()+" Progress: "+progress.getCurrentStatus());
				Thread.sleep(2000);
			}
			Long counts = future.get();
			log.info("Creating/updating the following counts for type: "+type.name()+" Counts: "+counts);
		}finally{
			reader.close();
		}
	}

	private void printCount(List<MigrationTypeCount> counts){
		for(MigrationTypeCount count: counts){
			log.info("\t"+count.getType().name()+" = "+count.getCount());
		}
	}
	
	/**
	 * Migrate one type.
	 * @param type
	 * @param progress
	 * @throws Exception 
	 */
	public DeltaData calculateDeltaForType(MigrationType type, long batchSize) throws Exception{
		// the first thing we need to do is calculate the what needs to be created, updated, or deleted.
		// We need three temp file to keep track of the deltas
		File createTemp = File.createTempFile("create", ".tmp");
		File updateTemp = File.createTempFile("update", ".tmp");
		File deleteTemp = File.createTempFile("delete", ".tmp");
		// Calculate the deltas
		DeltaCounts counts = calcualteDeltas(type, batchSize, createTemp, updateTemp, deleteTemp);
		return new DeltaData(type, createTemp, updateTemp, deleteTemp, counts);
		
	}

	/**
	 * Calcaulte the deltas
	 * @param type
	 * @param batchSize
	 * @param createTemp
	 * @param updateTemp
	 * @param deleteTemp
	 * @throws SynapseException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private DeltaCounts calcualteDeltas(MigrationType type, long batchSize, File createTemp, File updateTemp, File deleteTemp)	throws Exception {
		BasicProgress sourceProgress = new BasicProgress();
		BasicProgress destProgress = new BasicProgress();
		BufferedRowMetadataWriter createOut = null;
		BufferedRowMetadataWriter updateOut = null;
		BufferedRowMetadataWriter deleteOut = null;
		try{
			createOut = new BufferedRowMetadataWriter(new FileWriter(createTemp));
			updateOut = new BufferedRowMetadataWriter(new FileWriter(updateTemp));
			deleteOut = new BufferedRowMetadataWriter(new FileWriter(deleteTemp));
			MetadataIterator sourceIt = new MetadataIterator(type, factory.createNewSourceClient(), batchSize, sourceProgress);
			MetadataIterator destIt = new MetadataIterator(type, factory.createNewDestinationClient(), batchSize, destProgress);
			DeltaBuilder builder  = new DeltaBuilder(sourceIt, destIt, createOut, updateOut, deleteOut);
			// Do the work on a separate thread
			Future<DeltaCounts> future = this.threadPool.submit(builder);
			// Wait for the future to finish
			while(!future.isDone()){
				// Log the progress
				log.info("Calculating deltas for type: "+type.name()+" Progress: "+sourceProgress.getCurrentStatus());
				Thread.sleep(2000);
			}
			DeltaCounts counts = future.get();
			log.info("Calculated the following counts for type: "+type.name()+" Counts: "+counts);
			return counts;
		}finally{
			if(createOut != null){
				try {
					createOut.close();
				} catch (Exception e) {}
			}
			if(updateOut != null){
				try {
					updateOut.close();
				} catch (Exception e) {}
			}
			if(deleteOut != null){
				try {
					deleteOut.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Delete the requested object from the destination.
	 * @throws IOException 
	 * 
	 */
	private void deleteFromDestination(MigrationType type, File deleteTemp, long count, long batchSize) throws Exception{
		BufferedRowMetadataReader reader = new BufferedRowMetadataReader(new FileReader(deleteTemp));
		try{
			BasicProgress progress = new BasicProgress();
			DeleteWorker worker = new DeleteWorker(type, count, reader, progress, factory.createNewDestinationClient(), batchSize);
			Future<Long> future = this.threadPool.submit(worker);
			while(!future.isDone()){
				// Log the progress
				log.info("Deleting data for type: "+type.name()+" Progress: "+progress.getCurrentStatus());
				Thread.sleep(2000);
			}
			Long counts = future.get();
			log.info("Deleted the following counts for type: "+type.name()+" Counts: "+counts);
		}finally{
			reader.close();
		}

	}
}
