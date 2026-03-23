package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.TextAnalyzerManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class TextAnalyzerServiceImpl implements TextAnalyzerService {

	private final UserManager userManager;
	private final TextAnalyzerManager textAnalyzerManager;

	public TextAnalyzerServiceImpl(UserManager userManager, TextAnalyzerManager textAnalyzerManager) {
		this.userManager = userManager;
		this.textAnalyzerManager = textAnalyzerManager;
	}

	@Override
	public TextAnalyzer create(Long userId, TextAnalyzer request) {
		UserInfo user = userManager.getUserInfo(userId);
		return textAnalyzerManager.create(user, request);
	}

	@Override
	public TextAnalyzer get(Long userId, Long id) {
		UserInfo user = userManager.getUserInfo(userId);
		return textAnalyzerManager.get(user, id);
	}

	@Override
	public TextAnalyzer update(Long userId, TextAnalyzer request) {
		UserInfo user = userManager.getUserInfo(userId);
		return textAnalyzerManager.update(user, request);
	}

	@Override
	public void delete(Long userId, Long id) {
		UserInfo user = userManager.getUserInfo(userId);
		textAnalyzerManager.delete(user, id);
	}

	@Override
	public ListTextAnalyzersResponse list(Long userId, ListTextAnalyzersRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return textAnalyzerManager.list(user, request);
	}
}
