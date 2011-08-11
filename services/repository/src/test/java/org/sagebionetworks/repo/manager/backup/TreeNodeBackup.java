package org.sagebionetworks.repo.manager.backup;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevision;

/**
 * A tree node version of a NodeBackup.  Allows for a tree of nodes to be built.
 * @author John
 *
 */
public class TreeNodeBackup {
	private Node node;
	private List<TreeNodeBackup> children = new ArrayList<TreeNodeBackup>();
	private String benefactor;
	private AccessControlList acl;
	private List<NodeRevision> revisions = new ArrayList<NodeRevision>();
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public List<TreeNodeBackup> getChildren() {
		return children;
	}
	public void setChildren(List<TreeNodeBackup> children) {
		this.children = children;
	}
	public String getBenefactor() {
		return benefactor;
	}
	public void setBenefactor(String benefactor) {
		this.benefactor = benefactor;
	}
	public AccessControlList getAcl() {
		return acl;
	}
	public void setAcl(AccessControlList acl) {
		this.acl = acl;
	}
	public List<NodeRevision> getRevisions() {
		return revisions;
	}
	public void setRevisions(List<NodeRevision> revisions) {
		this.revisions = revisions;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acl == null) ? 0 : acl.hashCode());
		result = prime * result
				+ ((benefactor == null) ? 0 : benefactor.hashCode());
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result
				+ ((revisions == null) ? 0 : revisions.hashCode());
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
		TreeNodeBackup other = (TreeNodeBackup) obj;
		if (acl == null) {
			if (other.acl != null)
				return false;
		} else if (!acl.equals(other.acl))
			return false;
		if (benefactor == null) {
			if (other.benefactor != null)
				return false;
		} else if (!benefactor.equals(other.benefactor))
			return false;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (revisions == null) {
			if (other.revisions != null)
				return false;
		} else if (!revisions.equals(other.revisions))
			return false;
		return true;
	}
	
}