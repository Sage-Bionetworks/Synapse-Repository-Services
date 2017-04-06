package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessRequestFileHandleAssociationProviderTest {

	@Mock
	private DataAccessRequestDAO mockDataAccessRequestDao;
	@Mock
	private DataAccessRequestInterface mockDataAccessRequest;
	private DataAccessRequestFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new DataAccessRequestFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "dataAccessRequestDao", mockDataAccessRequestDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String requestId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockDataAccessRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockDataAccessRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockDataAccessRequest.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockDataAccessRequestDao.get(requestId)).thenReturn(mockDataAccessRequest);
		Set<String> associated = provider.getFileHandleIdsAssociatedWithObject(
				Arrays.asList(duc, other, "5"), requestId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertTrue(associated.contains(other));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirementNullAttachment() {
		String requestId = "1";
		String duc = "2";
		String irb = "3";
		when(mockDataAccessRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockDataAccessRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockDataAccessRequestDao.get(requestId)).thenReturn(mockDataAccessRequest);
		Set<String> associated = provider.getFileHandleIdsAssociatedWithObject(
				Arrays.asList(duc, "5"), requestId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.DATA_ACCESS_REQUEST, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
