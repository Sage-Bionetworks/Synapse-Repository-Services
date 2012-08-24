package org.sagebionetworks.tool.migration.dao;

import static org.sagebionetworks.tool.migration.Constants.MS_BETWEEN_SYNPASE_CALLS;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

public class MigrationQueryRunner implements QueryRunner<MigratableObjectData> {

	private SynapseAdministration client;
	
	private boolean queryForDependencies;
	
	public MigrationQueryRunner(SynapseAdministration client, boolean queryForDependencies) {
		this.client = client;
		this.queryForDependencies = queryForDependencies;
	}
	
	public static final long PAGE_SIZE = 1000L; // was 100
	public static final int MAX_RETRIES = 3;
	
	@Override
	public List<MigratableObjectData> getAllEntityData(BasicProgress progress)
			throws SynapseException, JSONException, InterruptedException,
			JSONObjectAdapterException {
		
		List<MigratableObjectData> results = new ArrayList<MigratableObjectData>();
		// First run the first page
		long offset = 0;
		PaginatedResults<MigratableObjectData> page = client.getAllMigratableObjectsPaginated(offset, PAGE_SIZE, queryForDependencies);

		results.addAll(page.getResults());
		long totalCount = page.getTotalNumberOfResults();
		// Update the progress if we have any
		if(progress != null){
			progress.setTotal(totalCount);
		}
		// Get as many pages as needed
		while((offset = getNextOffset(offset, PAGE_SIZE, totalCount)) > 0l){
			int retryCount = 0;
			while (retryCount<MAX_RETRIES) {
				try {
					page = client.getAllMigratableObjectsPaginated(offset, PAGE_SIZE, queryForDependencies);
					break;
				} catch (SynapseServiceException e) {
					// will retry, unless we've hit the retry limit
					if (retryCount>=MAX_RETRIES-1) {
						throw e;
					}
				}
				Thread.sleep(1000L);
				retryCount++;
			}
			if(progress != null){
				// Add this count to the current progress.
				progress.setCurrent(progress.getCurrent() + page.getResults().size());
			}
			results.addAll(page.getResults());
			// Yield between queries
			Thread.sleep(MS_BETWEEN_SYNPASE_CALLS);
		}
		
		if(progress != null){
			// Add this count to the current progress.
			progress.setCurrent(progress.getTotal());
		}
		return results;
	}

	/**
	 * What is the next offset given the current-offset, limit, and total count.
	 * Returns -1 when done paging.
	 * @param offset
	 * @param limit
	 * @param totalCount
	 * @return
	 */
	public static long getNextOffset(long offset, long limit, long totalCount){
		long next = offset+limit;
		if(next > totalCount) return -1;
		else return next;
	}

	@Override
	public long getTotalEntityCount() throws SynapseException, JSONException,
			JSONObjectAdapterException {
		// TODO add a special service that just gives the total, without computing a page of results
		PaginatedResults<MigratableObjectData> results = client.getAllMigratableObjectsPaginated(0, PAGE_SIZE, false);
		return (int)results.getTotalNumberOfResults();
	}

	@Override
	public MigratableObjectData getRootEntity() throws SynapseException,
			JSONException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<MigratableObjectData> getAllEntityDataOfType(EntityType type,
			BasicProgress progress) throws SynapseException, JSONException,
			InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<MigratableObjectData> getAllChildrenOfEntity(String parentId)
			throws SynapseException, IllegalAccessException, JSONException,
			InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getCountForType(EntityType type) throws SynapseException,
			JSONException {
		throw new UnsupportedOperationException();
	}


}
