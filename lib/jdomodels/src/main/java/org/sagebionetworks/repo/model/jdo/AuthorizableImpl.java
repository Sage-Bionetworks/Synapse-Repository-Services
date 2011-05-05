package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.Authorizable;

public class AuthorizableImpl implements Authorizable {
	private String id;
	private String type;
	
	public AuthorizableImpl() {
	}
	
	public AuthorizableImpl(String id, String type) {
		setId(id);
		setType(type);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	
}
