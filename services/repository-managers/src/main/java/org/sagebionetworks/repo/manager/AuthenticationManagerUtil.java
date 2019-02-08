package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;

public interface AuthenticationManagerUtil {
	boolean checkPassword(Long principalId, String password);

	@RequiresNewReadCommitted
	boolean checkPasswordWithLock(Long principalId, String password);
}
