package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.dynamo.dao.nodetree.IncompletePathException;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;

/**
 * 
 * This is a mock NodeTreeQueryDao used for autowired tests in the manager project where Dynamo is not available
 * 
 * @author brucehoff
 *
 */
public class MockNodeTreeQueryDao implements NodeTreeQueryDao {

	@Override
	public boolean isRoot(String nodeId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getAncestors(String nodeId)
			throws IncompletePathException {
		return new ArrayList<String>();
	}

	@Override
	public String getParent(String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDescendants(String nodeId, int pageSize,
			String lastDescIdExcl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDescendants(String nodeId, int generation,
			int pageSize, String lastDescIdExcl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDynamoEnabled() {
		return true;
	}

}
