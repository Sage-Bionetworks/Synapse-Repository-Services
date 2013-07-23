package org.sagebionetworks.repo.manager.backup;

import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The class acts as the source and destination for backups and restoration.
 * 
 * @author jmhill
 */
public class NodeBackupManagerImpl implements NodeBackupManager {

	@Autowired 
	NodeDAO nodeDao;
	@Autowired
	private NodeBackupDAO nodeBackupDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private NodeInheritanceDAO inheritanceDAO;

	@Override
	public NodeBackup getRoot() throws DatastoreException, NotFoundException {
		// First look up the ID of the root
		String id = getRootId();
		if(id == null) throw new NotFoundException("Cannot find the root node: "+NodeConstants.ROOT_FOLDER_PATH);
		return getNode(id);
	}

	@Override
	public String getRootId() throws DatastoreException, NotFoundException {
		String id = nodeDao.getNodeIdForPath(NodeConstants.ROOT_FOLDER_PATH);
		return id;
	}

	@Override
	public NodeBackup getNode(String id) throws NotFoundException, DatastoreException {
		// Build up the node from the id
		NodeBackup backup = new NodeBackup();
		// First get the node
		backup.setNode(nodeDao.getNode(id));
		String benefactor = inheritanceDAO.getBenefactor(id);
		backup.setBenefactor(benefactor);
		// This node only has an ACL if it is its own benefactor
		if(id.equals(benefactor)){
			backup.setAcl(aclDAO.get(id, ObjectType.ENTITY));
		}
		backup.setChildren(nodeDao.getChildrenIdsAsList(id));
		backup.setRevisions(nodeDao.getVersionNumbers(id));
		return backup;
	}

	@Override
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId) throws NotFoundException, DatastoreException {
		// Pass it along
		NodeRevisionBackup rev =  nodeBackupDao.getNodeRevision(nodeId,revisionId);
		// Make sure the xml version is set to the current version
		rev.setXmlVersion(NodeRevisionBackup.CURRENT_XML_VERSION);
		return rev;
	}

	@Override
	public long getTotalNodeCount() {
		return nodeBackupDao.getTotalNodeCount();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void clearAllData() {
		try {
			String id = getRootId();
			// There is nothing to do if there is no root.
			// This is a fix for PLFM-844.
			if(id == null) return;
			// Delete it.
			nodeDao.delete(id);
		} catch (Exception e) {
			// Convert all exceptions to runtimes to force a rollback on this node.
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean doesNodeExist(String nodeId, String etag) {
		if(nodeId == null) throw new IllegalAccessError("NodeId cannot be null");
		if(etag == null) throw new IllegalArgumentException("Etag cannot be null");
		try {
			String current = nodeDao.peekCurrentEtag(nodeId);
			return etag.equals(current);
		} catch (DatastoreException e) {
			return false;
		} catch (NotFoundException e) {
			return false;
		}
	}
}
