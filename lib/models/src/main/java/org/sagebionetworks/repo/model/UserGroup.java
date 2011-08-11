package org.sagebionetworks.repo.model;

import java.util.Date;

public class UserGroup implements Base{
	private String id;
	private String name;
	private Date creationDate;
	boolean isIndividual;
	private String uri;
	private String etag;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	/**
	 * @return the isIndividual
	 */
	public boolean isIndividual() {
		return isIndividual;
	}
	/**
	 * @param isIndividual the isIndividual to set
	 */
	public void setIndividual(boolean isIndividual) {
		this.isIndividual = isIndividual;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {return getName();}
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	
	/**
	 * Is the passed name an email address
	 * @param name
	 * @return
	 */
	public static boolean isEmailAddress(String name){
		if(name == null)throw new IllegalArgumentException("Name cannot be null");
		int index = name.indexOf("@");
		return index > 0;
	}
	
	/**
	 * Is the passed UserGroup valid?
	 * @param userGroup
	 */
	public static void validate(UserGroup userGroup){
		if(userGroup == null) throw new IllegalArgumentException("UserGroup cannot be null");
		if(userGroup.getId() == null) throw new IllegalArgumentException("UserGroup.id cannot be null");
		if(userGroup.getName() == null) throw new IllegalArgumentException("UserGrup.name cannot be null");
		// Only an individual can have an email address for a name
		if(isEmailAddress(userGroup.getName())){
			if(!userGroup.isIndividual()) throw new IllegalArgumentException("Invalid group name: "+userGroup.getName()+", group names cannot be email addresses");
		}else{
			if(userGroup.isIndividual()) throw new IllegalArgumentException("Invalid user name: "+userGroup.getName()+", user names must be email addresses");
		}
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (isIndividual ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		UserGroup other = (UserGroup) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isIndividual != other.isIndividual)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
