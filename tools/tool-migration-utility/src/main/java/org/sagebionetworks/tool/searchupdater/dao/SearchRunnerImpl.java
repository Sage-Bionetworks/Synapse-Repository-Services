package org.sagebionetworks.tool.searchupdater.dao;

import static org.sagebionetworks.tool.migration.Constants.ENTITY;
import static org.sagebionetworks.tool.migration.Constants.MAX_PAGE_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.EntityQueryResults;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.searchupdater.CloudSearchClient;
import org.sagebionetworks.tool.searchupdater.SearchMigrationDriver;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * @author deflaux
 * 
 */
public class SearchRunnerImpl implements QueryRunner {

	private static final String SEARCH_TOTAL_ENTITY = "bq=created_on:0..&return-fields=id,etag";
	private static final String SEARCH_TOTAL_ENTITY_COUNT = "bq=created_on:0..";
	static private Log log = LogFactory.getLog(SearchRunnerImpl.class);

	private CloudSearchClient csClient;

	/**
	 * @param csClient
	 */
	public SearchRunnerImpl(CloudSearchClient csClient) {
		this.csClient = csClient;
	}

	@Override
	public List<EntityData> getAllEntityData(BasicProgress progress)
			throws SynapseException, JSONException, InterruptedException {
		try {
			return searchForAllPages(SEARCH_TOTAL_ENTITY, ENTITY,
					MAX_PAGE_SIZE, progress);
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public long getTotalEntityCount() throws JSONException, SynapseException {
		String response;
		try {
			response = csClient.performSearch(SEARCH_TOTAL_ENTITY_COUNT);

		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
		EntityQueryResults results = translateFromSearchResultsToEntityQueryResults(response);
		return results.getTotalCount();
	}

	/**
	 * Get all of the pages starting with a root search. This will execute the
	 * search one page at a time until all of the results are fetched. The page
	 * size is set by the limit.
	 * 
	 * @param rootSearch
	 * @param limit
	 * @return
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	List<EntityData> searchForAllPages(String rootSearch, String prefix,
			long limit, BasicProgress progress) throws ClientProtocolException,
			IOException, HttpClientHelperException, JSONException,
			InterruptedException {

		List<EntityData> results = new ArrayList<EntityData>();
		// First run the first page
		long offset = 0;
		String search = getPageSearch(rootSearch, limit, offset);
		String response = csClient.performSearch(search);
		EntityQueryResults page = translateFromSearchResultsToEntityQueryResults(response);
		results.addAll(page.getResults());
		long totalCount = page.getTotalCount();
		// Update the progress if we have any
		if (progress != null) {
			progress.setTotal(totalCount);
		}
		// Get as many pages as needed
		while ((offset = getNextOffset(offset, limit, totalCount)) > 0l) {
			search = getPageSearch(rootSearch, limit, offset);
			response = csClient.performSearch(search);
			page = translateFromSearchResultsToEntityQueryResults(response);
			if (progress != null) {
				// Add this count to the current progress.
				progress.setCurrent(progress.getCurrent()
						+ page.getResults().size());
			}
			results.addAll(page.getResults());
			// Yield between queries
			Thread.sleep(1);
		}

		if (progress != null) {
			// Add this count to the current progress.
			progress.setCurrent(progress.getTotal());
		}
		return results;
	}

	/**
	 * What is the next offset given the current-offset, limit, and total count.
	 * Returns -1 when done paging.
	 * 
	 * @param offset
	 * @param limit
	 * @param totalCount
	 * @return
	 */
	long getNextOffset(long offset, long limit, long totalCount) {
		long next = offset + limit;
		if (next > totalCount)
			return -1;
		return next;
	}

	/**
	 * Add paging to a root search.
	 * 
	 * @param rootSearch
	 * @param limit
	 * @param offset
	 * @return
	 */
	String getPageSearch(String rootSearch, long limit, long offset) {
		StringBuilder builder = new StringBuilder();
		builder.append(rootSearch);
		builder.append("&size=");
		builder.append(limit);
		builder.append("&start=");
		builder.append(offset);
		return builder.toString();
	}

	/**
	 * Translate the query results from a JSONObject
	 * 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	EntityQueryResults translateFromSearchResultsToEntityQueryResults(
			String searchResults) throws JSONException {
		List<EntityData> results = new ArrayList<EntityData>();

		JSONObject json = new JSONObject(searchResults);

		if (log.isDebugEnabled()) {
			log.debug(json.toString(4));
		}

		long total = json.getJSONObject("hits").getLong("found");
		JSONArray rows = json.getJSONObject("hits").getJSONArray("hit");
		for (int i = 0; i < rows.length(); i++) {
			JSONObject row = rows.getJSONObject(i);
			if (row.has("data")) {
				JSONObject data = row.getJSONObject("data");
				if (data.has("id") && data.has("etag")) {
					EntityData entityData = new EntityData(data.getString("id"), data
							.getString("etag"), null);
					results.add(entityData);
				}
			}
		}

		return new EntityQueryResults(results, total);
	}

	@Override
	public List<EntityData> getAllAllChildrenOfEntity(String parentId)
			throws SynapseException, IllegalAccessException, JSONException,
			InterruptedException {
		throw new IllegalArgumentException("not implemented");
	}

	@Override
	public List<EntityData> getAllEntityDataOfType(EntityType type,
			BasicProgress progress) throws SynapseException, JSONException,
			InterruptedException {
		throw new IllegalArgumentException("not implemented");
	}

	@Override
	public long getCountForType(EntityType type) throws SynapseException,
			JSONException {
		throw new IllegalArgumentException("not implemented");
	}

	@Override
	public EntityData getRootEntity() throws SynapseException, JSONException {
		throw new IllegalArgumentException("not implemented");
	}

}
