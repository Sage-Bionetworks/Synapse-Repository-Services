package org.sagebionetworks.tool.migration.v3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * Deletes all ids on the passed list.
 * 
 * @author John
 *
 */
public class DeleteWorker implements Callable<Long>{
	
	MigrationType type;
	long count;
	Iterator<Long> iterator;
	BasicProgress progress;
	SynapseAdministrationInt destClient;
	long batchSize;
	
	

	public DeleteWorker(MigrationType type, long count,
			Iterator<Long> iterator, BasicProgress progress,
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
		Long id = null;
		List<String> batch = new LinkedList<String>();
		long deletedCount = 0;
		long current = 0;
		while(iterator.hasNext()){
			id = iterator.next();
			current++;
			this.progress.setCurrent(current);
			if(id != null){
				batch.add(""+id);
				if(batch.size() >= batchSize){
					IdList request = new IdList();
					request.setList(batch);
					MigrationTypeCount  mtc = destClient.deleteMigratableObject(type, request);
					deletedCount += mtc.getCount();
					batch.clear();
				}
			}
		}
		// If there is any data left in the batch send it
		if(batch.size() > 0){
			IdList request = new IdList();
			request.setList(batch);
			MigrationTypeCount  mtc = destClient.deleteMigratableObject(type, request);
			deletedCount += mtc.getCount();
			batch.clear();
		}
		progress.setDone();
		return deletedCount;
	}
	
	

}
