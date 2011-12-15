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
public class UpdateJobBuilder implements Callable<BuilderResponse> {
	
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
	public UpdateJobBuilder(List<EntityData> sourceList, Map<String, EntityData> destMap, Queue<Job> queue, int batchSize) {
		super();
		this.sourceList = sourceList;
		this.destMap = destMap;
		this.queue = queue;
		this.batchSize = batchSize;
	}

	@Override
	public BuilderResponse call() throws Exception {
		// Get the two clients		
		int updateSubmitted = 0;
		// Walk over the source list
		Set<String> batchToUpdate = new HashSet<String>();
		for(EntityData source: sourceList){
			// We only care about entities that already exist
			EntityData destEtntiy = destMap.get(source.getEntityId());
			if(destEtntiy != null){
				// Do the eTags match?
				if(!source.geteTag().equals(destEtntiy.geteTag())){
					// Tags do not match
					batchToUpdate.add(source.getEntityId());
					updateSubmitted++;
				}
			}
			if(batchToUpdate.size() >= this.batchSize){
				Job createJob = new Job(batchToUpdate, Type.UPDATE);
				this.queue.add(createJob);
				batchToUpdate = new HashSet<String>();
			}
		}
		// Submit any creates left over
		if(!batchToUpdate.isEmpty()){
			Job updateJob = new Job(batchToUpdate, Type.UPDATE);
			this.queue.add(updateJob);
			batchToUpdate = new HashSet<String>();
		}
		// Report the results.
		return new BuilderResponse(updateSubmitted, 0);
	}


}
