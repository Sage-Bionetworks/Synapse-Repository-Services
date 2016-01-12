package org.sagebionetworks.tool.migration.v4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v3.CreateUpdateWorker;
import org.sagebionetworks.tool.migration.v3.DeleteWorker;
import org.sagebionetworks.tool.migration.v3.SynapseClientFactory;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataReader;
import org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter;
import org.sagebionetworks.tool.migration.v4.delta.DeltaFinder;
import org.sagebionetworks.tool.migration.v4.delta.DeltaRanges;
import org.sagebionetworks.tool.migration.v4.delta.IdRange;
import org.sagebionetworks.tool.migration.v4.utils.ToolMigrationUtils;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;
import org.sagebionetworks.tool.progress.BasicProgress;

/**
 * The V4 migration client.
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
	 * @throws Exception 
	 */
	public boolean migrate(int maxRetries, long batchSize, long timeoutMS, int retryDenominator, boolean deferExceptions) throws Exception {
		boolean failed = false;
		try{
			// First set the destination stack status to down
			setDestinationStatus(StatusEnum.READ_ONLY, "Staging is down for data migration");
			for (int i = 0; i < maxRetries; i++) {
				try {
					this.migrateAllTypes(batchSize, timeoutMS, retryDenominator, deferExceptions);
				} catch (Throwable e) {
					failed = true;
					log.error("Failed at attempt: " + i + " with error " + e.getMessage(), e);
				}
				if (! failed) {
					break;
				}
			}
		} catch (Exception e) {
			log.error("Migration failed", e);
		} finally {
			// After migration is complete, re-enable read/write
			setDestinationStatus(StatusEnum.READ_WRITE, "Synapse is ready for read/write");
		}
		return failed;
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
		List<MigrationTypeCount> startSourceCounts = ToolMigrationUtils.getTypeCounts(source);
		List<MigrationTypeCount> startDestCounts = ToolMigrationUtils.getTypeCounts(destination);
		log.info("Start counts:");
		printCounts(startSourceCounts, startDestCounts);
		
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
		
		// Get the metadata for the (primary) types to migrate
		List<TypeToMigrateMetadata> typesToMigrateMetadata = ToolMigrationUtils.buildTypeToMigrateMetadata(startSourceCounts, startDestCounts, primaryTypesToMigrate);
		
		// Do the actual migration.
		migrateAll(batchSize, timeoutMS, retryDenominator, typesToMigrateMetadata, deferExceptions);
		
		// Print the final counts
		List<MigrationTypeCount> endSourceCounts = ToolMigrationUtils.getTypeCounts(source);
		List<MigrationTypeCount> endDestCounts = ToolMigrationUtils.getTypeCounts(destination);
		log.info("End counts:");
		printCounts(endSourceCounts, endDestCounts);
		
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
	private void migrateAll(long batchSize, long timeoutMS,	int retryDenominator, List<TypeToMigrateMetadata> primaryTypes, boolean deferExceptions)
			throws Exception {

		// Each migration uses a different salt (same for each type)
		String salt = UUID.randomUUID().toString();
		
		List<DeltaData> deltaList = new LinkedList<DeltaData>();
		for (TypeToMigrateMetadata tm: primaryTypes) {
			DeltaData dd = calculateDeltaForType(tm, salt, batchSize);
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
	public DeltaData calculateDeltaForType(TypeToMigrateMetadata tm, String salt, long batchSize) throws Exception{

		// First, we find the delta ranges
		DeltaFinder finder = new DeltaFinder(tm, factory.createNewSourceClient(), factory.createNewDestinationClient(), salt, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		
		// the first thing we need to do is calculate the what needs to be created, updated, or deleted.
		// We need three temp file to keep track of the deltas
		File createTemp = File.createTempFile("create", ".tmp");
		File updateTemp = File.createTempFile("update", ".tmp");
		File deleteTemp = File.createTempFile("delete", ".tmp");
		
		// Calculate the deltas
		DeltaCounts counts = calculateDeltas(tm, ranges, batchSize, createTemp, updateTemp, deleteTemp);
		return new DeltaData(tm.getType(), createTemp, updateTemp, deleteTemp, counts);
		
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
	private DeltaCounts calculateDeltas(TypeToMigrateMetadata typeMeta, DeltaRanges ranges, long batchSize, File createTemp, File updateTemp, File deleteTemp)	throws Exception {

		BasicProgress sourceProgress = new BasicProgress();
		BasicProgress destProgress = new BasicProgress();
		BufferedRowMetadataWriter createOut = null;
		BufferedRowMetadataWriter updateOut = null;
		BufferedRowMetadataWriter deleteOut = null;
		try{
			createOut = new BufferedRowMetadataWriter(new FileWriter(createTemp));
			updateOut = new BufferedRowMetadataWriter(new FileWriter(updateTemp));
			deleteOut = new BufferedRowMetadataWriter(new FileWriter(deleteTemp));
			
			// Unconditional inserts
			long insCount = 0;
			List<IdRange> insRanges = ranges.getInsRanges();
			for (IdRange r: insRanges) {
				RangeMetadataIterator it = new RangeMetadataIterator(typeMeta.getType(), factory.createNewSourceClient(), batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
				RowMetadata sourceRow = null;
				do {
					sourceRow = it.next();
					if (sourceRow != null) {
						createOut.write(sourceRow);
						insCount++;
					}
				} while (sourceRow != null);
			}
			// Unconditional deletes
			long delCount = 0;
			List<IdRange> delRanges = ranges.getDelRanges();
			for (IdRange r: delRanges) {
				RangeMetadataIterator it = new RangeMetadataIterator(typeMeta.getType(), factory.createNewDestinationClient(), batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
				RowMetadata destRow = null;
				do {
					destRow = it.next();
					if (destRow != null) {
						deleteOut.write(destRow);
						delCount++;
					}
				} while (destRow != null);
			}
			
			// Updates
			DeltaCounts counts = null;
			List<IdRange> updRanges = ranges.getUpdRanges();
			for (IdRange r: updRanges) {

				RangeMetadataIterator sourceIt = new RangeMetadataIterator(typeMeta.getType(), factory.createNewSourceClient(), batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
				RangeMetadataIterator destIt = new RangeMetadataIterator(typeMeta.getType(), factory.createNewDestinationClient(), batchSize, r.getMinId(), r.getMaxId(), destProgress);
				DeltaBuilder builder  = new DeltaBuilder(sourceIt, destIt, createOut, updateOut, deleteOut);

				// Do the work on a separate thread
				Future<DeltaCounts> future = this.threadPool.submit(builder);
				// Wait for the future to finish
				while(!future.isDone()){
					// Log the progress
					log.info("Calculating deltas for type: "+typeMeta.getType().name()+" Progress: "+sourceProgress.getCurrentStatus());
					Thread.sleep(2000);
				}
				counts = future.get();
			}
			
			// Fix the counts
			counts.setCreate(insCount+counts.getCreate());
			counts.setDelete(delCount+counts.getDelete());
			
			log.info("Calculated the following counts for type: "+typeMeta.getType().name()+" Counts: "+counts);
			
			return counts;
			
		} finally {
			
			if (createOut != null)  {
				try {
					createOut.close();
				} catch (Exception e) {}
			}
			if (updateOut != null) {
				try {
					updateOut.close();
				} catch (Exception e) {}
			}
			if (deleteOut != null) {
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

