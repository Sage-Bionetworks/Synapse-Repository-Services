package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.springframework.test.util.ReflectionTestUtils;

public class AccessRequirementFileHandleAssociationProviderTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	private AccessRequirementFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new AccessRequirementFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "accessRequirementDao", mockAccessRequirementDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForNonACTAccessRequirement() {
		String accessRequirementId = "1";
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList("2"), accessRequirementId);
		assertTrue(associated.isEmpty());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String accessRequirementId = "1";
		String ducTemplateFileHandleId = "2";
		when(mockAccessRequirement.getDucTemplateFileHandleId()).thenReturn(ducTemplateFileHandleId);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(ducTemplateFileHandleId, "3"), accessRequirementId);
		assertEquals(Collections.singleton(ducTemplateFileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.ACCESS_REQUIREMENT, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
