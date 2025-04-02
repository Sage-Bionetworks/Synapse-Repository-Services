package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.web.TwoFactorAuthEnabledRequiredException;

@ExtendWith(MockitoExtension.class)
public class TwoFactorAuthRequiredFilterTest {

	@Mock
	private GroupMembersDAO mockGroupMemberDao;
	
	@Mock
	private AuthenticationDAO mockAuthDao;
	
	@Mock
	private FeatureManager mockFeatureManager;
	
	@InjectMocks
	private TwoFactorAuthRequiredFilter filter;
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Test
	public void testDoFilterWithAnonymousUser() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
				
		// Call under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verifyZeroInteractions(mockGroupMemberDao, mockAuthDao, mockFeatureManager);
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}
	
	@Test
	public void testDoFilterWithFeatureDisabledAndNotAdmin() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT)).thenReturn(true);
		when(mockGroupMemberDao.areMemberOf(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), Set.of("123"))).thenReturn(false);
		
		// Call under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verifyZeroInteractions(mockAuthDao);
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}
	
	@Test
	public void testDoFilterWithFeatureDisabledAndIsAdminAndTwoFaEnabled() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT)).thenReturn(true);
		when(mockGroupMemberDao.areMemberOf(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), Set.of("123"))).thenReturn(true);
		when(mockAuthDao.isTwoFactorAuthEnabled(123L)).thenReturn(true);
		
		// Call under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);		
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}
	
	@Test
	public void testDoFilterWithFeatureDisabledAndIsAdminAndTwoFaDisabled() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT)).thenReturn(true);
		when(mockGroupMemberDao.areMemberOf(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), Set.of("123"))).thenReturn(true);
		when(mockAuthDao.isTwoFactorAuthEnabled(123L)).thenReturn(false);
		
		assertThrows(TwoFactorAuthEnabledRequiredException.class, () -> {
			// Call under test
			filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		});
		
		verifyNoMoreInteractions(mockFilterChain);
	}
	
	@Test
	public void testDoFilterWithFeatureEnabledAndTwoFaEnabled() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT)).thenReturn(false);
		when(mockAuthDao.isTwoFactorAuthEnabled(123L)).thenReturn(true);
		
		// Call under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verifyZeroInteractions(mockGroupMemberDao);
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}

	@Test
	public void testDoFilterWithFeatureEnabledAndTwoFaDisabled() throws Exception {
		
		when(mockHttpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_2FA_REQUIREMENT)).thenReturn(false);
		when(mockAuthDao.isTwoFactorAuthEnabled(123L)).thenReturn(false);
		
		assertThrows(TwoFactorAuthEnabledRequiredException.class, () -> {
			// Call under test
			filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		});
		
		verifyNoMoreInteractions(mockFilterChain, mockGroupMemberDao);
	}
}
