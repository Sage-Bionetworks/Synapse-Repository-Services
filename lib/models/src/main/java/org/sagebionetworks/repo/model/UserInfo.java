package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *  Contains both a user and the groups to which she belongs.
 */
public class UserInfo {

	// ALL the groups the user belongs to, except "Public",
	// which everyone implicitly belongs to, and "Administrators",
	// which is encoded in the 'isAdmin' field
	private Set<Long> groups;
	
	private final boolean isAdmin;
	private Long id;
	private Date creationDate;
	private boolean acceptsTermsOfUse;

	public UserInfo(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
	@Deprecated
	public UserInfo(boolean isAdmin, String id){
		this(isAdmin, Long.parseLong(id));
	}
	
	/**
	 * Helper to create a UserInfo
	 * @param isAdmin
	 * @param id
	 */
	public UserInfo(boolean isAdmin, Long id){
		this.isAdmin = isAdmin;
		this.id = id;
		this.groups = new LinkedHashSet<Long>();
		this.groups.add(this.id);
	}

	public Set<Long> getGroups() {
		return groups;
	}

	public void setGroups(Set<Long> groups) {
		this.groups = groups;
	}

	/**
	 * Is the passed userInfo object valid?
	 */
	public static void validateUserInfo(UserInfo info) throws UserNotFoundException {

		if (info == null) throw new IllegalArgumentException("UserInfo cannot be null");
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * @return the acceptsTermsOfUse
	 */
	public boolean acceptsTermsOfUse() {
		return acceptsTermsOfUse;
	}

	/**
	 * @param acceptsTermsOfUse the acceptsTermsOfUse to set
	 */
	public void setAcceptsTermsOfUse(boolean acceptsTermsOfUse) {
		this.acceptsTermsOfUse = acceptsTermsOfUse;
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
		if (getClass() != obj.getClass())
			return false;
		UserInfo other = (UserInfo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
}
