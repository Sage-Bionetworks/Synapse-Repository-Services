package org.sagebionetworks.search.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.search.SearchIndexQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

/**
 * Async job worker that executes search queries against a SearchIndex's OpenSearch index.
 * Delegates to SearchIndexQueryManager for authorization, configuration resolution, and query execution.
 * If the index is still building (CREATING), throws RecoverableMessageException for auto-retry.
 */
@Service
public class SearchQueryWorker implements AsyncJobRunner<SearchIndexQuery, SearchQueryResults> {

	private static final Logger LOG = LogManager.getLogger(SearchQueryWorker.class);

	private final SearchIndexQueryManager searchIndexQueryManager;

	public SearchQueryWorker(SearchIndexQueryManager searchIndexQueryManager) {
		this.searchIndexQueryManager = searchIndexQueryManager;
	}

	@Override
	public Class<SearchIndexQuery> getRequestType() {
		return SearchIndexQuery.class;
	}

	@Override
	public Class<SearchQueryResults> getResponseType() {
		return SearchQueryResults.class;
	}

	@Override
	public SearchQueryResults run(String jobId, UserInfo user, SearchIndexQuery request,
			AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		try {
			return searchIndexQueryManager.search(user, request);
		} catch (IllegalStateException e) {
			if (e.getMessage() != null && e.getMessage().contains("still building")) {
				LOG.info("SearchIndex {} still building for job {} — will retry", request.getSearchIndexId(), jobId);
				throw new RecoverableMessageException(e.getMessage());
			}
			LOG.error("Failed to execute search query for job " + jobId
					+ " on searchIndex " + request.getSearchIndexId(), e);
			throw e;
		} catch (IllegalArgumentException e) {
			// Index in FAILED or DELETING state — terminal, propagate so AsyncJobRunnerAdapter records FAILED
			LOG.warn("Search index {} is in a terminal state for job {}: {}", request.getSearchIndexId(), jobId, e.getMessage());
			throw e;
		}
	}
}
