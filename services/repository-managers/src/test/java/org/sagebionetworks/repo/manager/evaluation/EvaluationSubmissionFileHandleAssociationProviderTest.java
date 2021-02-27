package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class EvaluationSubmissionFileHandleAssociationProviderTest {

	@Mock
	private SubmissionFileHandleDAO mockSubmissionFileHandleDAO;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private EvaluationSubmissionFileHandleAssociationProvider provider;
	

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String submissionId = "1";
		List<String> associatedIds = Arrays.asList("2", "3");
		when(mockSubmissionFileHandleDAO.getAllBySubmission(submissionId)).thenReturn(associatedIds);
		Set<String> result = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList("2", "4"), submissionId);
		assertEquals(Collections.singleton("2"), result);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.EVALUATION_SUBMISSIONS, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.SubmissionAttachment, provider.getAssociateType());
	}
}
