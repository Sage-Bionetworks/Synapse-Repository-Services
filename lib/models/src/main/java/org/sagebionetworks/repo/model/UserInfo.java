package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
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
	private boolean hasTwoFactorAuthEnabled;
	private String sessionId;

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
	
	public boolean hasTwoFactorAuthEnabled() {
		return hasTwoFactorAuthEnabled;
	}
	
	public void setTwoFactorAuthEnabled(boolean isTwoFactorAuthEnabled) {
		this.hasTwoFactorAuthEnabled = isTwoFactorAuthEnabled;
	}

	/**
	 * The session ID of the access record associated with the current request.
	 * @return
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * The session ID of the access record associated with the current request.
	 * @param sessionId
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(acceptsTermsOfUse, creationDate, groups, hasTwoFactorAuthEnabled, id, isAdmin, sessionId);
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
		return acceptsTermsOfUse == other.acceptsTermsOfUse && Objects.equals(creationDate, other.creationDate)
				&& Objects.equals(groups, other.groups) && hasTwoFactorAuthEnabled == other.hasTwoFactorAuthEnabled
				&& Objects.equals(id, other.id) && isAdmin == other.isAdmin
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "UserInfo [groups=" + groups + ", isAdmin=" + isAdmin + ", id=" + id + ", creationDate=" + creationDate
				+ ", acceptsTermsOfUse=" + acceptsTermsOfUse + ", hasTwoFactorAuthEnabled=" + hasTwoFactorAuthEnabled
				+ ", sessionId=" + sessionId + "]";
	}
	
}
