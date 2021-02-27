package org.sagebionetworks.repo.manager.dataaccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class SubmissionFileHandleAssociationProviderTest {

	@Mock
	private SubmissionDAO mockSubmissionDao;
	
	@Mock
	private Submission mockSubmission;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private SubmissionFileHandleAssociationProvider provider;
	
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
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.DataAccessSubmissionAttachment, provider.getAssociateType());
	}
	
}
