package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.ColumnAnalyzerOverrideManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.search.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.table.search.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.table.search.ListColumnAnalyzerOverridesResponse;
import org.springframework.stereotype.Service;

@Service
public class ColumnAnalyzerOverrideServiceImpl implements ColumnAnalyzerOverrideService {

	private final UserManager userManager;
	private final ColumnAnalyzerOverrideManager columnAnalyzerOverrideManager;

	public ColumnAnalyzerOverrideServiceImpl(UserManager userManager, ColumnAnalyzerOverrideManager columnAnalyzerOverrideManager) {
		this.userManager = userManager;
		this.columnAnalyzerOverrideManager = columnAnalyzerOverrideManager;
	}

	@Override
	public ColumnAnalyzerOverride create(Long userId, ColumnAnalyzerOverride request) {
		UserInfo user = userManager.getUserInfo(userId);
		return columnAnalyzerOverrideManager.create(user, request);
	}

	@Override
	public ColumnAnalyzerOverride get(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return columnAnalyzerOverrideManager.get(user, id);
	}

	@Override
	public ColumnAnalyzerOverride update(Long userId, ColumnAnalyzerOverride request) {
		UserInfo user = userManager.getUserInfo(userId);
		return columnAnalyzerOverrideManager.update(user, request);
	}

	@Override
	public void delete(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		columnAnalyzerOverrideManager.delete(user, id);
	}

	@Override
	public ListColumnAnalyzerOverridesResponse list(Long userId, ListColumnAnalyzerOverridesRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return columnAnalyzerOverrideManager.list(user, request);
	}
}
