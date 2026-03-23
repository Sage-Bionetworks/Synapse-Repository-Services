package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;

public interface TextAnalyzerService {

	TextAnalyzer create(Long userId, TextAnalyzer request);

	TextAnalyzer get(Long userId, Long id);

	TextAnalyzer update(Long userId, TextAnalyzer request);

	void delete(Long userId, Long id);

	ListTextAnalyzersResponse list(Long userId, ListTextAnalyzersRequest request);
}
