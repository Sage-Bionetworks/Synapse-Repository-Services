package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.auth.CallersContext;

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
	private String realmId;
	private Date creationDate;
	private boolean hasTwoFactorAuthEnabled;
	private CallersContext context;
	private Long realmAnonymousUserId;
	private Long realmAuthenticatedUsersId;
	private Long realmPublicUsersId;

	// Note: this is only used in unit tests
	@Deprecated
	public UserInfo(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
	// Note: this is only used in unit tests
	@Deprecated
	public UserInfo(boolean isAdmin, Long id){
		this(isAdmin, id, null);
	}
	
	/**
	 * Helper to create a UserInfo
	 * @param isAdmin
	 * @param id
	 */
	public UserInfo(boolean isAdmin, Long id, String realmId){
		this.isAdmin = isAdmin;
		this.id = id;
		this.groups = new LinkedHashSet<Long>();
		this.groups.add(this.id);
		this.realmId=realmId;
	}
	
	public boolean isUserAnonymous() {
		return id==null || id.equals(realmAnonymousUserId);
	}

	public Long getRealmAnonymousUserId() {
		return realmAnonymousUserId;
	}

	public void setRealmAnonymousUserId(Long realmAnonymousUserId) {
		this.realmAnonymousUserId = realmAnonymousUserId;
	}

	public Long getRealmAuthenticatedUsersId() {
		return realmAuthenticatedUsersId;
	}

	public void setRealmAuthenticatedUsersId(Long realmAuthenticatedUsersId) {
		this.realmAuthenticatedUsersId = realmAuthenticatedUsersId;
	}

	public Long getRealmPublicUsersId() {
		return realmPublicUsersId;
	}

	public void setRealmPublicUsersId(Long realmPublicUsersId) {
		this.realmPublicUsersId = realmPublicUsersId;
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

	public String getRealmId() {
		return realmId;
	}

	public void setRealmId(String realmId) {
		this.realmId = realmId;
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
	
	public boolean hasTwoFactorAuthEnabled() {
		return hasTwoFactorAuthEnabled;
	}
	
	public void setTwoFactorAuthEnabled(boolean isTwoFactorAuthEnabled) {
		this.hasTwoFactorAuthEnabled = isTwoFactorAuthEnabled;
	}

	public CallersContext getContext() {
		return context;
	}

	public void setContext(CallersContext context) {
		this.context = context;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((groups == null) ? 0 : groups.hashCode());
		result = prime * result + (hasTwoFactorAuthEnabled ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (isAdmin ? 1231 : 1237);
		result = prime * result + ((realmAnonymousUserId == null) ? 0 : realmAnonymousUserId.hashCode());
		result = prime * result + ((realmAuthenticatedUsersId == null) ? 0 : realmAuthenticatedUsersId.hashCode());
		result = prime * result + ((realmId == null) ? 0 : realmId.hashCode());
		result = prime * result + ((realmPublicUsersId == null) ? 0 : realmPublicUsersId.hashCode());
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
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (groups == null) {
			if (other.groups != null)
				return false;
		} else if (!groups.equals(other.groups))
			return false;
		if (hasTwoFactorAuthEnabled != other.hasTwoFactorAuthEnabled)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isAdmin != other.isAdmin)
			return false;
		if (realmAnonymousUserId == null) {
			if (other.realmAnonymousUserId != null)
				return false;
		} else if (!realmAnonymousUserId.equals(other.realmAnonymousUserId))
			return false;
		if (realmAuthenticatedUsersId == null) {
			if (other.realmAuthenticatedUsersId != null)
				return false;
		} else if (!realmAuthenticatedUsersId.equals(other.realmAuthenticatedUsersId))
			return false;
		if (realmId == null) {
			if (other.realmId != null)
				return false;
		} else if (!realmId.equals(other.realmId))
			return false;
		if (realmPublicUsersId == null) {
			if (other.realmPublicUsersId != null)
				return false;
		} else if (!realmPublicUsersId.equals(other.realmPublicUsersId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UserInfo [groups=" + groups + ", isAdmin=" + isAdmin + ", id=" + id + ", realmId=" + realmId
				+ ", creationDate=" + creationDate + ", hasTwoFactorAuthEnabled=" + hasTwoFactorAuthEnabled
				+ ", context=" + context + ", realmAnonymousUserId=" + realmAnonymousUserId
				+ ", realmAuthenticatedUsersId=" + realmAuthenticatedUsersId + ", realmPublicUsersId="
				+ realmPublicUsersId + "]";
	}
	
}
