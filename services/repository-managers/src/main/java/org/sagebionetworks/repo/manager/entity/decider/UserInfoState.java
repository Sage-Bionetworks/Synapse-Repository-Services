package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Captures state about a user to be used for a single access check. Since the
 * state can be changed this object should not be reused outside of the context
 * in which it is used.
 *
 */
public class UserInfoState {

	private boolean isAdmin;
	private boolean isCertifiedUser;
	private boolean acceptsTermsOfUse;
	private boolean isUserAnonymous;
	private UserInfo userInfo;

	public UserInfoState(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		this.isAdmin = userInfo.isAdmin();
		this.acceptsTermsOfUse = userInfo.acceptsTermsOfUse();
		this.isCertifiedUser = AuthorizationUtils.isCertifiedUser(userInfo);
		this.isUserAnonymous = AuthorizationUtils.isUserAnonymous(userInfo);
		this.userInfo = userInfo;
	}

	/**
	 * Is the user an administrator?
	 */
	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * Is the user certified?
	 */
	public boolean isCertifiedUser() {
		return isCertifiedUser;
	}

	/**
	 * Has the user accepted the terms of use?
	 */
	public boolean acceptsTermsOfUse() {
		return acceptsTermsOfUse;
	}

	/**
	 * Is the user anonymous?
	 */
	public boolean isUserAnonymous() {
		return isUserAnonymous;
	}

	/**
	 * Override the actual user certification state in order to answer questions
	 * such as: 'Would the user be allowed to perform this action if they were
	 * certified?'
	 * 
	 * @param isCertified
	 */
	public UserInfoState overrideIsCertified(boolean isCertified) {
		this.isCertifiedUser = isCertified;
		return this;
	}
	
	public UserInfoState overrideAcceptsTermsOfUse(boolean acceptsTermsOfUse) {
		this.acceptsTermsOfUse = acceptsTermsOfUse;
		return this;
	}
	
	/**
	 * Get the actual unmodified UserInfo.
	 * @return
	 */
	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public int hashCode() {
		return Objects.hash(acceptsTermsOfUse, isAdmin, isCertifiedUser, isUserAnonymous, userInfo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserInfoState)) {
			return false;
		}
		UserInfoState other = (UserInfoState) obj;
		return acceptsTermsOfUse == other.acceptsTermsOfUse && isAdmin == other.isAdmin
				&& isCertifiedUser == other.isCertifiedUser && isUserAnonymous == other.isUserAnonymous
				&& Objects.equals(userInfo, other.userInfo);
	}

	@Override
	public String toString() {
		return "UserInfoState [isAdmin=" + isAdmin + ", isCertifiedUser=" + isCertifiedUser + ", acceptsTermsOfUse="
				+ acceptsTermsOfUse + ", isUserAnonymous=" + isUserAnonymous + ", userInfo=" + userInfo + "]";
	}

}
