package org.sagebionetworks.tool.migration.job;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job.Type;

/**
 * This builders only job is to create CreationJobs.
 * 
 * @author John
 *
 */
public class DeleteJobBuilder implements Callable<BuilderResponse> {
	
	Map<String, EntityData> sourceMap;
	List<EntityData> destList;
	private Queue<Job> queue;
	private int batchSize;


	/**
	 * 
	 * @param sourceMap
	 * @param destList
	 * @param queue
	 * @param batchSize
	 */
	public DeleteJobBuilder(Map<String, EntityData> sourceMap,
			List<EntityData> destList, Queue<Job> queue, int batchSize) {
		super();
		this.sourceMap = sourceMap;
		this.destList = destList;
		this.queue = queue;
		this.batchSize = batchSize;
	}


	@Override
	public BuilderResponse call() throws Exception {
		// Get the two clients		
		int deleteSubmitted = 0;
		Set<String> deletedToDate = new HashSet<String>();
		Set<String> batchToDelete = new HashSet<String>();
		// Walk over the source list.
		for(EntityData dest: destList){
			// Find any entity in the destination that does not exist in the source.
			if(!sourceMap.containsKey(dest.getEntityId())){
				// This entity should be deleted, but if we are already
				// deleting its parent then there is no need to delete it.
				if(!deletedToDate.contains(dest.getParentId())){
					// The entity exists in the destination but not the source.
					// We are not already deleting its parent.
					batchToDelete.add(dest.getEntityId());
					deleteSubmitted++;
				}
				// This entity will still be deleted
				deletedToDate.add(dest.getEntityId());
			}
			if(batchToDelete.size() >= this.batchSize){
				Job createJob = new Job(batchToDelete, Type.DELETE);
				this.queue.add(createJob);
				batchToDelete = new HashSet<String>();
			}
		}
		// Submit any creates left over
		if(!batchToDelete.isEmpty()){
			Job updateJob = new Job(batchToDelete, Type.DELETE);
			this.queue.add(updateJob);
			batchToDelete = new HashSet<String>();
		}
		// Report the results.
		return new BuilderResponse(deleteSubmitted, 0);
	}

}
