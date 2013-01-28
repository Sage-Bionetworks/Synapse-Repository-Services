package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserInfo;

public class TrashManagerImpl implements TrashManager {

	
	@Override
	public void moveToTrash(UserInfo userInfo, String nodeId) {
		
	}

	@Override
	public void restoreFromTrash(UserInfo userInfo, String nodeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public QueryResults<TrashedEntity> viewTrash(UserInfo userInfo,
			Integer offset, Integer limit) {
		// TODO Auto-generated method stub
		return null;
	}

}
