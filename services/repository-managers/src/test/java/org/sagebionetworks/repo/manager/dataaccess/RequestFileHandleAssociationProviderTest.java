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
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class RequestFileHandleAssociationProviderTest {

	@Mock
	private RequestDAO mockRequestDao;
	@Mock
	private RequestInterface mockRequest;
	private RequestFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new RequestFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "requestDao", mockRequestDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String requestId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockRequest.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockRequestDao.get(requestId)).thenReturn(mockRequest);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
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
		when(mockRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockRequestDao.get(requestId)).thenReturn(mockRequest);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
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
