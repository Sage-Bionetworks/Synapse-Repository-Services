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
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessSubmissionFileHandleAssociationProviderTest {

	@Mock
	private DataAccessSubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private DataAccessSubmission mockDataAccessSubmission;
	private DataAccessSubmissionFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new DataAccessSubmissionFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "dataAccessSubmissionDao", mockDataAccessSubmissionDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String SubmissionId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockDataAccessSubmission.getDucFileHandleId()).thenReturn(duc);
		when(mockDataAccessSubmission.getIrbFileHandleId()).thenReturn(irb);
		when(mockDataAccessSubmission.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockDataAccessSubmissionDao.getSubmission(SubmissionId)).thenReturn(mockDataAccessSubmission);
		Set<String> associated = provider.getFileHandleIdsAssociatedWithObject(
				Arrays.asList(duc, other, "5"), SubmissionId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertTrue(associated.contains(other));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.DATA_ACCESS_SUBMISSION, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
