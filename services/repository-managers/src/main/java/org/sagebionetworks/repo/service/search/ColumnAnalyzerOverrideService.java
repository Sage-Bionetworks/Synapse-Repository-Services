package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesResponse;

public interface ColumnAnalyzerOverrideService {

	ColumnAnalyzerOverride create(Long userId, ColumnAnalyzerOverride request);

	ColumnAnalyzerOverride get(Long userId, String id);

	ColumnAnalyzerOverride update(Long userId, ColumnAnalyzerOverride request);

	void delete(Long userId, String id);

	ListColumnAnalyzerOverridesResponse list(Long userId, ListColumnAnalyzerOverridesRequest request);
}
