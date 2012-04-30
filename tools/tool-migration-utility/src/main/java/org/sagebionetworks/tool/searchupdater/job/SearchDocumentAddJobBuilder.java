package org.sagebionetworks.tool.searchupdater.job;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.BuilderResponse;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.Job.Type;

/**
 * This builders only job is to create CreationJobs.
 * 
 * @author John
 * 
 */
public class SearchDocumentAddJobBuilder implements Callable<BuilderResponse> {

	List<EntityData> sourceList;
	Map<String, EntityData> destMap;
	private Queue<Job> queue;
	private int batchSize;

	/**
	 * This builder is passed everything it needs to do its job.
	 * 
	 * @param sourceList
	 * @param destMap
	 * @param queue
	 * @param batchSize
	 */
	public SearchDocumentAddJobBuilder(List<EntityData> sourceList,
			Map<String, EntityData> destMap, Queue<Job> queue, int batchSize) {
		super();
		this.sourceList = sourceList;
		this.destMap = destMap;
		this.queue = queue;
		this.batchSize = batchSize;
	}

	@Override
	public BuilderResponse call() throws Exception {
		Set<String> batchToAdd = new HashSet<String>();
		int addsSubmitted = 0;

		// Walk over the source list
		for (EntityData source : sourceList) {
			String sourceEntityId = source.getEntityId();
			if (!destMap.containsKey(sourceEntityId)) {
				// Search index doesn't have this entity, add it
				batchToAdd.add(sourceEntityId);
				addsSubmitted++;
			} else {
				EntityData destEntity = destMap.get(sourceEntityId);
				if(null == destEntity) {
					destEntity = destMap.get(sourceEntityId);
				}
				if (!source.geteTag().equals(destEntity.geteTag())) {
					// Search index has an out of date version of this entity,
					// add it
					batchToAdd.add(sourceEntityId);
					addsSubmitted++;
				}
			}

			if (batchToAdd.size() >= this.batchSize) {
				Job addJob = new Job(batchToAdd, Type.SEARCH_ADD);
				this.queue.add(addJob);
				batchToAdd = new HashSet<String>();
			}
		}
		// Submit any adds left over
		if (!batchToAdd.isEmpty()) {
			Job addJob = new Job(batchToAdd, Type.SEARCH_ADD);
			this.queue.add(addJob);
			batchToAdd = new HashSet<String>();
		}
		// Report the results.
		return new BuilderResponse(addsSubmitted, 0);
	}
}
