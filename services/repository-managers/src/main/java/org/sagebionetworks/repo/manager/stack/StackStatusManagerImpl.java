package org.sagebionetworks.repo.manager.stack;

import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The manager for the stack status.
 * 
 * @author jmhill
 *
 */
@Service
public class StackStatusManagerImpl implements StackStatusManager {

	private StackStatusDao stackStatusDao;
	
	@Autowired
	public StackStatusManagerImpl(final StackStatusDao stackStatusDao) {
		this.stackStatusDao = stackStatusDao;
	}

	@Override
	public StackStatus getCurrentStatus() {
		// Just get the current status.
		return stackStatusDao.getFullCurrentStatus();
	}

	@Override
	public StackStatus updateStatus(UserInfo username, StackStatus updated) throws UnauthorizedException {
		if(updated == null) throw new IllegalArgumentException("StackStatus cannot be null");
		if(updated.getStatus() == null) throw new IllegalArgumentException("StackStatus.getStatus() cannot be null");
		UserInfo.validateUserInfo(username);
		// Only an admin can change the status.
		if(!username.isAdmin()) throw new UnauthorizedException("Must be an administrator to change the status of the stack");
		// Update the status
		stackStatusDao.updateStatus(updated);
		return stackStatusDao.getFullCurrentStatus();
	}

}
