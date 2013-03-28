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
public class UpdateJobBuilder implements Callable<BuilderResponse> {
	
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
	public UpdateJobBuilder(List<MigratableObjectData> sourceList, Map<MigratableObjectDescriptor, MigratableObjectData> destMap, Queue<Job> queue, int batchSize) {
		super();
		this.sourceList = sourceList;
		this.destMap = destMap;
		this.queue = queue;
		this.batchSize = batchSize;
	}
		
	private static boolean etagsDiffer(String sourceEtag, String destEtag) {
		if (sourceEtag == null) {
			if (destEtag == null) return false;
			return true;
		}
		return !sourceEtag.equals(destEtag);
	}

	@Override
	public BuilderResponse call() throws Exception {
		try {
			// Get the two clients		
			int updateSubmitted = 0;
			// Walk over the source list
			Map<MigratableObjectType, Set<String>> batchesToUpdate = new HashMap<MigratableObjectType, Set<String>>();
			for(MigratableObjectData source: sourceList) {
				// We only care about entities that already exist
				MigratableObjectData destObject = destMap.get(source.getId());
				if(destObject != null){
					// Do the eTags match?
					if(     etagsDiffer(source.getEtag(), destObject.getEtag())
							// also check dependencies
							&& JobUtil.dependenciesFulfilled(source, destMap.keySet())
					) {
						System.out.println("UpdateJobBuilder: Need to update "+source.getId()+" source etag is "+source.getEtag()+
								" while dest etag is "+destObject.getEtag());
						// Tags do not match. New dependencies are in place.  Let's migrate it!
						MigratableObjectType objectType = source.getId().getType();
						Set<String> batchToUpdate = batchesToUpdate.get(objectType);
						if (batchToUpdate==null) {
							batchToUpdate = new HashSet<String>();
							batchesToUpdate.put(objectType, batchToUpdate);
						}
						batchToUpdate.add(source.getId().getId());
						updateSubmitted++;
						if(batchToUpdate.size() >= this.batchSize){
							Job createJob = new Job(batchToUpdate, objectType, Type.UPDATE);
							this.queue.add(createJob);
							batchesToUpdate.remove(objectType);
						}
					}
				}
			}
			// Submit any updates left over
			for (MigratableObjectType objectType : batchesToUpdate.keySet()) {
				Set<String> batchToUpdate = batchesToUpdate.get(objectType);
				if(!batchToUpdate.isEmpty()) {
					Job updateJob = new Job(batchToUpdate, objectType, Type.UPDATE);
					this.queue.add(updateJob);
				}
			}
			batchesToUpdate.clear();
			// Report the results.
			return new BuilderResponse(updateSubmitted, 0);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}


}
