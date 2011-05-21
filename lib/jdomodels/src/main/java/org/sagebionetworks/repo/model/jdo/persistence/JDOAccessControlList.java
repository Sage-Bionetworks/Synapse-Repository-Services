package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.Date;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import org.sagebionetworks.repo.model.jdo.JDOBase;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_ACCESS_CONTROL_LIST)
public class JDOAccessControlList implements JDOBase {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	

	@Persistent
	@Unique
	@Column(name=SqlConstants.COL_ACL_OWNER_ID)
    @ForeignKey(name="ACL_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode resource;
	
    @Persistent(serialized="false", mappedBy = "owner")
    @Element(dependent = "true")
	private Set<JDOResourceAccess2> resourceAccess;

	@Column(name=SqlConstants.COL_NODE_ETAG)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long eTag = new Long(0);
	
	@Column(name=SqlConstants.COL_NODE_CREATED_BY)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String createdBy;
	
	@Column(name=SqlConstants.COL_NODE_CREATED_ON)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long creationDate;
	
	@Column(name=SqlConstants.COL_NODE_MODIFIED_BY)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String modifiedBy;
	
	@Column(name=SqlConstants.COL_NODE_MODIFIED_ON)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long modifiedOn;

	/**
	 * @return the resource
	 */
	public JDONode getResource() {
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(JDONode resource) {
		this.resource = resource;
	}

	/**
	 * @return the resourceAccess
	 */
	public Set<JDOResourceAccess2> getResourceAccess() {
		return resourceAccess;
	}

	/**
	 * @param resourceAccess the resourceAccess to set
	 */
	public void setResourceAccess(Set<JDOResourceAccess2> resourceAccess) {
		this.resourceAccess = resourceAccess;
	}

	/**
	 * @return the eTag
	 */
	public Long getEtag() {
		return eTag;
	}

	/**
	 * @param eTag the eTag to set
	 */
	public void setEtag(Long eTag) {
		this.eTag = eTag;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return new Date(creationDate);
	}

	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	/**
	 * @return the modifiedBy
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * @param modifiedBy the modifiedBy to set
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
	 * @return the modifiedOn
	 */
	public Long getModifiedOn() {
		return modifiedOn;
	}

	/**
	 * @param modifiedOn the modifiedOn to set
	 */
	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (!(obj instanceof JDOAccessControlList))
			return false;
		JDOAccessControlList other = (JDOAccessControlList) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
}
