package org.sagebionetworks.repo.manager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
	public void nodeParentChanged(String nodeId) {
		throw new IllegalArgumentException("This method is not yet supported");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setNodeToInheritFromItself(String nodeId) throws NotFoundException {
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
	public void setNodeToInheritFromNearestParent(String nodeId) throws NotFoundException {
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
	 */
	private void changeAllChildrenTo(String currentlyInheritingFromId, String parentId, String changeToInheritFromId) throws NotFoundException{
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
	 */
	private void addChildrenToChange(String currentBenefactorId, String parentId, Set<String> toChange) throws NotFoundException{
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
	 */
	@Override
	public String getBenefactor(String nodeId) throws NotFoundException {
		return nodeInheritanceDao.getBenefactor(nodeId);
	}

}
