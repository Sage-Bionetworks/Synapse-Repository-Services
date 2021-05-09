package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementFileHandleAssociationProviderTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	
	@InjectMocks
	private AccessRequirementFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForNonACTAccessRequirement() {
		String accessRequirementId = "1";
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList("2"), accessRequirementId);
		assertTrue(associated.isEmpty());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.AccessRequirementAttachment, provider.getAssociateType());
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
