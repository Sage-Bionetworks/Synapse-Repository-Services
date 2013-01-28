package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserInfo;

public interface TrashManager {

	void moveToTrash(UserInfo userInfo, String nodeId);

	void restoreFromTrash(UserInfo userInfo, String nodeId);

	QueryResults<TrashedEntity> viewTrash(UserInfo userInfo, Integer offset, Integer limit);
}
