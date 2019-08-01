package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.evaluation.SubmissionFileHandleDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionFileHandleAssociationProviderTest {

	@Mock
	private SubmissionFileHandleDAO mockSubmissionFileHandleDAO;
	private SubmissionFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new SubmissionFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "submissionFileHandleDao", mockSubmissionFileHandleDAO);
	}

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
}
