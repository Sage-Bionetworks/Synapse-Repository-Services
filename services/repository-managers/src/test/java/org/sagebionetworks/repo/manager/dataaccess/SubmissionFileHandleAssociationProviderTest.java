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
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionFileHandleAssociationProviderTest {

	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private Submission mockSubmission;
	private SubmissionFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new SubmissionFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "submissionDao", mockSubmissionDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String SubmissionId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockSubmission.getDucFileHandleId()).thenReturn(duc);
		when(mockSubmission.getIrbFileHandleId()).thenReturn(irb);
		when(mockSubmission.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockSubmissionDao.getSubmission(SubmissionId)).thenReturn(mockSubmission);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, other, "5"), SubmissionId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertTrue(associated.contains(other));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirementNullAttachment() {
		String SubmissionId = "1";
		String duc = "2";
		String irb = "3";
		when(mockSubmission.getDucFileHandleId()).thenReturn(duc);
		when(mockSubmission.getIrbFileHandleId()).thenReturn(irb);
		when(mockSubmissionDao.getSubmission(SubmissionId)).thenReturn(mockSubmission);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, "5"), SubmissionId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.DATA_ACCESS_SUBMISSION, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
