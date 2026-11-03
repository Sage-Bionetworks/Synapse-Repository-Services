package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesRequest;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesResponse;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;

public interface SearchPipelineService {

	NamedSearchPipeline create(Long userId, NamedSearchPipeline request);

	NamedSearchPipeline get(Long userId, String id);

	NamedSearchPipeline update(Long userId, NamedSearchPipeline request);

	ListNamedSearchPipelinesResponse list(Long userId, ListNamedSearchPipelinesRequest request);
}
