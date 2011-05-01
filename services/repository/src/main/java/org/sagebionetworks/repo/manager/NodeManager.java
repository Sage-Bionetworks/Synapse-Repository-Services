package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.User;

public interface NodeManager {
	
	/**
	 * Create a new no
	 * @param userId
	 * @param newNode
	 * @return
	 */
	public String createNewNode(User user, Node newNode);

}
