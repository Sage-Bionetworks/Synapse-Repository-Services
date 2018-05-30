package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_BY;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_DIRECTION;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_OFFSET;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EntityManagerImplUnitTest {

	@Mock
	private UserManager mockUserManager;
	@Mock
	private EntityPermissionsManager mockPermissionsManager;
	@Mock
	private NodeManager mockNodeManager;
	@Mock
	private UserInfo mockUser;

	private EntityManagerImpl entityManager;
	Long userId = 007L;
	
	EntityChildrenRequest childRequest;
	Set<Long> nonvisibleChildren;
	List<EntityHeader> childPage;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
	
		entityManager = new EntityManagerImpl();
		ReflectionTestUtils.setField(entityManager, "nodeManager", mockNodeManager);
		ReflectionTestUtils.setField(entityManager, "entityPermissionsManager", mockPermissionsManager);
		ReflectionTestUtils.setField(entityManager, "userManager", mockUserManager);
		
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		
		childRequest = new EntityChildrenRequest();
		childRequest.setParentId("syn123");
		childRequest.setIncludeTypes(Lists.newArrayList(EntityType.file, EntityType.folder));
		nonvisibleChildren = Sets.newHashSet(555L,777L);
		
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockPermissionsManager.getNonvisibleChildren(mockUser, childRequest.getParentId())).thenReturn(nonvisibleChildren);
		childPage = Lists.newArrayList(new EntityHeader());
		when(mockNodeManager.getChildren(anyString(),
						anyListOf(EntityType.class), anySetOf(Long.class),
						any(SortBy.class), any(Direction.class), anyLong(),
						anyLong())).thenReturn(childPage);

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
		Entity entity = new Project();
		entity.setId(id);
		
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
		Entity entity = new Project();
		entity.setId(id);
		
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
	public void testUpdateFileHandleId() throws Exception {
		String id = "123";
		Node node = new Node();
		node.setFileHandleId("101");
		NamedAnnotations annos = new NamedAnnotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getAnnotations(mockUser, id)).thenReturn(annos);
		FileEntity entity = new FileEntity();
		entity.setId(id);
		String dataFileHandleId = "202"; // i.e. we are updating from 101 to 202
		entity.setDataFileHandleId(dataFileHandleId);
		entity.setVersionComment("a comment for the original version");
		entity.setVersionLabel("a label for the original version");
		
		boolean newVersion = false; // even though new version is false the 
		// modified file handle will trigger a version update
		
		// method under test
		entityManager.updateEntity(mockUser, entity, newVersion, null);
		
		assertNull(node.getVersionComment());
		assertNull(node.getVersionLabel());
	}

	@Test
	public void testGetChildren(){
		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		verify(mockPermissionsManager).hasAccess(childRequest.getParentId(),
				ACCESS_TYPE.READ, mockUser);
		verify(mockPermissionsManager).getNonvisibleChildren(mockUser,
				childRequest.getParentId());
		verify(mockNodeManager).getChildren(childRequest.getParentId(),
				childRequest.getIncludeTypes(), nonvisibleChildren,
				DEFAULT_SORT_BY, DEFAULT_SORT_DIRECTION, DEFAULT_LIMIT+1,
				DEFAULT_OFFSET);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullUser(){
		mockUser = null;
		// call under test
		entityManager.getChildren(mockUser, childRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullRequest(){
		childRequest = null;
		// call under test
		entityManager.getChildren(mockUser, childRequest);
	}
	
	/**
	 * Null parentId is used to list projects.
	 */
	@Test
	public void testGetChildrenNullParentId(){
		childRequest.setParentId(null);
		childRequest.setIncludeTypes(null);
		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		// hasAcces should not be called for root.
		verify(mockPermissionsManager, never()).hasAccess(anyString(), any(ACCESS_TYPE.class), any(UserInfo.class));
		verify(mockPermissionsManager).getNonvisibleChildren(mockUser, EntityManagerImpl.ROOT_ID);
		verify(mockNodeManager).getChildren(
				EntityManagerImpl.ROOT_ID,
				EntityManagerImpl.PROJECT_ONLY,
				new HashSet<Long>(),
				SortBy.NAME, Direction.ASC,
				NextPageToken.DEFAULT_LIMIT+1,
				NextPageToken.DEFAULT_OFFSET);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullIncludeTypes(){
		childRequest.setIncludeTypes(null);
		// call under test
		entityManager.getChildren(mockUser, childRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenEmptyIncludeTypes(){
		childRequest.setIncludeTypes(new LinkedList<EntityType>());
		// call under test
		entityManager.getChildren(mockUser, childRequest);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetChildrenCannotReadParent(){
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// call under test
		entityManager.getChildren(mockUser, childRequest);
	}
	
	@Test
	public void testGetChildrenNextPage(){
		long limit = 10L;
		long offset = 10L;
		childPage.clear();
		for(int i=0; i<limit+1; i++){
			childPage.add(new EntityHeader());
		}
		NextPageToken token = new NextPageToken(limit, offset);
		childRequest.setNextPageToken(token.toToken());
		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		verify(mockNodeManager).getChildren(childRequest.getParentId(),
				childRequest.getIncludeTypes(), nonvisibleChildren,
				DEFAULT_SORT_BY, DEFAULT_SORT_DIRECTION, limit+1,
				offset);
		assertEquals(new NextPageToken(limit, offset+limit).toToken(), response.getNextPageToken());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupChildWithNullUserInfo() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		entityManager.lookupChild(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupChildWithNullRequest() {
		entityManager.lookupChild(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupChildWithNullEntityName() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		entityManager.lookupChild(mockUser, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testLookupChildCannotReadParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		entityManager.lookupChild(mockUser, request);
	}

	@Test (expected = NotFoundException.class)
	public void testLookupChildNotFound() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenThrow(new NotFoundException());
		entityManager.lookupChild(mockUser, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testLookupChildUnauthorized() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		entityManager.lookupChild(mockUser, request);
	}

	@Test
	public void testLookupChild() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}

	@Test
	public void testLookupChildWithNullParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeManager.lookupChild(EntityManagerImpl.ROOT_ID, request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}
}
