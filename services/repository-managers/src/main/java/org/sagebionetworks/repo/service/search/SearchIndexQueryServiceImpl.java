package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchIndexQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.service.AsynchronousJobServices;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexQueryServiceImpl implements SearchIndexQueryService {

	private final UserManager userManager;
	private final AsynchronousJobServices asynchJobServices;
	private final SearchIndexQueryManager searchIndexQueryManager;

	public SearchIndexQueryServiceImpl(UserManager userManager, AsynchronousJobServices asynchJobServices,
			SearchIndexQueryManager searchIndexQueryManager) {
		this.userManager = userManager;
		this.asynchJobServices = asynchJobServices;
		this.searchIndexQueryManager = searchIndexQueryManager;
	}

	@Override
	public AsynchronousJobStatus startSearchQuery(Long userId, SearchIndexQuery query) {
		return asynchJobServices.startJob(userId, query);
	}

	@Override
	public AsynchronousJobStatus getSearchQueryResults(Long userId, String asyncToken) throws Throwable {
		return asynchJobServices.getJobStatusAndThrow(userId, asyncToken);
	}

	@Override
	public SearchQueryResults autocomplete(Long userId, SearchAutocompleteRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return searchIndexQueryManager.autocomplete(userInfo, request);
	}
}
