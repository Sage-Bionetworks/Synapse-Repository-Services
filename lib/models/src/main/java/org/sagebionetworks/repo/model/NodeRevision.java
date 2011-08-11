package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * 
 * @author jmhill
 *
 */
public class NodeRevision {
	
	private String nodeId;
	private Long revisionNumber;
	private String label;
	private String comment;
	private String modifiedBy;
	private Date modifiedOn;
	private Annotations annotations;
	
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public Long getRevisionNumber() {
		return revisionNumber;
	}
	public void setRevisionNumber(Long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	public Date getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	public Annotations getAnnotations() {
		return annotations;
	}
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((revisionNumber == null) ? 0 : revisionNumber.hashCode());
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
		NodeRevision other = (NodeRevision) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "NodeRevision [nodeId=" + nodeId + ", revisionNumber="
				+ revisionNumber + ", label=" + label + ", comment=" + comment
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", annotations=" + annotations + "]";
	}
}
