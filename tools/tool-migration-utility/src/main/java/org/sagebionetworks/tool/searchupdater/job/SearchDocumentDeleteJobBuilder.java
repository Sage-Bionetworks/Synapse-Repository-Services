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
 * @author deflaux
 * 
 */
public class SearchDocumentDeleteJobBuilder implements
		Callable<BuilderResponse> {

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
	public SearchDocumentDeleteJobBuilder(Map<String, EntityData> sourceMap,
			List<EntityData> destList, Queue<Job> queue, int batchSize) {
		super();
		this.sourceMap = sourceMap;
		this.destList = destList;
		this.queue = queue;
		this.batchSize = batchSize;
	}

	@Override
	public BuilderResponse call() throws Exception {
		Set<String> batchToDelete = new HashSet<String>();
		int deleteSubmitted = 0;

		// Walk over the destination list.
		for (EntityData dest : destList) {
			// Find any entity in the destination that does not exist in the
			// source.
			if (!sourceMap.containsKey(dest.getEntityId())
					// Work around bug PLFM-1270
					&& !sourceMap.containsKey(dest.getEntityId().replaceFirst("syn", ""))
					) {
				// The entity exists in the destination but not the source.
				batchToDelete.add(dest.getEntityId());
				deleteSubmitted++;
			}
			if (batchToDelete.size() >= this.batchSize) {
				Job deleteJob = new Job(batchToDelete, Type.SEARCH_DELETE);
				this.queue.add(deleteJob);
				batchToDelete = new HashSet<String>();
			}
		}

		// Submit any deletes left over
		if (!batchToDelete.isEmpty()) {
			Job deleteJob = new Job(batchToDelete, Type.SEARCH_DELETE);
			this.queue.add(deleteJob);
			batchToDelete = new HashSet<String>();
		}

		// Report the results.
		return new BuilderResponse(deleteSubmitted, 0);
	}

}
