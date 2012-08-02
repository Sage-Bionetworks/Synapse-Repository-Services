package org.sagebionetworks.tool.migration.job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.tool.migration.job.Job.Type;

/**
 * This builders only job is to create CreationJobs.
 * 
 * @author John
 *
 */
public class DeleteJobBuilder implements Callable<BuilderResponse> {
	
	Map<MigratableObjectDescriptor, MigratableObjectData> sourceMap;
	List<MigratableObjectData> destList;
	private Queue<Job> queue;
	private int batchSize;


	/**
	 * 
	 * @param sourceMap
	 * @param destList
	 * @param queue
	 * @param batchSize
	 */
	public DeleteJobBuilder(Map<MigratableObjectDescriptor, MigratableObjectData> sourceMap,
			List<MigratableObjectData> destList, Queue<Job> queue, int batchSize) {
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
		//Set<MigratableObjectDescriptor> batchToDelete = new HashSet<MigratableObjectDescriptor>();
		Map<MigratableObjectType, Set<String>> batchesToDelete = new HashMap<MigratableObjectType, Set<String>>();
		// Walk over the dest list.
		for(MigratableObjectData dest: destList){
			// Find any entity in the destination that does not exist in the source.
			if(!sourceMap.containsKey(dest.getId())){
				MigratableObjectType objectType = dest.getId().getType();
				Set<String> batchToDelete = batchesToDelete.get(objectType);
				if (batchToDelete==null) {
					batchToDelete = new HashSet<String>();
					batchesToDelete.put(objectType, batchToDelete);
				}
				batchToDelete.add(dest.getId().getId());
				deleteSubmitted++;
				if(batchToDelete.size() >= this.batchSize){
					Job createJob = new Job(batchToDelete, objectType, Type.DELETE);
					this.queue.add(createJob);
					batchesToDelete.remove(objectType);
				}
			}
		}
		// Submit any creates left over
		for (MigratableObjectType objectType : batchesToDelete.keySet()) {
			Set<String> batchToDelete = batchesToDelete.get(objectType);
			if(!batchToDelete.isEmpty()){
				Job updateJob = new Job(batchToDelete, objectType, Type.DELETE);
				this.queue.add(updateJob);
			}
		}
		batchesToDelete.clear();
		// Report the results.
		return new BuilderResponse(deleteSubmitted, 0);
	}
	

}
