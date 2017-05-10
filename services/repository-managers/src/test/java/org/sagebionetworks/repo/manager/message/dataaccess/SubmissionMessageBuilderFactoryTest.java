package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.message.BroadcastMessageBuilder;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionMessageBuilderFactoryTest {

	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private MarkdownDao mockMarkdownDao;
	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private Submission mockSubmission;
	
	SubmissionMessageBuilderFactory factory;
	private Long actorUserId;
	private String actorUsername;
	private String objectId;
	private String requirementId;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		factory = new SubmissionMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(factory, "markdownDao", mockMarkdownDao);
		ReflectionTestUtils.setField(factory, "submissionDao", mockSubmissionDao);

		actorUserId = 1L;
		actorUsername = "username";
		objectId = "123";
		requirementId = "2";
		when(mockPrincipalAliasDAO.getUserName(actorUserId)).thenReturn(actorUsername);
		when(mockSubmissionDao.getSubmission(objectId)).thenReturn(mockSubmission);
		when(mockSubmission.getAccessRequirementId()).thenReturn(requirementId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithInvalidObjectId() {
		factory.createMessageBuilder(null, ChangeType.CREATE, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithNullChangeType() {
		factory.createMessageBuilder(objectId, null, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithUpdateChangeType() {
		factory.createMessageBuilder(objectId, ChangeType.UPDATE, actorUserId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildWithInvalidUserId() {
		factory.createMessageBuilder(objectId, ChangeType.CREATE, null);
	}

	@Test
	public void testBuild(){
		ChangeType type = ChangeType.CREATE;
		BroadcastMessageBuilder builder = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(builder);
		assertTrue(builder instanceof SubmissionBroadcastMessageBuilder);
		verify(mockPrincipalAliasDAO).getUserName(actorUserId);
	}
}
