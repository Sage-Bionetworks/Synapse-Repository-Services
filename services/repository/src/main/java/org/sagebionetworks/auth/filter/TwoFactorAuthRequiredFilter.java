package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.web.TwoFactorAuthEnabledRequiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that checks that two factor authentication is enabled for any user performing requests against the APIs. The check is always performed for admin users, while it is under a feature flag for other users.
 */
@Component("twoFactorAuthRequiredFilter")
public class TwoFactorAuthRequiredFilter extends OncePerRequestFilter {

	private GroupMembersDAO groupMemberDao;
	private AuthenticationDAO authDao;
	private FeatureManager featureManager;
	
	
	public TwoFactorAuthRequiredFilter(GroupMembersDAO groupMemberDao, AuthenticationDAO authDao, FeatureManager featureManager) {
		this.groupMemberDao = groupMemberDao;
		this.authDao = authDao;
		this.featureManager = featureManager;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws ServletException, IOException {
		// The Authentication filter will always inject this parameter with the current user id
		String userIdParam = httpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Long userId = Long.parseLong(userIdParam);
		
		// The anonymous user is not a conventional user and cannot enable 2fa
		// we check to see if the given user is the anonymous user in any realm
		if (HttpAuthUtil.isAnonymous(httpRequest)) {
			filterChain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		// By default having two FA enabled WON'T be required for all users and will be turned on as necessary, the check is always performed for admin users (See https://sagebionetworks.jira.com/browse/PLFM-8839)
		if (featureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT) && !groupMemberDao.areMemberOf(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), Set.of(userIdParam))) {
			filterChain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		if (!authDao.isTwoFactorAuthEnabled(userId)) {
			throw new TwoFactorAuthEnabledRequiredException();
		}

		filterChain.doFilter(httpRequest, httpResponse);
		
	}

}
