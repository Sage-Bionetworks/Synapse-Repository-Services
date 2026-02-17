package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

@Service
public abstract class SourceHandlerProviderImpl implements SourceHandlerProvider {

	@Override
	public SourceHandler createNewProvider(AsyncJobProgressCallback callback, UserInfo user, GridSession session,
			GridSource gridSource) {
		switch (gridSource.getType()) {
		case entityview:
			return createEntityViewHandler(callback, user, session);
		default:
			throw new IllegalArgumentException("Unsupported type: " + gridSource.getType());
		}
	}

	@Lookup
	protected abstract EntityViewSourceHandler createEntityViewHandler(AsyncJobProgressCallback callback,
			UserInfo user, GridSession session);

}
