package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;

public class EntityManagerImplUnitTest {

	private UserManager mockUserManager;
	private EntityPermissionsManager mockPermissionsManager;
	private UserInfo mockUser;
	private EntityManagerImpl entityManager;
	private NodeManager mockNodeManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	Long userId = 007L;
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(EntityPermissionsManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNodeManager = Mockito.mock(NodeManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockUser = new UserInfo(false);
		entityManager = new EntityManagerImpl(mockNodeManager, mockPermissionsManager, mockUserManager);
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
}
