package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Contains the data that makes up a node's backup.
 * @author jmhill
 *
 */
public class NodeBackup {
	
	private Node node;
	private AccessControlList acl;
	private String benefactor;
	private List<Long> revisions = new ArrayList<Long>();
	private List<String> children = new ArrayList<String>();
	
	public NodeBackup(){
	}
	
	public NodeBackup(Node node, AccessControlList acl, String benefactor,
			List<Long> revisions, List<String> children) {
		super();
		this.node = node;
		this.acl = acl;
		this.benefactor = benefactor;
		this.revisions = revisions;
		this.children = children;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public AccessControlList getAcl() {
		return acl;
	}
	public void setAcl(AccessControlList acl) {
		this.acl = acl;
	}
	public String getBenefactor() {
		return benefactor;
	}
	public void setBenefactor(String benefactor) {
		this.benefactor = benefactor;
	}
	public List<Long> getRevisions() {
		return revisions;
	}
	public void setRevisions(List<Long> revisions) {
		this.revisions = revisions;
	}
	public List<String> getChildren() {
		return children;
	}
	public void setChildren(List<String> children) {
		this.children = children;
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
		NodeBackup other = (NodeBackup) obj;
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

	@Override
	public String toString() {
		return "NodeBackup [node=" + node + ", acl=" + acl + ", benefactor="
				+ benefactor + ", revisions=" + revisions + ", children="
				+ children + "]";
	}

}
