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
public class CreationJobBuilder implements Callable<BuilderResponse> {
	
	List<EntityData> sourceList;
	Map<String, EntityData> destMap;
	private Queue<Job> queue;
	private int batchSize;


	/**
	 * This builder is passed everything it needs to do its job.
	 * @param sourceList
	 * @param destMap
	 * @param queue
	 * @param batchSize
	 */
	public CreationJobBuilder(List<EntityData> sourceList,	Map<String, EntityData> destMap, Queue<Job> queue, int batchSize) {
		super();
		this.sourceList = sourceList;
		this.destMap = destMap;
		this.queue = queue;
		this.batchSize = batchSize;
	}

	@Override
	public BuilderResponse call() throws Exception {
		// Get the two clients		
		int createsSubmitted = 0;
		int pendingCreates = 0;
		// Walk over the source list
		Set<String> batchToCreate = new HashSet<String>();
		for(EntityData source: sourceList){
			// Is this entity already in the destination?
			if(!destMap.containsKey(source.getEntityId())){
				
				if((source.getParentId() == null)){
					// It is always safe to add the root entity.
					batchToCreate.add(source.getEntityId());
					createsSubmitted++;
				}else{
					// We can only add this entity if the parent is already in the destination
					if(destMap.containsKey(source.getParentId())){
						batchToCreate.add(source.getEntityId());
						createsSubmitted++;
					}else{
						// This will get picked up in a future round.
						pendingCreates++;
					}
				}
			}
			if(batchToCreate.size() >= this.batchSize){
				Job createJob = new Job(batchToCreate, Type.CREATE);
				this.queue.add(createJob);
				batchToCreate = new HashSet<String>();
			}
		}
		// Submit any creates left over
		if(!batchToCreate.isEmpty()){
			Job createJob = new Job(batchToCreate, Type.CREATE);
			this.queue.add(createJob);
			batchToCreate = new HashSet<String>();
		}
		// Report the results.
		return new BuilderResponse(createsSubmitted, pendingCreates);
	}


}
