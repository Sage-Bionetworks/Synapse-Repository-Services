package org.sagebionetworks.repo.manager.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;

@ExtendWith(MockitoExtension.class)
public class FeatureManagerImplTest {

	@Mock
	private FeatureStatusDao mockFeatureStatusDao;
	
	@InjectMocks
	private FeatureManagerImpl manager;
	
	@Mock
	private UserInfo mockUser;
	
	@Test
	public void testIsFeatureEnabledWithNoFeature() {
		
		Feature feature = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.isFeatureEnabled(feature);
		}).getMessage();
		
		assertEquals("The feature is required.", message);
	}
	
	@Test
	public void testIsFeatureEnabled() {
		
		boolean enabled = true;
		
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(enabled));
		
		Feature feature = Feature.DATA_ACCESS_NOTIFICATIONS;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(enabled, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsFeatureEnabledWithDisabled() {
		
		boolean enabled = false;
		
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(enabled));
		
		Feature feature = Feature.DATA_ACCESS_NOTIFICATIONS;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(enabled, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsFeatureEnabledWithNoData() {
		
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.empty());
		
		Feature feature = Feature.DATA_ACCESS_NOTIFICATIONS;
		
		boolean expected = false;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(expected, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsUserInTestingGroupWithNoUser() {
		
		UserInfo user = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.isUserInTestingGroup(user);
		}).getMessage();

		assertEquals("The user is required.", message);
	}
	
	@Test
	public void testIsUserInTestingGroupWithNullGroup() {
		
		Set<Long> userGroups = null;
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		
		boolean expected = false;
		
		// Call under test
		boolean result =  manager.isUserInTestingGroup(mockUser);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
	}
	
	@Test
	public void testIsUserInTestingGroupWithNoGroup() {
		
		Set<Long> userGroups = Collections.emptySet();
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		
		boolean expected = false;
		
		// Call under test
		boolean result =  manager.isUserInTestingGroup(mockUser);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
	}
	
	@Test
	public void testIsUserInTestingGroupWithTestingGroup() {
		
		Set<Long> userGroups = Collections.singleton(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId());
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		
		boolean expected = true;
		
		// Call under test
		boolean result =  manager.isUserInTestingGroup(mockUser);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
	}
	
	@Test
	public void testGetFeatureStatus() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		
		boolean isFeatureEnabled = true;
		
		when(user.isAdmin()).thenReturn(true);
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(isFeatureEnabled));
		
		FeatureStatus expected = new FeatureStatus();
		expected.setFeature(feature);
		expected.setEnabled(isFeatureEnabled);
		
		// Call under test
		FeatureStatus result = manager.getFeatureStatus(user, feature);
		
		assertEquals(expected, result);
		
		verify(user).isAdmin();
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testGetFeatureStatusWithUnauthorized() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		
		when(user.isAdmin()).thenReturn(false);
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.getFeatureStatus(user, feature);
		}).getMessage();
		
		assertEquals("You must be an administrator to perform this operation.", errorMessage);

		verifyZeroInteractions(mockFeatureStatusDao);
	}

	@Test
	public void testGetFeatureStatusWithNoUser() {
		UserInfo user = null;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getFeatureStatus(user, feature);
		}).getMessage();
		
		assertEquals("The user is required.", errorMessage);

		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testGetFeatureStatusWithNoFeature() {
		UserInfo user = mockUser;
		Feature feature = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getFeatureStatus(user, feature);
		}).getMessage();
		
		assertEquals("The feature is required.", errorMessage);

		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testSetFeatureStatus() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		boolean isFeatureEnabled = true;
		
		when(user.isAdmin()).thenReturn(true);
		doNothing().when(mockFeatureStatusDao).setFeatureEnabled(any(), anyBoolean());
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(isFeatureEnabled));
		
		FeatureStatus status = new FeatureStatus();
		
		status.setFeature(feature);
		status.setEnabled(isFeatureEnabled);
		
		// Call under test
		FeatureStatus result = manager.setFeatureStatus(user, feature, status);
		
		assertEquals(status, result);
		
		verify(mockFeatureStatusDao).setFeatureEnabled(feature, isFeatureEnabled);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testSetFeatureStatusUnauthorized() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		boolean isFeatureEnabled = true;
		
		when(user.isAdmin()).thenReturn(false);
		
		FeatureStatus status = new FeatureStatus();
		
		status.setFeature(feature);
		status.setEnabled(isFeatureEnabled);
		
		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.setFeatureStatus(user, feature, status);
		});

		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testSetFeatureStatusWithNoUser() {
		UserInfo user = null;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		boolean isFeatureEnabled = true;
		
		FeatureStatus status = new FeatureStatus();
		
		status.setFeature(feature);
		status.setEnabled(isFeatureEnabled);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setFeatureStatus(user, feature, status);
		}).getMessage();
		
		assertEquals("The user is required.", errorMessage);

		
		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testSetFeatureStatusWithNoFeature() {
		UserInfo user = mockUser;
		Feature feature = null;
		boolean isFeatureEnabled = true;
		
		FeatureStatus status = new FeatureStatus();
		
		status.setFeature(feature);
		status.setEnabled(isFeatureEnabled);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setFeatureStatus(user, feature, status);
		}).getMessage();
		
		assertEquals("The feature is required.", errorMessage);

		
		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testSetFeatureStatusWithNoStatus() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		
		FeatureStatus status = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setFeatureStatus(user, feature, status);
		}).getMessage();
		
		assertEquals("The status is required.", errorMessage);

		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
	@Test
	public void testSetFeatureStatusWithNoStatusEnabled() {
		UserInfo user = mockUser;
		Feature feature = Feature.DATA_ACCESS_AUTO_REVOCATION;
		
		FeatureStatus status = new FeatureStatus();
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setFeatureStatus(user, feature, status);
		}).getMessage();
		
		assertEquals("The status.enabled is required.", errorMessage);

		verifyZeroInteractions(mockFeatureStatusDao);
	}
	
}
