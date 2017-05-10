package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.message.BroadcastMessageBuilder;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionStatusMessageBuilderFactoryTest {

	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private MarkdownDao mockMarkdownDao;
	@Mock
	private Submission mockSubmission;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	
	SubmissionStatusMessageBuilderFactory factory;
	private String objectId;
	private Long actorUserId;
	private Long requirementId;
	private RestrictableObjectDescriptor rod;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		factory = new SubmissionStatusMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "submissionDao", mockSubmissionDao);
		ReflectionTestUtils.setField(factory, "markdownDao", mockMarkdownDao);
		ReflectionTestUtils.setField(factory, "accessRequirementDao", mockAccessRequirementDao);

		objectId = "1";
		actorUserId = 2L;
		requirementId = 3L;

		rod = new RestrictableObjectDescriptor();
		rod.setId("4");
		rod.setType(RestrictableObjectType.ENTITY);

		when(mockSubmissionDao.getSubmission(objectId)).thenReturn(mockSubmission);
		when(mockSubmission.getState()).thenReturn(SubmissionState.APPROVED);
		when(mockSubmission.getAccessRequirementId()).thenReturn(requirementId.toString());
		when(mockAccessRequirementDao.getSubjects(requirementId)).thenReturn(Arrays.asList(rod));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithInvalidObjectId() {
		factory.createMessageBuilder(null, ChangeType.UPDATE, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithNullChangeType() {
		factory.createMessageBuilder(objectId, null, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithCreateChangeType() {
		factory.createMessageBuilder(objectId, ChangeType.CREATE, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithInvalidSubmissionState() {
		when(mockSubmission.getState()).thenReturn(SubmissionState.CANCELLED);
		factory.createMessageBuilder(objectId, ChangeType.UPDATE, actorUserId);
	}

	@Test
	public void testBuild(){
		ChangeType type = ChangeType.UPDATE;
		BroadcastMessageBuilder builder = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(builder);
		assertTrue(builder instanceof SubmissionStatusBroadcastMessageBuilder);
		verify(mockSubmissionDao).getSubmission(objectId);
	}
}
