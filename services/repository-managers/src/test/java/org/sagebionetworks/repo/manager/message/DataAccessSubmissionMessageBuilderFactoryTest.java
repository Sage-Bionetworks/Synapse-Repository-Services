package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessSubmissionMessageBuilderFactoryTest {

	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private MarkdownDao mockMarkdownDao;
	@Mock
	private DataAccessSubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private DataAccessSubmission mockSubmission;
	
	DataAccessSubmissionMessageBuilderFactory factory;
	private Long actorUserId;
	private String actorUsername;
	private String objectId;
	private String requirementId;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		factory = new DataAccessSubmissionMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(factory, "markdownDao", mockMarkdownDao);
		ReflectionTestUtils.setField(factory, "dataAccessSubmissionDao", mockDataAccessSubmissionDao);

		actorUserId = 1L;
		actorUsername = "username";
		objectId = "123";
		requirementId = "2";
		when(mockPrincipalAliasDAO.getUserName(actorUserId)).thenReturn(actorUsername);
		when(mockDataAccessSubmissionDao.getSubmission(objectId)).thenReturn(mockSubmission);
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
		assertTrue(builder instanceof DataAccessSubmissionBroadcastMessageBuilder);
		verify(mockPrincipalAliasDAO).getUserName(actorUserId);
	}
}
