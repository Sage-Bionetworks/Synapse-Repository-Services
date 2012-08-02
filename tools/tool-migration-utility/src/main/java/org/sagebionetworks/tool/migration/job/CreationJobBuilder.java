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
public class CreationJobBuilder implements Callable<BuilderResponse> {
	
	List<MigratableObjectData> sourceList;
	Map<MigratableObjectDescriptor, MigratableObjectData> destMap;
	private Queue<Job> queue;
	private int batchSize;
	
	/**
	 * This builder is passed everything it needs to do its job.
	 * @param sourceList
	 * @param destMap
	 * @param queue
	 * @param batchSize
	 */
	public CreationJobBuilder(List<MigratableObjectData> sourceList, Map<MigratableObjectDescriptor, MigratableObjectData> destMap, Queue<Job> queue, int batchSize) {
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
		Map<MigratableObjectType, Set<String>> batchesToCreate = new HashMap<MigratableObjectType, Set<String>>();
		for(MigratableObjectData source: sourceList){
			// Is this entity already in the destination?
			if(!destMap.containsKey(source.getId())){
				// We can only add this entity if its dependencies are in the destination
				if(JobUtil.dependenciesFulfilled(source, destMap.keySet())) {
					MigratableObjectType objectType = source.getId().getType();
					Set<String> batchToCreate = batchesToCreate.get(objectType);
					if (batchToCreate==null) {
						batchToCreate = new HashSet<String>();
						batchesToCreate.put(objectType, batchToCreate);
					}
					batchToCreate.add(source.getId().getId());
					createsSubmitted++;
					if(batchToCreate.size() >= this.batchSize){
						Job createJob = new Job(batchToCreate, objectType, Type.CREATE);
						this.queue.add(createJob);
						batchesToCreate.remove(objectType);
					}
				}else{
					// This will get picked up in a future round.
					pendingCreates++;
				}
			}
		}
		// Submit any creates left over
		for (MigratableObjectType objectType : batchesToCreate.keySet()) {
			Set<String> batchToCreate = batchesToCreate.get(objectType);
			if(!batchToCreate.isEmpty()){
				Job createJob = new Job(batchToCreate, objectType, Type.CREATE);
				this.queue.add(createJob);
			}
		}
		batchesToCreate.clear();
		// Report the results.
		return new BuilderResponse(createsSubmitted, pendingCreates);
	}


}
