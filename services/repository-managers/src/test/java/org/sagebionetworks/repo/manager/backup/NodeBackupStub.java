package org.sagebionetworks.repo.manager.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a stub implementation of a Node backup source and destination for testing.
 * @author jmhill
 *
 */
public class NodeBackupStub implements NodeBackupManager {
	
	private TreeNodeBackup root;
	private Map<String, TreeNodeBackup> nodeIdMap = new HashMap<String, TreeNodeBackup>();
	private Map<String, NodeRevisionBackup> revisionIdMap = new HashMap<String, NodeRevisionBackup>();
	private long nodeIdSequence;
	private boolean wasCleared = false;
	
	public NodeBackupStub(TreeNodeBackup root){
		this.root = root;
		// Start the Id sequence at zero.
		nodeIdSequence = 0;
		init(root);
	}
	
	public NodeBackupStub(TreeNodeBackup root, int sequenceStart){
		this.root = root;
		nodeIdSequence = sequenceStart;
		init(root);
	}

	private void init(TreeNodeBackup root) {
		this.root.getNode().setParentId(null);
		// Build up a map of each node
		recursiveBuildMap(root, null);
	}
	
	public NodeBackupStub() {
	}

	/**
	 * Recursively populate the maps and ids.
	 * @param nodeNode
	 */
	private void recursiveBuildMap(TreeNodeBackup nodeNode, String previousBenefactor){
		if(nodeNode == null) throw new IllegalArgumentException("Node cannot be null");
		Long nodeId = new Long(nodeIdSequence++);
		Node node = nodeNode.getNode();
		node.setId(nodeId.toString());
		if(nodeNode.getAcl() != null){
			previousBenefactor = nodeId.toString();
			nodeNode.setBenefactor(nodeId.toString());
		}else{
			nodeNode.setBenefactor(previousBenefactor);
		}
		nodeIdMap.put(nodeId.toString(), nodeNode);
		// Map its revisions
		long revNumbers = 0;
		for(NodeRevisionBackup rev: nodeNode.getRevisions()){
			rev.setNodeId(nodeId.toString());
			Long revId = new Long(revNumbers++);
			rev.setRevisionNumber(revId);
			String revKey = createKeyForLongs(nodeId.toString(), revId);
			revisionIdMap.put(revKey, rev);
		}
		// Map the children
		for(TreeNodeBackup child: nodeNode.getChildren()){
			child.getNode().setParentId(nodeId.toString());
			recursiveBuildMap(child, previousBenefactor);
		}
	}
	
	/**
	 * Create a key from two longs.
	 * @param nodId
	 * @param revId
	 * @return
	 */
	private static String createKeyForLongs(String nodId, Long revId){
		if(nodId == null) throw new IllegalArgumentException("Node Id cannot be null");
		if(revId == null) throw new IllegalArgumentException("Revisoin ID cannot be null");
		return nodId.toString()+"+"+revId.toString();
	}
	

	@Override
	public NodeBackup getRoot() throws NotFoundException {
		if(root == null) throw new NotFoundException();
		return createBackupForNode(root);
	}

	@Override
	public NodeBackup getNode(String id) {
		TreeNodeBackup nn = getNodeNode(id);
		return createBackupForNode(nn);
	}
	
	private NodeBackup createBackupForNode(TreeNodeBackup node){
		if(node == null) throw new IllegalArgumentException("TreeNode cannot be null");
		NodeBackup backup = new NodeBackup();
		backup.setNode(node.getNode());
		backup.setAcl(node.getAcl());
		backup.setBenefactor(node.getBenefactor());
		String nodeId = node.getNode().getId();
		backup.setChildren(getNodeChildrenIds(nodeId));
		backup.setRevisions(getNodeRevisionIds(nodeId));
		return backup;
	}
	
	private TreeNodeBackup createTreeNodeForBackup(NodeBackup backup){
		if(backup == null) throw new IllegalArgumentException("Node cannot be null");
		TreeNodeBackup node = new TreeNodeBackup();
		node.setNode(backup.getNode());
		node.setAcl(backup.getAcl());
		node.setBenefactor(backup.getBenefactor());
		// Find this node's parent
		if(backup.getNode().getParentId() != null){
			TreeNodeBackup parent = getNodeNode(backup.getNode().getParentId());
			parent.getChildren().add(node);
		}
		nodeIdMap.put(backup.getNode().getId(), node);
		return node;
	}

	private TreeNodeBackup getNodeNode(String id) {
		TreeNodeBackup nn =  nodeIdMap.get(id);
		if(nn == null) throw new IllegalArgumentException("Node not found for ID: "+id);
		return nn;
	}

	public List<String> getNodeChildrenIds(String id) {
		TreeNodeBackup nn = getNodeNode(id);
		// Build up the list of children
		List<String> childrenIds = new ArrayList<String>();
		for(TreeNodeBackup child: nn.getChildren()){
			childrenIds.add(child.getNode().getId());
		}
		return childrenIds;
	}

	public String getNodeBenefactor(String id) {
		TreeNodeBackup nn = getNodeNode(id);
		return nn.getBenefactor();
	}

	public List<Long> getNodeRevisionIds(String id) {
		TreeNodeBackup nn = getNodeNode(id);
		// Build up the list of children
		List<Long> rervisions = new ArrayList<Long>();
		for(NodeRevisionBackup rev: nn.getRevisions()){
			rervisions.add(rev.getRevisionNumber());
		}
		return rervisions;
	}

	public AccessControlList getNodeACL(String id) {
		TreeNodeBackup nn = getNodeNode(id);
		return nn.getAcl();
	}

	@Override
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revId) {
		String revKey = createKeyForLongs(nodeId, revId);
		NodeRevisionBackup rev = revisionIdMap.get(revKey);
		if(rev != null){
			rev.setXmlVersion(NodeRevisionBackup.CURRENT_XML_VERSION);
		}
		return rev;
	}
	
	public void createOrUpdateNode(NodeBackup backup) {
		if(backup == null) throw new IllegalArgumentException("Backup cannot be null");
		TreeNodeBackup newNode = createTreeNodeForBackup(backup);
		// Is this the root?
		if(root == null){
			root = newNode;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeBackupStub other = (NodeBackupStub) obj;
		if (root == null) {
			if (other.root != null)
				return false;
		} else if (!root.equals(other.root))
			return false;
		return true;
	}

	@Override
	public long getTotalNodeCount() {
		return nodeIdMap.size();
	}

	@Override
	public void clearAllData() {
		root = null;
		nodeIdMap.clear();
		revisionIdMap.clear();
		wasCleared = true;
	}
	
	/**
	 * Was this stub cleared.
	 * @return
	 */
	public boolean getWasCleared(){
		return wasCleared;
	}

	@Override
	public String toString() {
		return "NodeBackupStub [root=" + root + ", nodeIdMap=" + nodeIdMap
				+ ", revisionIdMap=" + revisionIdMap + ", nodeIdSequence="
				+ nodeIdSequence + "]";
	}

	@Override
	public String getRootId() throws DatastoreException, NotFoundException {
		return getRoot().getNode().getId();
	}

	@Override
	public boolean doesNodeExist(String nodeId, String etag) {
		// TODO Auto-generated method stub
		return false;
	}
}
