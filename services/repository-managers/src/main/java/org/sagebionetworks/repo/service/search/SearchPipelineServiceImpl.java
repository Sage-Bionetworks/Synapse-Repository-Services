package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchPipelineManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesRequest;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesResponse;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;
import org.springframework.stereotype.Service;

@Service
public class SearchPipelineServiceImpl implements SearchPipelineService {

	private final UserManager userManager;
	private final SearchPipelineManager searchPipelineManager;

	public SearchPipelineServiceImpl(UserManager userManager, SearchPipelineManager searchPipelineManager) {
		this.userManager = userManager;
		this.searchPipelineManager = searchPipelineManager;
	}

	@Override
	public NamedSearchPipeline create(Long userId, NamedSearchPipeline request) {
		UserInfo user = userManager.getUserInfo(userId);
		return searchPipelineManager.create(user, request);
	}

	@Override
	public NamedSearchPipeline get(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return searchPipelineManager.get(user, id);
	}

	@Override
	public NamedSearchPipeline update(Long userId, NamedSearchPipeline request) {
		UserInfo user = userManager.getUserInfo(userId);
		return searchPipelineManager.update(user, request);
	}

	@Override
	public ListNamedSearchPipelinesResponse list(Long userId, ListNamedSearchPipelinesRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return searchPipelineManager.list(user, request);
	}
}
