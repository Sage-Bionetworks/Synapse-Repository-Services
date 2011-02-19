package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable = "false")
public class GAEJDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	@Persistent
	private GAEJDOUserGroup owner;
	
	@Persistent
	private Key resource;
		
	// e.g. read, change, share
	@Persistent
	private String accessType;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public GAEJDOUserGroup getOwner() {
		return owner;
	}

	public void setOwner(GAEJDOUserGroup owner) {
		this.owner = owner;
	}

	public Key getResource() {
		return resource;
	}

	public void setResource(Key resource) {
		this.resource = resource;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}
	
	
}
