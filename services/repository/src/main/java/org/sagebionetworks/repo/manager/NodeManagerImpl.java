package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Sage business logic for node management.
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeManagerImpl implements NodeManager {
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	AuthorizationDAO authorizationDao;
	
	@Override
	public String createNewNode(User user, Node newNode) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	

}
