package org.sagebionetworks.repo.model.jdo;

import java.util.Date;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



@PersistenceCapable(detachable = "false")
public class JDOUserGroup implements JDOBase {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;
	
	@Persistent
	private String name;
	
	@Persistent
	private Date creationDate;

	@Persistent
	private Set<Long> users;
	

    @Persistent(mappedBy = "owner")
	@Element(dependent = "true")
	private Set<JDOResourceAccess> resourceAccess;
	
	// the types of objects that the group can create, 
	// as represented by JDOxxxxx.class.getName()
	@Persistent
	private Set<String> creatableTypes;
	
	// true for system generated groups like 'Public'
	@Persistent
	private Boolean isSystemGroup = false;
	
	// true for groups established for individuals (in which case group 'name'==userId)
	@Persistent
	private Boolean isIndividual;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {return getName();}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Set<Long> getUsers() {
		return users;
	}

	public void setUsers(Set<Long> users) {
		this.users = users;
	}

	public Set<JDOResourceAccess> getResourceAccess() {
		return resourceAccess;
	}

	public void setResourceAccess(Set<JDOResourceAccess> resourceAccess) {
		this.resourceAccess = resourceAccess;
	}

	public Set<String> getCreatableTypes() {
		return creatableTypes;
	}

	public void setCreatableTypes(Set<String> creatableTypes) {
		this.creatableTypes = creatableTypes;
	}

	public Boolean getIsSystemGroup() {
		return isSystemGroup;
	}

	public void setIsSystemGroup(Boolean isSystemGroup) {
		this.isSystemGroup = isSystemGroup;
	}

	public Boolean getIsIndividual() {
		return isIndividual;
	}

	public void setIsIndividual(Boolean isIndividual) {
		this.isIndividual = isIndividual;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof JDOUserGroup))
			return false;
		JDOUserGroup other = (JDOUserGroup) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
}
