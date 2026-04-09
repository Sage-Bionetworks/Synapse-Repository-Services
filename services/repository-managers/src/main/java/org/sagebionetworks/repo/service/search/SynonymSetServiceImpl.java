package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SynonymSetManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.springframework.stereotype.Service;

@Service
public class SynonymSetServiceImpl implements SynonymSetService {

	private final UserManager userManager;
	private final SynonymSetManager synonymSetManager;

	public SynonymSetServiceImpl(UserManager userManager, SynonymSetManager synonymSetManager) {
		this.userManager = userManager;
		this.synonymSetManager = synonymSetManager;
	}

	@Override
	public SynonymSet create(Long userId, SynonymSet request) {
		UserInfo user = userManager.getUserInfo(userId);
		return synonymSetManager.create(user, request);
	}

	@Override
	public SynonymSet get(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return synonymSetManager.get(user, id);
	}

	@Override
	public SynonymSet update(Long userId, SynonymSet request) {
		UserInfo user = userManager.getUserInfo(userId);
		return synonymSetManager.update(user, request);
	}

	@Override
	public void delete(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		synonymSetManager.delete(user, id);
	}

	@Override
	public ListSynonymSetsResponse list(Long userId, ListSynonymSetsRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return synonymSetManager.list(user, request);
	}
}
