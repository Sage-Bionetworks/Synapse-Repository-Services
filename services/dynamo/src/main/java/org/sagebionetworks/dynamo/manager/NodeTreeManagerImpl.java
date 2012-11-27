package org.sagebionetworks.dynamo.manager;

import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.model.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeTreeManagerImpl {

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeTreeDao nodeTreeDao;

}
