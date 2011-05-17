package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;


@PersistenceCapable(detachable = "false", table=SqlConstants.TABLE_RESOURCE_ACCESS)
public class JDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	@Column (name=SqlConstants.COL_RESOURCE_ACCESS_OWNER)
	private JDOUserGroup owner;
	
	@Persistent
	@Column (name=SqlConstants.COL_RESOURCE_ACCESS_TYPE)
	private String resourceType;
	
	@Persistent
	@Column (name=SqlConstants.COL_RESOURCE_ACCESS_RESOURCE_ID)
	private Long resourceId;
		
	// e.g. read, change, share
	@Persistent(serialized="false")
	@Join (table=SqlConstants.TABLE_RESOURCE_ACCESS_TYPE, column=SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID)
	private Set<String> accessType = new HashSet<String>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JDOUserGroup getOwner() {
		return owner;
	}

	public void setOwner(JDOUserGroup owner) {
		this.owner = owner;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
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

	/**
	 * @param accessType the accessType to set
	 */
	public void setAccessTypeByEnum(Set<AuthorizationConstants.ACCESS_TYPE> accessType) {
		Set<String> stringSet = new HashSet<String>();
		for (AuthorizationConstants.ACCESS_TYPE t : accessType) stringSet.add(t.name());
		this.accessType = stringSet;
	}
	
	public Set<AuthorizationConstants.ACCESS_TYPE> getAccessTypeAsEnum() {
		Set<AuthorizationConstants.ACCESS_TYPE> ans = new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		Set<String> strSet = getAccessType();
		for (String s : strSet) ans.add(AuthorizationConstants.ACCESS_TYPE.valueOf(s));
		return ans;
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
		if (!(obj instanceof JDOResourceAccess))
			return false;
		JDOResourceAccess other = (JDOResourceAccess) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	public String toString() {
		String typeAbbr = this.getResourceType();
		int i = typeAbbr.lastIndexOf(".");
		if (i>0 && i<typeAbbr.length()-1) typeAbbr = typeAbbr.substring(i+1);
		return "type="+this.getResourceType()+", rid="+this.getResourceId();
		}
}
