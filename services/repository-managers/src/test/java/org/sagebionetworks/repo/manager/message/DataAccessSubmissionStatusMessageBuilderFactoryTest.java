package org.sagebionetworks.repo.manager.message;

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
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessSubmissionStatusMessageBuilderFactoryTest {

	@Mock
	private DataAccessSubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private MarkdownDao mockMarkdownDao;
	@Mock
	private DataAccessSubmission mockSubmission;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	
	DataAccessSubmissionStatusMessageBuilderFactory factory;
	private String objectId;
	private Long actorUserId;
	private Long requirementId;
	private RestrictableObjectDescriptor rod;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		factory = new DataAccessSubmissionStatusMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "dataAccessSubmissionDao", mockDataAccessSubmissionDao);
		ReflectionTestUtils.setField(factory, "markdownDao", mockMarkdownDao);
		ReflectionTestUtils.setField(factory, "accessRequirementDao", mockAccessRequirementDao);

		objectId = "1";
		actorUserId = 2L;
		requirementId = 3L;

		rod = new RestrictableObjectDescriptor();
		rod.setId("4");
		rod.setType(RestrictableObjectType.ENTITY);

		when(mockDataAccessSubmissionDao.getSubmission(objectId)).thenReturn(mockSubmission);
		when(mockSubmission.getState()).thenReturn(DataAccessSubmissionState.APPROVED);
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
		when(mockSubmission.getState()).thenReturn(DataAccessSubmissionState.CANCELLED);
		factory.createMessageBuilder(objectId, ChangeType.UPDATE, actorUserId);
	}

	@Test
	public void testBuild(){
		ChangeType type = ChangeType.UPDATE;
		BroadcastMessageBuilder builder = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(builder);
		assertTrue(builder instanceof DataAccessSubmissionStatusBroadcastMessageBuilder);
		verify(mockDataAccessSubmissionDao).getSubmission(objectId);
	}
}
