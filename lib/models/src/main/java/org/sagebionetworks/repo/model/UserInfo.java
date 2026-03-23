package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
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
	private boolean isCertified;
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
	public void setCertified(boolean certified) {
		this.isCertified = certified;
	}
	public boolean isCertified() {
		return isCertified;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserInfo userInfo = (UserInfo) o;
		return isAdmin == userInfo.isAdmin && isCertified == userInfo.isCertified &&
				hasTwoFactorAuthEnabled == userInfo.hasTwoFactorAuthEnabled && Objects.equals(groups, userInfo.groups)
				&& Objects.equals(id, userInfo.id) && Objects.equals(realmId, userInfo.realmId)
				&& Objects.equals(creationDate, userInfo.creationDate) && Objects.equals(context, userInfo.context)
				&& Objects.equals(realmAnonymousUserId, userInfo.realmAnonymousUserId)
				&& Objects.equals(realmAuthenticatedUsersId, userInfo.realmAuthenticatedUsersId)
				&& Objects.equals(realmPublicUsersId, userInfo.realmPublicUsersId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groups, isAdmin, isCertified, id, realmId, creationDate, hasTwoFactorAuthEnabled, context,
				realmAnonymousUserId, realmAuthenticatedUsersId, realmPublicUsersId);
	}


	@Override
	public String toString() {
		return "UserInfo [groups=" + groups + ", isAdmin=" + isAdmin + ", isCertified=" + isCertified + ", id=" + id + ", realmId=" + realmId
				+ ", creationDate=" + creationDate + ", hasTwoFactorAuthEnabled=" + hasTwoFactorAuthEnabled
				+ ", context=" + context + ", realmAnonymousUserId=" + realmAnonymousUserId
				+ ", realmAuthenticatedUsersId=" + realmAuthenticatedUsersId + ", realmPublicUsersId="
				+ realmPublicUsersId + "]";
	}

}
