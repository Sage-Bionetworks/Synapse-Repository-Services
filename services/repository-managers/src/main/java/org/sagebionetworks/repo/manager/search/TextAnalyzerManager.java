package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;

public interface TextAnalyzerManager {

	TextAnalyzer create(UserInfo user, TextAnalyzer analyzer);

	TextAnalyzer get(UserInfo user, Long id);

	TextAnalyzer update(UserInfo user, TextAnalyzer analyzer);

	void delete(UserInfo user, Long id);

	ListTextAnalyzersResponse list(UserInfo user, ListTextAnalyzersRequest request);
}
