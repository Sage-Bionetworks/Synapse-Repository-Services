package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class EntityManagerImplUnitTest {

	private UserManager mockUserManager;
	private EntityPermissionsManager mockPermissionsManager;
	private UserInfo mockUser;
	private EntityManagerImpl entityManager;
	private NodeManager mockNodeManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	private ForumManager mockForumManager;
	private SubscriptionManager mockSubscriptionManager;
	Long userId = 007L;
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(EntityPermissionsManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNodeManager = Mockito.mock(NodeManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockForumManager = Mockito.mock(ForumManager.class);
		mockSubscriptionManager = Mockito.mock(SubscriptionManager.class);
		mockUser = Mockito.mock(UserInfo.class);
		when(mockUser.getId()).thenReturn(userId);
		entityManager = new EntityManagerImpl(mockNodeManager, mockPermissionsManager, mockUserManager);
		ReflectionTestUtils.setField(entityManager, "forumManager", mockForumManager);
		ReflectionTestUtils.setField(entityManager, "subscriptionManager", mockSubscriptionManager);
	}

	@Test (expected=UnauthorizedException.class)
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
		
	}
	
	@Test 
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
	}
	
	@Test
	public void testGetEntitySecondaryFields() throws Exception {
		String id = "123";
		NamedAnnotations annos = new NamedAnnotations();
		annos.getPrimaryAnnotations().addAnnotation("fileNameOverride", "bar.txt");
		when(mockNodeManager.getAnnotations(mockUser, id)).thenReturn(annos);		
		FileEntity entity = entityManager.getEntitySecondaryFields(mockUser, id, FileEntity.class);
		assertEquals("0", entity.getCreatedBy());
		assertEquals("0", entity.getModifiedBy());
		assertEquals("bar.txt", entity.getFileNameOverride());
	}

	@Test
	public void testGetEntitySecondaryFieldsForVersion() throws Exception {
		String id = "123";
		NamedAnnotations annos = new NamedAnnotations();
		annos.getPrimaryAnnotations().addAnnotation("fileNameOverride", "bar.txt");
		when(mockNodeManager.getAnnotationsForVersion(mockUser, id, 1L)).thenReturn(annos);		
		FileEntity entity = entityManager.getEntitySecondaryFieldsForVersion(mockUser, id, 1L, FileEntity.class);
		assertEquals("0", entity.getCreatedBy());
		assertEquals("0", entity.getModifiedBy());
		assertEquals("bar.txt", entity.getFileNameOverride());
	}

	@Test
	public void testUpdateEntityActivityId() throws Exception {
		String id = "123";
		Node node = mock(Node.class);
		NamedAnnotations annos = new NamedAnnotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getAnnotations(mockUser, id)).thenReturn(annos);
		Entity entity = mock(Entity.class);
		when(entity.getId()).thenReturn(id);
		
		String activityId;		

		// Update: same version, null activity id. IMPORTANT: Do not overwrite activity id with null!
		activityId = null;
		entityManager.updateEntity(mockUser, entity, false, activityId);		
		verify(node, never()).setActivityId(anyString());
		reset(node);
		
		// Update: same version, defined activity id. 
		activityId = "1";
		entityManager.updateEntity(mockUser, entity, false, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);
	
		// Update: new version, null activity id. 
		activityId = null;
		entityManager.updateEntity(mockUser, entity, true, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);

		// Update: new version, defined activity id. 
		activityId = "1";
		entityManager.updateEntity(mockUser, entity, true, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);
	}

	@Test
	public void testDeleteActivityId() throws Exception {
		String id = "123";
		Node node = mock(Node.class);
		NamedAnnotations annos = new NamedAnnotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getAnnotations(mockUser, id)).thenReturn(annos);
		Entity entity = mock(Entity.class);
		when(entity.getId()).thenReturn(id);
		
		String activityId;		

		// Update: same version, null activity id. IMPORTANT: Do not overwrite activity id with null!
		activityId = null;
		entityManager.updateEntity(mockUser, entity, false, activityId);		
		verify(node, never()).setActivityId(anyString());
		reset(node);
		
		// Update: same version, defined activity id. 
		activityId = "1";
		entityManager.updateEntity(mockUser, entity, false, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);
	
		// Update: new version, null activity id. 
		activityId = null;
		entityManager.updateEntity(mockUser, entity, true, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);

		// Update: new version, defined activity id. 
		activityId = "1";
		entityManager.updateEntity(mockUser, entity, true, activityId);		
		verify(node).setActivityId(activityId);
		reset(node);
	}

	@Test
	public void testForumAndSubscribeWhenProjectCreated() {
		String projectId = "123";
		Entity newEntity = new Project();
		newEntity.setId(projectId);
		newEntity.setName("new project");
		String forumId = "456";
		Forum forum = new Forum();
		forum.setId(forumId);
		when(mockForumManager.createForum(eq(mockUser), anyString())).thenReturn(forum);
		entityManager.createEntity(mockUser, newEntity, null);
		ArgumentCaptor<Topic> ac = new ArgumentCaptor<Topic>();
		verify(mockSubscriptionManager).create(eq(mockUser), ac.capture());
		Topic topic = ac.getValue();
		assertEquals(forumId, topic.getObjectId());
		assertEquals(SubscriptionObjectType.FORUM, topic.getObjectType());
	}
}
