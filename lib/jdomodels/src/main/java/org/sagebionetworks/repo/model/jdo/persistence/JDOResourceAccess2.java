package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_RESOURCE_ACCESS)
public class JDOResourceAccess2 {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	@Column (name=SqlConstants.COL_RESOURCE_ACCESS_OWNER)
	private JDOAccessControlList owner;
	
	@Persistent
	@Column(name=SqlConstants.COL_USER_GROUP_ID)
//	@ForeignKey(name="RESOURCE_ACCESS_USER_GROUP_FK", deleteAction=ForeignKeyAction.NONE)
	private long userGroupId;
				
	// e.g. read, write, delete, as defined in AuthorizationConstants.ACCESS_TYPE
	@Persistent(serialized="false")
	@Join(table=SqlConstants.TABLE_RESOURCE_ACCESS_TYPE, column=SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID)
	private Set<String> accessType = new HashSet<String>();

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
	 * @return the userGroupId
	 */
	public long getUserGroupId() {
		return userGroupId;
	}

	/**
	 * @param userGroupId the userGroupId to set
	 */
	public void setUserGroupId(long userGroupId) {
		this.userGroupId = userGroupId;
	}

	/**
	 * @return the accessType
	 */
	public Set<String> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType the accessType to set
	 */
	public void setAccessType(Set<String> accessType) {
		this.accessType = accessType;
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
		if (!(obj instanceof JDOResourceAccess2))
			return false;
		JDOResourceAccess2 other = (JDOResourceAccess2) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}


}
