package org.sagebionetworks.repo.manager.backup;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.jdo.FieldTypeCache;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	/**
	 * Create this node.  This is important. We do not want a transaction
	 * around an entire system restoration call.  Such a transaction will not scale, and a partial
	 * restore is better than restoring nothing if there is a single bad node.
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	private void createOrUpdateNode(NodeBackup backup) {
		if(backup == null) throw new IllegalArgumentException("NodeBackup cannot be null");
		if(backup.getNode() == null) throw new IllegalArgumentException("NodeBackup.node cannot be null");
		if(backup.getNode().getId() == null) throw new IllegalArgumentException("NodeBackup.node.id cannot be null");
		if(backup.getBenefactor() == null) throw new IllegalArgumentException("NodeBackup.benefactor cannot be null");
		if(backup.getBenefactor().equals(backup.getNode().getId()) && backup.getAcl()==null) 
			throw new IllegalArgumentException("Node is it's own permissions benefactor but ACL is missing");
		if(!backup.getBenefactor().equals(backup.getNode().getId()) && backup.getAcl()!=null) 
			throw new IllegalArgumentException("Node is NOT it's own permissions benefactor, yet it has an ACL");
		String nodeId = backup.getNode().getId();
		// Does this node already exist
		try {
			// Now process the node
			if (nodeDao.doesNodeExist(KeyFactory.stringToKey(nodeId))) {
				// Update the node
				nodeDao.updateNodeFromBackup(backup.getNode());
				boolean destHasAcl = false;
				try {
					aclDAO.getForResource(backup.getNode().getId());
					destHasAcl = true;
				} catch (NotFoundException e) {
					destHasAcl = false;
				}
				if (backup.getAcl() != null) {
					if (destHasAcl) {
						aclDAO.update(backup.getAcl());
					} else {
						aclDAO.create(backup.getAcl());
					}
				} else {
					if (destHasAcl) {
						aclDAO.delete(backup.getNode().getId());
					} else {
						// neither source nor dest has an ACL, so there's nothing to do
					}
				}
			} else {
				// Update the node
				nodeDao.createNewNodeFromBackup(backup.getNode());
				if (backup.getAcl() != null) {
					aclDAO.create(backup.getAcl());
				}
			}
			// Set the benefactor without changing the etag
			boolean keepOldEtag = true;
			inheritanceDAO.addBeneficiary(nodeId, backup.getBenefactor(), keepOldEtag);
		} catch (Exception e) {
			// Convert all exceptions to runtimes to force a rollback on this
			// node.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create or update a single revision.
	 * @param rev
	 */
	private void createOrUpdateRevision(NodeRevisionBackup rev) {
		if(rev == null) throw new IllegalArgumentException("NodeRevisionBackup cannot be null");
		if(rev.getNodeId() == null) throw new IllegalArgumentException("NodeRevisionBackup.nodeId cannot be null");
		if(rev.getRevisionNumber() == null) throw new IllegalArgumentException("NodeRevisionBackup.revisionNumber cannot be null");
		if(rev.getLabel() == null) throw new IllegalArgumentException("NodeRevisionBackup.revisionNumber cannot be null");
		try{
			// Validate the annotations
			if(rev.getNamedAnnotations() != null){
				NamedAnnotations named = rev.getNamedAnnotations();
				Iterator<String> it = named.nameIterator();
				while(it.hasNext()){
					FieldTypeCache.validateAnnotations(named.getAnnotationsForName(it.next()));
				}
			}
			if(nodeDao.doesNodeRevisionExist(rev.getNodeId(), rev.getRevisionNumber())){
				// This is an update.
				nodeBackupDao.updateRevisionFromBackup(rev);
			}else{
				// This is a create
				nodeBackupDao.createNewRevisionFromBackup(rev);
			}
			
		}catch(Exception e ){
			// Convert all exceptions to runtimes to force a rollback on this node.
			throw new RuntimeException(e);
		}
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

	/**
	 * Create or update an entity within a single transaction.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void createOrUpdateNodeWithRevisions(NodeBackup backup,	List<NodeRevisionBackup> revisions) {
		if(backup == null) throw new IllegalArgumentException("backup cannot be null");
		if(revisions == null) throw new IllegalArgumentException("revisions cannot be null");
		// Make sure we process revision in their natural order
		Collections.sort(revisions, new Comparator<NodeRevisionBackup>(){
			@Override
			public int compare(NodeRevisionBackup one, NodeRevisionBackup two) {
				// Sort based on the revision number only.
				return one.getRevisionNumber().compareTo(two.getRevisionNumber());
			}} );
		// First handle the node
		createOrUpdateNode(backup);
		// Now process all revisions
		for(NodeRevisionBackup rev: revisions){
			createOrUpdateRevision(rev);
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
