package org.sagebionetworks.repo.manager.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
	
}
