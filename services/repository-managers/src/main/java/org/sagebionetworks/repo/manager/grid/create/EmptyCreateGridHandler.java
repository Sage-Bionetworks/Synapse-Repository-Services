package org.sagebionetworks.repo.manager.grid.create;

import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.springframework.stereotype.Service;

@Service
public class EmptyCreateGridHandler implements CreateGridHandler {

	private final GridDao gridDao;

	public EmptyCreateGridHandler(GridDao gridDao) {
		super();
		this.gridDao = gridDao;
	}

	@Override
	public boolean canCreate(CreateGridRequest request) {
		return request.getInitialQuery() == null && request.getRecordSetId() == null;
	}

	@Override
	public CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			PatchStore patchStore) {
		return new CreateGridHandlerResult()
				.setGridSession(gridDao.createGridSession(new CreateGridSession().setUserId(user.getId())));
	}

}
