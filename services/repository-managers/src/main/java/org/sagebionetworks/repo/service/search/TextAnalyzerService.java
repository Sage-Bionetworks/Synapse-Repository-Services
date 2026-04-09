package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;

public interface TextAnalyzerService {

	TextAnalyzer create(Long userId, TextAnalyzer request);

	TextAnalyzer get(Long userId, Long id);

	TextAnalyzer update(Long userId, TextAnalyzer request);

	void delete(Long userId, Long id);

	ListTextAnalyzersResponse list(Long userId, ListTextAnalyzersRequest request);
}
