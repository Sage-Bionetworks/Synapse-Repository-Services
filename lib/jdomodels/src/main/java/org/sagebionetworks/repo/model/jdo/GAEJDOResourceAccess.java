package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



@PersistenceCapable(detachable = "false")
public class GAEJDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	private GAEJDOUserGroup owner;
	
	@Persistent
	private Long resource;
		
	// e.g. read, change, share
	@Persistent
	private String accessType;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GAEJDOUserGroup getOwner() {
		return owner;
	}

	public void setOwner(GAEJDOUserGroup owner) {
		this.owner = owner;
	}

	public Long getResource() {
		return resource;
	}

	public void setResource(Long resource) {
		this.resource = resource;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}
	
	
}
