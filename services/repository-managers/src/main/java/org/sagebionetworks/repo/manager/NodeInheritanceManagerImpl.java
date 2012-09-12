package org.sagebionetworks.repo.manager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author jmhill
 *
 */
@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
public class NodeInheritanceManagerImpl implements NodeInheritanceManager {
	
	@Autowired
	NodeInheritanceDAO nodeInheritanceDao;
	@Autowired
	NodeDAO nodeDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void nodeParentChanged(String nodeId, String parentNodeId) throws NotFoundException, DatastoreException {
		//first determine who this node is inheriting from
		String oldBenefactorId = nodeInheritanceDao.getBenefactor(nodeId);		
		//if node inherits from itself everything is in order
		if (oldBenefactorId.equals(nodeId)){
			return;
		}		
		//here node needs to be set to nearest benefactor and children
		//need to be adjusted accordingly.  Nearest benefactor will be 
		//set to what the parent node has as benefactor
		String changeToId = nodeInheritanceDao.getBenefactor(parentNodeId);

		//change our node and all appropriate children
		changeAllChildrenTo(oldBenefactorId, nodeId, changeToId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setNodeToInheritFromItself(String nodeId) throws NotFoundException, DatastoreException {
		// Find all children of this node that are currently inheriting from the same 
		// benefactor.
		String currentBenefactorId = nodeInheritanceDao.getBenefactor(nodeId);
		// There is nothing to do if a node already inherits from itself
		if(nodeId.equals(currentBenefactorId)) return;
		// Change all the required children
		changeAllChildrenTo(currentBenefactorId, nodeId, nodeId);
	}
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setNodeToInheritFromNearestParent(String nodeId) throws NotFoundException, DatastoreException {
		// First determine who this node is inheriting from.
		String currentBenefactorId = nodeInheritanceDao.getBenefactor(nodeId);
		Node node = nodeDao.getNode(nodeId);
		String changeToId = null;
		if(node.getParentId() == null){
			// this node should inherit from itself.
			// If this node is already inheriting from itself then there is nothing to do
			if(currentBenefactorId == nodeId) return;
			// Change to this node
			changeToId = nodeId;
		}else{
			// Change to the parent's benefactor
			changeToId = nodeInheritanceDao.getBenefactor(node.getParentId());
		}
		// Do the change
		// Change all the required children
		changeAllChildrenTo(currentBenefactorId, nodeId, changeToId);
	}
	
	/**
	 * Change all children of the given parent(parentId), that are currently inheriting from the given id (currentlyInheritingFromId),
	 * to now inherit from given id (changeToInheritFromId).
	 * @param currentlyInheritingFromId - Children currently inheriting from this nodeId will be changed.
	 * @param parentId - The parent 
	 * @param changeToInheritFromId
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	private void changeAllChildrenTo(String currentlyInheritingFromId, String parentId, String changeToInheritFromId) throws NotFoundException, DatastoreException{
		// This is the set of nodes that will need to change.
		Set<String> toChange = new HashSet<String>();
		// Recursively build up the set to change
		// Now add all children
		addChildrenToChange(currentlyInheritingFromId, parentId, toChange);
		// We sort the Ids to prevent deadlock on concurrent updates
		String[] sortedArrayIds = toChange.toArray(new String[toChange.size()]);
		Arrays.sort(sortedArrayIds);
		// Now update each node.
		for(String idToChange: sortedArrayIds){
			nodeInheritanceDao.addBeneficiary(idToChange, changeToInheritFromId);
		}
	}
	
	/**
	 * This is a recursive method that finds all of the nodes that need to change.
	 * @param currentBenefactorId
	 * @param parentId
	 * @param toChange
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	private void addChildrenToChange(String currentBenefactorId, String parentId, Set<String> toChange) throws NotFoundException, DatastoreException{
		// Find find the parent's benefactor
		String parentCurrentBenefactorId = nodeInheritanceDao.getBenefactor(parentId);
		if(parentCurrentBenefactorId.equals(currentBenefactorId)){
			toChange.add(parentId);
			// Now check this node's children
			Iterator<String> it = nodeDao.getChildrenIds(parentId).iterator();
			// Add each child
			while(it.hasNext()){
				String childId = it.next();
				addChildrenToChange(currentBenefactorId, childId, toChange);
			}
		}
	}

	/**
	 * Get the benefactor of a node.
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String getBenefactor(String nodeId) throws NotFoundException, DatastoreException {
		return nodeInheritanceDao.getBenefactor(nodeId);
	}
	
	/**
	 * Add a beneficiary to a node
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException, DatastoreException {
		nodeInheritanceDao.addBeneficiary(beneficiaryId, toBenefactorId);
	}

}
