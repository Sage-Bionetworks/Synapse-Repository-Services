package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_REVISION, objectIdClass=RevisionId.class)
@Unique(name="UNIQUE_REVISION_LABEL", members={"owner", "label"})
public class JDORevision {
		
	@PrimaryKey
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	@Column(name=SqlConstants.COL_REVISION_OWNER_NODE)
	@ForeignKey(name="REVISION_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode owner;
	
	@PrimaryKey
	@Column(name=SqlConstants.COL_REVISION_NUMBER)
	private Long revisionNumber;
	
	@Column(name=SqlConstants.COL_REVISION_LABEL)
    @Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String label;
	
	@Column(name=SqlConstants.COL_REVISION_COMMENT)
	@Persistent
	private String comment;
	
	@Column(name=SqlConstants.COL_NODE_MODIFIED_BY)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String modifiedBy;
	
	@Column(name=SqlConstants.COL_NODE_MODIFIED_ON)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long modifiedOn;
	
	@Column(name=SqlConstants.COL_REVISION_ANNOS_BLOB)
	@Persistent
	private byte[] annotations;

	@Column(name=SqlConstants.COL_REVISION_REFS_BLOB)
	@Persistent
	private byte[] references;

	public JDONode getOwner() {
		return owner;
	}

	public void setOwner(JDONode owner) {
		this.owner = owner;
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

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public byte[] getAnnotations() {
		return annotations;
	}

	public void setAnnotations(byte[] annotations) {
		this.annotations = annotations;
	}

	/**
	 * @return the references
	 */
	public byte[] getReferences() {
		return references;
	}

	/**
	 * @param references the references to set
	 */
	public void setReferences(byte[] references) {
		this.references = references;
	}

	public Long getRevisionNumber() {
		return revisionNumber;
	}

	public void setRevisionNumber(Long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		JDORevision other = (JDORevision) obj;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		return true;
	}

}
