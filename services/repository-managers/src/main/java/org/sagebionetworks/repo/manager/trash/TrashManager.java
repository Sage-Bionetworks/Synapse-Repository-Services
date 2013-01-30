package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface TrashManager {

	void moveToTrash(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;

	void restoreFromTrash(UserInfo userInfo, String nodeId);

	QueryResults<TrashedEntity> viewTrash(UserInfo userInfo, Integer offset, Integer limit);
}
