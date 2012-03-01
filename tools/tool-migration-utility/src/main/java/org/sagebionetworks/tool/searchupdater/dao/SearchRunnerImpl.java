package org.sagebionetworks.tool.searchupdater.dao;

import static org.sagebionetworks.tool.migration.Constants.ENTITY;
import static org.sagebionetworks.tool.migration.Constants.MAX_PAGE_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.EntityQueryResults;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.searchupdater.CloudSearchClient;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * @author deflaux
 * 
 */
public class SearchRunnerImpl implements QueryRunner {

	private static final String SEARCH_TOTAL_ENTITY = "bq=created_on:0..&return-fields=id,etag";
	private static final String SEARCH_TOTAL_ENTITY_COUNT = "bq=created_on:0..";
	private static Log log = LogFactory.getLog(SearchRunnerImpl.class);
	private static final AwesomeSearchFactory searchResultsFactory = new AwesomeSearchFactory(new AdapterFactoryImpl());

	private CloudSearchClient csClient;

	/**
	 * @param csClient
	 */
	public SearchRunnerImpl(CloudSearchClient csClient) {
		this.csClient = csClient;
	}

	@Override
	public List<EntityData> getAllEntityData(BasicProgress progress)
			throws SynapseException, JSONException, InterruptedException, JSONObjectAdapterException {
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
	public long getTotalEntityCount() throws JSONException, SynapseException, JSONObjectAdapterException {
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
		EntityQueryResults results = fromAwesomeSearchResults(response);
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
	 * @throws JSONObjectAdapterException 
	 */
	List<EntityData> searchForAllPages(String rootSearch, String prefix,
			long limit, BasicProgress progress) throws ClientProtocolException,
			IOException, HttpClientHelperException, JSONException,
			InterruptedException, JSONObjectAdapterException {

		List<EntityData> results = new ArrayList<EntityData>();
		// First run the first page
		long offset = 0;
		String search = getPageSearch(rootSearch, limit, offset);
		String response = csClient.performSearch(search);
		EntityQueryResults page = fromAwesomeSearchResults(response);
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
			page = fromAwesomeSearchResults(response);
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
	 * @throws JSONObjectAdapterException
	 */
	EntityQueryResults fromAwesomeSearchResults(
			String awesomeSearchResults) throws JSONObjectAdapterException, JSONException {
		List<EntityData> results = new ArrayList<EntityData>();

		if (log.isDebugEnabled()) {
			log.debug(new JSONObject(awesomeSearchResults).toString(4));
		}

		SearchResults searchResults = searchResultsFactory
				.fromAwesomeSearchResults(awesomeSearchResults);

		for (Hit hit : searchResults.getHits()) {
			if (null != hit.getId() && null != hit.getEtag()) {
				EntityData entityData = new EntityData(hit.getId(), hit
						.getEtag(), null);
				results.add(entityData);
			}
		}

		return new EntityQueryResults(results, searchResults.getFound());
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
