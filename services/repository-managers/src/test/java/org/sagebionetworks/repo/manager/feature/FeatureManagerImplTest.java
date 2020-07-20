package org.sagebionetworks.repo.manager.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;

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
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(enabled, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsFeatureEnabledWithDisabled() {
		
		boolean enabled = false;
		
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(enabled));
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(enabled, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsFeatureEnabledWithNoData() {
		
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.empty());
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		
		boolean expected = false;
		
		// Call under test
		boolean result =  manager.isFeatureEnabled(feature);
		
		assertEquals(expected, result);
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
	}
	
	@Test
	public void testIsFeatureEnabledForUserWithNoFeature() {
		
		Feature feature = null;
		UserInfo user = mockUser;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.isFeatureEnabledForUser(feature, user);
		}).getMessage();
		
		assertEquals("The feature is required.", message);
	}
	
	@Test
	public void testIsFeatureEnabledForUserWithNoUser() {
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		UserInfo user = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.isFeatureEnabledForUser(feature, user);
		}).getMessage();
		
		assertEquals("The user is required.", message);
	}
	
	@Test
	public void testIsFeatureEnabledForUserWithDisabledAndNotTestingGroup() {
		
		Set<Long> userGroups = Collections.emptySet();
		boolean enabled = false;
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(enabled));
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		UserInfo user = mockUser;

		boolean expected = enabled;
		// Call under test
		boolean result = manager.isFeatureEnabledForUser(feature, user);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
		
	}
	
	@Test
	public void testIsFeatureEnabledForUserWithEnabledAndNotTestingGroup() {
		
		Set<Long> userGroups = Collections.emptySet();
		boolean enabled = true;
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(enabled));
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		UserInfo user = mockUser;

		boolean expected = enabled;
		// Call under test
		boolean result = manager.isFeatureEnabledForUser(feature, user);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
		verify(mockFeatureStatusDao).isFeatureEnabled(feature);
		
	}
	
	@Test
	public void testIsFeatureEnabledForUserWithDisabledAndInTestingGroup() {
		
		Set<Long> userGroups = Collections.singleton(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId());
		
		when(mockUser.getGroups()).thenReturn(userGroups);
		
		Feature feature = Feature.DATA_ACCESS_RENEWALS;
		UserInfo user = mockUser;

		boolean expected = true;
		// Call under test
		boolean result = manager.isFeatureEnabledForUser(feature, user);
		
		assertEquals(expected, result);
		verify(mockUser).getGroups();
		verifyZeroInteractions(mockFeatureStatusDao);
		
	}
	
}
