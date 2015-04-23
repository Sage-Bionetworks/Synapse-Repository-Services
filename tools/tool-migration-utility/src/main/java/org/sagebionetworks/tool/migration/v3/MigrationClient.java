package org.sagebionetworks.tool.migration.v3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataReader;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter;
import org.sagebionetworks.tool.progress.BasicProgress;

/**
 * The V3 migration client.
 * @author jmhill
 *
 */
public class MigrationClient {
	
	static private Log log = LogFactory.getLog(MigrationClient.class);

	SynapseClientFactory factory;
	ExecutorService threadPool;
	List<Exception> deferredExceptions;
	final int MAX_DEFERRED_EXCEPTIONS = 10;
	
	/**
	 * New migration client.
	 * @param factory
	 */
	public MigrationClient(SynapseClientFactory factory) {
		if(factory == null) throw new IllegalArgumentException("Factory cannot be null");
		this.factory = factory;
		threadPool = Executors.newFixedThreadPool(1);
		deferredExceptions = new ArrayList<Exception>();
	}

	/**
	 * Migrate all data from the source to destination.
	 * 
	 * @param finalSynchronize - If finalSynchronize is set to true then source repository will be placed in read-only mode during the migration and left in read-only
	 * after migration finishes successfully (failures will result in the source returning to read-write).
	 * If finalSynchronize is set to false, the source repository will remain in READ_WRITE mode during the migration process.
	 * @throws Exception 
	 */
	public void migrate(boolean finalSynchronize, long batchSize, long timeoutMS, int retryDenominator, boolean deferExceptions) throws Exception {
		// First set the destination stack status to down
		setDestinationStatus(StatusEnum.DOWN, "Staging is down for data migration");
		if(finalSynchronize){
			// This is the final synchronize so place the source into read-only mode.
			setSourceStatus(StatusEnum.READ_ONLY, "Synapse is in read-only mode for maintenance");
		}
		try{
			this.migrateAllTypes(batchSize, timeoutMS, retryDenominator, deferExceptions);
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
	private static void setStatus(SynapseAdminClient client, StatusEnum status, String message) throws JSONObjectAdapterException, SynapseException{
		StackStatus destStatus = client.getCurrentStackStatus();
		destStatus.setStatus(status);
		destStatus.setCurrentMessage(message);
		destStatus = client.updateCurrentStackStatus(destStatus);
	}
	
	/**
	 * Migrate all types.
	 * @param batchSize - Max batch size
	 * @param timeoutMS - max time to wait for a deamon job.
	 * @param retryDenominator - how to divide a batch into sub-batches when errors occur.
	 * @throws Exception
	 */
	public void migrateAllTypes(long batchSize, long timeoutMS, int retryDenominator, boolean deferExceptions) throws Exception {
		SynapseAdminClient source = factory.createNewSourceClient();
		SynapseAdminClient destination = factory.createNewDestinationClient();
		// Get the counts for all type from both the source and destination
		MigrationTypeCounts startSourceCounts = source.getTypeCounts();
		MigrationTypeCounts startDestCounts = destination.getTypeCounts();
		log.info("Start counts:");
		printCounts(startSourceCounts.getList(), startDestCounts.getList());
		// Get the primary types for src and dest
		List<MigrationType> srcPrimaryTypes = source.getPrimaryTypes().getList();
		List<MigrationType> destPrimaryTypes = destination.getPrimaryTypes().getList();
		// Only migrate the src primary types that are at destination
		List<MigrationType> primaryTypesToMigrate = new LinkedList<MigrationType>();
		for (MigrationType pt: destPrimaryTypes) {
			if (srcPrimaryTypes.contains(pt)) {
				primaryTypesToMigrate.add(pt);
			}
		}
		// Do the actual migration.
		migrateAll(batchSize, timeoutMS, retryDenominator, primaryTypesToMigrate, deferExceptions);
		// Print the final counts
		MigrationTypeCounts endSourceCounts = source.getTypeCounts();
		MigrationTypeCounts endDestCounts = destination.getTypeCounts();
		log.info("End counts:");
		printCounts(endSourceCounts.getList(), endDestCounts.getList());
		
		if ((deferExceptions) && (this.deferredExceptions.size() > 0)) {
			log.error("Encountered " + this.deferredExceptions.size() + " execution exceptions in the worker threads");
			this.dumpDeferredExceptions();
			throw this.deferredExceptions.get(deferredExceptions.size()-1);
		}
	}

	/**
	 * Does the actaul migration work.
	 * @param batchSize
	 * @param timeoutMS
	 * @param retryDenominator
	 * @param primaryTypes
	 * @throws Exception
	 */
	private void migrateAll(long batchSize, long timeoutMS,	int retryDenominator, List<MigrationType> primaryTypes, boolean deferExceptions)
			throws Exception {
		List<DeltaData> deltaList = new LinkedList<DeltaData>();
		for(MigrationType type: primaryTypes){
			DeltaData dd = calculateDeltaForType(type, batchSize);
			deltaList.add(dd);
		}
		
		// First attempt to delete, catching any exception (for case like fileHandles)
		Exception firstDeleteException = null;
		try {
			// Delete any data in reverse order
			for(int i=deltaList.size()-1; i >= 0; i--){
				DeltaData dd = deltaList.get(i);
				long count =  dd.getCounts().getDelete();
				if(count > 0){
					deleteFromDestination(dd.getType(), dd.getDeleteTemp(), count, batchSize, deferExceptions);
				}
			}
		} catch (Exception e) {
			firstDeleteException = e;
			log.info("Exception thrown during first delete phase.", e);
		}
		
		// If exception in insert/update phase, then rethrow at end so main is aware of problem
		Exception insException = null;
		try {
			// Now do all adds in the original order
			for(int i=0; i<deltaList.size(); i++){
				DeltaData dd = deltaList.get(i);
				long count = dd.getCounts().getCreate();
				if(count > 0){
					createUpdateInDestination(dd.getType(), dd.getCreateTemp(), count, batchSize, timeoutMS, retryDenominator, deferExceptions);
				}
			}
		} catch (Exception e) {
			insException = e;
			log.info("Exception thrown during insert phase", e);
		}
		Exception updException = null;
		try {
			// Now do all updates in the original order
			for(int i=0; i<deltaList.size(); i++){
				DeltaData dd = deltaList.get(i);
				long count = dd.getCounts().getUpdate();
				if(count > 0){
					createUpdateInDestination(dd.getType(), dd.getUpdateTemp(), count, batchSize, timeoutMS, retryDenominator, deferExceptions);
				}
			}
		} catch (Exception e) {
			updException = e;
			log.info("Exception thrown during update phases", e);
		}

		// Only do the post-deletes if the initial ones raised an exception
		if (firstDeleteException != null) {
			// Now we need to delete any data in reverse order
			for(int i=deltaList.size()-1; i >= 0; i--){
				DeltaData dd = deltaList.get(i);
				long count =  dd.getCounts().getDelete();
				if(count > 0){
					deleteFromDestination(dd.getType(), dd.getDeleteTemp(), count, batchSize, deferExceptions);
				}
			}
		}
		
		if (insException != null) {
			throw insException;
		}
		if (updException != null) {
			throw updException;
		}
	}
	
	/**
	 * Create or update
	 * @param type
	 * @param createUpdateTemp
	 * @param create
	 * @param batchSize
	 * @throws Exception
	 */
	private void createUpdateInDestination(MigrationType type, File createUpdateTemp, long count, long batchSize, long timeout, int retryDenominator, boolean deferExceptions) throws Exception {
		BufferedRowMetadataReader reader = new BufferedRowMetadataReader(new FileReader(createUpdateTemp));
		try{
			BasicProgress progress = new BasicProgress();
			CreateUpdateWorker worker = new CreateUpdateWorker(type, count, reader,progress,factory.createNewDestinationClient(), factory.createNewSourceClient(), batchSize, timeout, retryDenominator);
			Future<Long> future = this.threadPool.submit(worker);
			while(!future.isDone()){
				// Log the progress
				String message = progress.getMessage();
				if(message == null){
					message = "";
				}
				log.info("Creating/updating data for type: "+type.name()+" Progress: "+progress.getCurrentStatus()+" "+message);
				Thread.sleep(2000);
			}
			try {
				Long counts = future.get();
				log.info("Creating/updating the following counts for type: "+type.name()+" Counts: "+counts);
			} catch (ExecutionException e) {
				if (deferExceptions) {
					deferException(e);
				} else {
					throw(e);
				}
			}
		}finally{
			reader.close();
		}
	}

	private void printCounts(List<MigrationTypeCount> srcCounts, List<MigrationTypeCount> destCounts) {
		Map<MigrationType, Long> mapSrcCounts = new HashMap<MigrationType, Long>();
		for (MigrationTypeCount sMtc: srcCounts) {
			mapSrcCounts.put(sMtc.getType(), sMtc.getCount());
		}
		// All migration types of source should be at destination
		// Note: unused src migration types are covered, they're not in destination results
		for (MigrationTypeCount mtc: destCounts) {
			log.info("\t" + mtc.getType().name() + ":\t" + (mapSrcCounts.containsKey(mtc.getType()) ? mapSrcCounts.get(mtc.getType()).toString() : "NA") + "\t" + mtc.getCount());
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
	private void deleteFromDestination(MigrationType type, File deleteTemp, long count, long batchSize, boolean deferExceptions) throws Exception{
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
			try {
				Long counts = future.get();
				log.info("Deleted the following counts for type: "+type.name()+" Counts: "+counts);
			} catch (ExecutionException e) {
				if (deferExceptions) {
					deferException(e);
				} else {
					throw(e);
				}
			}
		}finally{
			reader.close();
		}

	}

	private void deferException(ExecutionException e) throws ExecutionException {
		if (deferredExceptions.size() <= this.MAX_DEFERRED_EXCEPTIONS) {
			log.debug("Deferring execution exception in MigrationClient.createUpdateInDestination()");
			deferredExceptions.add(e);
		} else {
			log.debug("Encountered more than " + this.MAX_DEFERRED_EXCEPTIONS + " execution exceptions in the worker threads. Dumping deferred first");
			this.dumpDeferredExceptions();
			throw e;
		}
	}
	
	private void dumpDeferredExceptions() {
		int i = 0;
		for (Exception e: this.deferredExceptions) {
			log.error("Deferred exception " + i++, e);
		}
	}
}
