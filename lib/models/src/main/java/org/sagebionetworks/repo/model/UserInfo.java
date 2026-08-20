package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.util.ValidateArgument;

/**
 *  Contains both a user and the groups to which she belongs.
 */
public class UserInfo {

	// ALL the groups the user belongs to, except "Public",
	// which everyone implicitly belongs to, and "Administrators",
	// which is encoded in the 'isAdmin' field
	private final Set<Long> groups;

	private final boolean isAdmin;
	private final Long id;
	private final String realmId;
	private boolean isCertified;
	private Date creationDate;
	private boolean hasTwoFactorAuthEnabled;
	private CallersContext context;
	private Long realmAnonymousUserId;
	private Long realmAuthenticatedUsersId;
	private Long realmPublicUsersId;

	/**
	 * Creates a UserInfo whose group membership defaults to the user's own principal id.
	 *
	 * @param isAdmin whether the user is an administrator
	 * @param id      the user's principal id, must not be null
	 * @param realmId the id of the realm the user belongs to
	 */
	public UserInfo(boolean isAdmin, Long id, String realmId) {
		this(isAdmin, id, realmId, defaultGroups(id));
	}

	/**
	 * Creates a UserInfo with an explicit set of group memberships.
	 *
	 * @param isAdmin whether the user is an administrator
	 * @param id      the user's principal id, must not be null
	 * @param realmId the id of the realm the user belongs to
	 * @param groups  all the groups the user belongs to
	 */
	public UserInfo(boolean isAdmin, Long id, String realmId, Set<Long> groups) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(realmId, "realmId");
		ValidateArgument.required(groups, "groups");
		this.isAdmin = isAdmin;
		this.id = id;
		this.realmId = realmId;
		this.groups = groups;
	}

	private static Set<Long> defaultGroups(Long id) {
		Set<Long> groups = new LinkedHashSet<Long>();
		groups.add(id);
		return groups;
	}

	public boolean isUserAnonymous() {
		return id.equals(realmAnonymousUserId);
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

	/**
	 * Is the passed userInfo object valid?
	 */
	public static void validateUserInfo(UserInfo info) throws UserNotFoundException {

		if (info == null) throw new IllegalArgumentException("UserInfo cannot be null");
	}

	public String getRealmId() {
		return realmId;
	}

	public Long getId() {
		return id;
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
