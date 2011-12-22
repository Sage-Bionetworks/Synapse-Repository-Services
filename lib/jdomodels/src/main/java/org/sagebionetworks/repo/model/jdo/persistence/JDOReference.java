package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * @author deflaux
 * 
 * Note: Cacheable MUST BE "false".  See: PLFM-852.
 * 
 */
@PersistenceCapable(detachable = "true", table = SqlConstants.TABLE_REFERENCE, objectIdClass = ReferenceId.class, cacheable="false")
public class JDOReference {

	@PrimaryKey
	@Persistent(nullValue = NullValue.EXCEPTION)
	// cannot be null
	@Column(name = SqlConstants.COL_REFERENCE_OWNER_NODE)
	// If the node that holds (owns) this reference is deleted, this reference
	// should be deleted too because it is no longer of value
	@ForeignKey(name = "REFERENCE_OWNER_FK", deleteAction = ForeignKeyAction.CASCADE)
	private JDONode owner;

	@PrimaryKey
	@Persistent(nullValue = NullValue.EXCEPTION)
	// cannot be null
	@Column(name = SqlConstants.COL_REFERENCE_TARGET_NODE)
	// This reference should not be deleted if the target of the reference is
	// deleted, we have made a design decision to _NOT_ enforce referential
	// integrity. Therefore store the id, not the JDONode here to avoid the
	// creation of a foreign key constraint.
	private Long targetId;

	@PrimaryKey
	@Persistent(nullValue = NullValue.EXCEPTION)
	// cannot be null
	@Column(name = SqlConstants.COL_REFERENCE_TARGET_REVISION_NUMBER)
	private Long targetRevision;

	@PrimaryKey
	@Persistent(nullValue = NullValue.EXCEPTION)
	// cannot be null
	@Column(name = SqlConstants.COL_REFERENCE_GROUP_NAME)
	private String groupName;

	/**
	 * @return the node holding the reference
	 */
	public JDONode getOwner() {
		return owner;
	}

	/**
	 * Set the node holding the reference
	 * 
	 * @param owner
	 */
	public void setOwner(JDONode owner) {
		this.owner = owner;
	}

	/**
	 * @return the id of the node to which this reference refers
	 */
	public Long getTargetId() {
		return targetId;
	}

	/**
	 * Set the id of the node to which this reference refers
	 * 
	 * @param targetId
	 */
	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	/**
	 * @return the revision of the node to which this reference refers
	 */
	public Long getTargetRevision() {
		return targetRevision;
	}

	/**
	 * Set the revision of the node to which this reference refers
	 * 
	 * @param targetRevision
	 */
	public void setTargetRevision(Long targetRevision) {
		this.targetRevision = targetRevision;
	}

	/**
	 * @return the reference groupName
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @param groupName
	 *            the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result
				+ ((targetId == null) ? 0 : targetId.hashCode());
		result = prime * result
				+ ((targetRevision == null) ? 0 : targetRevision.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JDOReference other = (JDOReference) obj;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		if (targetRevision == null) {
			if (other.targetRevision != null)
				return false;
		} else if (!targetRevision.equals(other.targetRevision))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JDOReference [groupName=" + groupName + ", owner=" + owner
				+ ", targetId=" + targetId + ", targetRevision="
				+ targetRevision + "]";
	}

	

}
