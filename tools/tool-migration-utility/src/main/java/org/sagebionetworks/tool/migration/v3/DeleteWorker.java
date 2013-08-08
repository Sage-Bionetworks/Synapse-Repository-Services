package org.sagebionetworks.tool.migration.v3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * Deletes all ids on the passed list.
 * 
 * @author John
 *
 */
public class DeleteWorker implements Callable<Long>, BatchWorker{
	
	MigrationType type;
	long count;
	Iterator<RowMetadata> iterator;
	BasicProgress progress;
	SynapseAdministrationInt destClient;
	long batchSize;
	
	

	public DeleteWorker(MigrationType type, long count,
			Iterator<RowMetadata> iterator, BasicProgress progress,
			SynapseAdministrationInt destClient, long batchSize) {
		super();
		this.type = type;
		this.count = count;
		this.iterator = iterator;
		this.progress = progress;
		this.destClient = destClient;
		this.batchSize = batchSize;
	}



	@Override
	public Long call() throws Exception {
		// Iterate and create batches.
		RowMetadata row = null;
		List<Long> batch = new LinkedList<Long>();
		long deletedCount = 0;
		long current = 0;
		while(iterator.hasNext()){
			row = iterator.next();
			current++;
			this.progress.setCurrent(current);
			if(row != null){
				batch.add(row.getId());
				if(batch.size() >= batchSize){
					Long c = this.migrateBatch(batch);
					deletedCount += c;
					batch.clear();
				}
			}
		}
		// If there is any data left in the batch send it
		if(batch.size() > 0){
			Long c = this.migrateBatch(batch);
			deletedCount += c;
			batch.clear();
		}
		progress.setDone();
		return deletedCount;
	}
	
	protected Long migrateBatch(List<Long> batch) throws Exception {
		Long c = BatchUtility.attemptBatchWithRetry(this, batch);
		return c;
	}
	
	public Long attemptBatch(List<Long> batch) throws JSONObjectAdapterException, SynapseException {
		IdList req = new IdList();
		req.setList(batch);
		MigrationTypeCount mtc = null;
		// Catch exception and re-throw as DaemonFailedException (technically, it's not)
		try {
			mtc = destClient.deleteMigratableObject(type, req);
		} catch (Exception e) {
			throw new DaemonFailedException(e);
		}
		
		return mtc.getCount();
	}
	
}
