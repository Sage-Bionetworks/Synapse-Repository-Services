package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;

public interface AuthenticationManagerUtil {
	boolean checkPassword(Long principalId, String password);

	@RequiresNewReadCommitted
	boolean authenticateWithLock(Long principalId, String password);
}
