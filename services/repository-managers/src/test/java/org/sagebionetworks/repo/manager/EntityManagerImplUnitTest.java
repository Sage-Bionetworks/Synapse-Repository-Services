package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_BY;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_DIRECTION;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_OFFSET;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityManagerImplUnitTest {
	private static final String ACTIVITY_ID = "activity-id";
	private static final String ENTITY_ID = "entity-id";
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String PARENT_ENTITY_ID = "parent-entity-id";
	private static final long STS_STORAGE_LOCATION_ID = 123;
	private static final long NON_STS_STORAGE_LOCATION_ID = 456;
	private static final long DIFFERENT_STS_STORAGE_LOCATION_ID = 789;

	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private EntityPermissionsManager mockPermissionsManager;
	@Mock
	private NodeManager mockNodeManager;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ObjectTypeManager mockObjectTypeManger;
	
	@Captor
	private ArgumentCaptor<ChildStatsRequest> statsRequestCaptor;

	@InjectMocks
	@Spy
	private EntityManagerImpl entityManager;
	Long userId = 007L;
	
	EntityChildrenRequest childRequest;
	Set<Long> nonvisibleChildren;
	List<EntityHeader> childPage;

	private FileEntity fileEntity;
	private S3FileHandle fileHandle;
	private S3StorageLocationSetting storageLocationSetting;
	private UploadDestinationListSetting projectSetting;

	@BeforeEach
	public void before(){
		
		childRequest = new EntityChildrenRequest();
		childRequest.setParentId("syn123");
		childRequest.setIncludeTypes(Lists.newArrayList(EntityType.file, EntityType.folder));
		childRequest.setIncludeTotalChildCount(true);
		childRequest.setIncludeSumFileSizes(false);
		
		nonvisibleChildren = Sets.newHashSet(555L,777L);
		childPage = Lists.newArrayList(new EntityHeader());

		fileEntity = new FileEntity();
		fileEntity.setId(ENTITY_ID);
		fileEntity.setDataFileHandleId(FILE_HANDLE_ID);
		fileEntity.setParentId(PARENT_ENTITY_ID);

		fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);

		storageLocationSetting = new S3StorageLocationSetting();

		projectSetting = new UploadDestinationListSetting();
	}

	@Test
	public void createEntity_ValidateFileEntityStsRestrictions() {
		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		String testErrorMessage = "test exception from validateFileEntityStsRestrictions";
		doThrow(new IllegalArgumentException(testErrorMessage)).when(entityManager).validateFileEntityStsRestrictions(
				mockUser, fileEntity);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.createEntity(mockUser, fileEntity,
				ACTIVITY_ID), testErrorMessage);
	}

	@Test
	public void createEntity_CanAddFile() {
		// Mock dependencies.
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation
				.getArgument(0));

		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		doNothing().when(entityManager).validateFileEntityStsRestrictions(mockUser, fileEntity);

		// Method under test.
		// Note that the ID generator would be called to fill in the ID, but that doesn't happen here because of our
		// test environment.
		entityManager.createEntity(mockUser, fileEntity, ACTIVITY_ID);

		ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
		verify(mockNodeManager).createNewNode(nodeCaptor.capture(), notNull(), same(mockUser));
		Node node = nodeCaptor.getValue();
		assertEquals(EntityType.file, node.getNodeType());
		assertEquals(ACTIVITY_ID, node.getActivityId());
	}

	@Test
	public void createEntity_CanAddFolder() {
		// Mock dependencies.
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation
				.getArgument(0));

		// Method under test.
		// Note that the ID generator would be called to fill in the ID, but that doesn't happen here because of our
		// test environment.
		Folder folder = new Folder();
		entityManager.createEntity(mockUser, folder, ACTIVITY_ID);

		ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
		verify(mockNodeManager).createNewNode(nodeCaptor.capture(), notNull(), same(mockUser));
		Node node = nodeCaptor.getValue();
		assertEquals(EntityType.folder, node.getNodeType());
		assertEquals(ACTIVITY_ID, node.getActivityId());
	}

	@Test
	public void createEntity_CannotAddTableToStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test.
		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);
		assertThrows(IllegalArgumentException.class, () -> entityManager.createEntity(mockUser, tableEntity,
				ACTIVITY_ID), "Can only create Files and Folders inside STS-enabled folders");
	}

	@Test
	public void createEntity_CanAddTableToNonStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation
				.getArgument(0));

		// Method under test.
		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);
		entityManager.createEntity(mockUser, tableEntity, ACTIVITY_ID);
		verify(mockNodeManager).createNewNode(notNull(), notNull(), same(mockUser));
	}

	@Test
	public void createEntity_CanAddTableToParentWithoutProjectSetting() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(null);
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation
				.getArgument(0));

		// Method under test.
		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);
		entityManager.createEntity(mockUser, tableEntity, ACTIVITY_ID);
		verify(mockNodeManager).createNewNode(notNull(), notNull(), same(mockUser));
	}

	@Test
	public void createEntity_CanAddTableWithoutParent() {
		// Mock dependencies.
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation
				.getArgument(0));

		// Method under test.
		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(null);
		entityManager.createEntity(mockUser, tableEntity, ACTIVITY_ID);
		verify(mockNodeManager).createNewNode(notNull(), notNull(), same(mockUser));
	}

	@Test
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String entityId = "abc";
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			entityManager.validateReadAccess(mockUser, entityId);
		});
		
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		
	}
	
	@Test 
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String entityId = "abc";
		
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		
		entityManager.validateReadAccess(mockUser, entityId);
		
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
	}


	@Test
	public void testUpdateEntityActivityId() throws Exception {
		String id = "123";
		String parentId = "456";
		
		Node node = mock(Node.class);
		Annotations annos = new Annotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		when(node.getParentId()).thenReturn(parentId);
		
		Entity entity = new Project();
		entity.setId(id);
		entity.setParentId(parentId);
		
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
	public void testUpdateEntity_NullId() {
		fileEntity.setId(null);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> entityManager.updateEntity(mockUser, fileEntity,
				false, ACTIVITY_ID), "The id of the entity should be present");
		verifyZeroInteractions(mockNodeManager);
	}

	/**
	 * Test for PLFM-5873
	 */
	@Test
	public void testUpdateEntityWithNullParent() {
		fileEntity.setParentId(null);

		// Method under test
		assertThrows(IllegalArgumentException.class, ()-> entityManager.updateEntity(mockUser, fileEntity, false,
				ACTIVITY_ID), "The parentId of the entity should be present" );
		verifyZeroInteractions(mockNodeManager);
	}
	
	@Test
	public void testDeleteActivityId() throws Exception {
		String id = "123";
		String parentId = "456";
		
		Node node = mock(Node.class);
		Annotations annos = new Annotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		when(node.getParentId()).thenReturn(parentId);
		
		Entity entity = new Project();
		entity.setId(id);
		entity.setParentId(parentId);
		
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
	public void testUpdateEntity_ValidateFileEntityStsRestrictions() {
		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		String testErrorMessage = "test exception from validateFileEntityStsRestrictions";
		doThrow(new IllegalArgumentException(testErrorMessage)).when(entityManager).validateFileEntityStsRestrictions(
				mockUser, fileEntity);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.updateEntity(mockUser, fileEntity,
				false, ACTIVITY_ID), testErrorMessage);
	}

	@Test
	public void testUpdateFileHandleId() throws Exception {
		// Mock dependencies.
		String id = "123";
		String parentId = "456";
		
		Node node = new Node();
		node.setFileHandleId("101");
		Annotations annos = new Annotations();
		when(mockNodeManager.get(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);

		// Make file entity to update.
		FileEntity entity = new FileEntity();
		entity.setId(id);
		entity.setParentId(parentId);
		String dataFileHandleId = "202"; // i.e. we are updating from 101 to 202
		entity.setDataFileHandleId(dataFileHandleId);
		entity.setVersionComment("a comment for the original version");
		entity.setVersionLabel("a label for the original version");

		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		doNothing().when(entityManager).validateFileEntityStsRestrictions(mockUser, entity);

		boolean newVersion = false; // even though new version is false the
		// modified file handle will trigger a version update
		
		// method under test
		entityManager.updateEntity(mockUser, entity, newVersion, null);
		
		assertNull(node.getVersionComment());
		assertNull(node.getVersionLabel());
	}

	@Test
	public void testGetChildren() {
		
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		when(mockPermissionsManager.getNonvisibleChildren(mockUser, childRequest.getParentId())).thenReturn(nonvisibleChildren);
		
		when(mockNodeManager.getChildren(anyString(),
						anyListOf(EntityType.class), anySetOf(Long.class),
						any(SortBy.class), any(Direction.class), anyLong(),
						anyLong())).thenReturn(childPage);

		ChildStatsResponse statsReponse = new ChildStatsResponse().withSumFileSizesBytes(123L).withTotalChildCount(4L);
		when(mockNodeManager.getChildrenStats(any(ChildStatsRequest.class))).thenReturn(statsReponse);

		
		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		assertEquals(statsReponse.getTotalChildCount(), response.getTotalChildCount());
		assertEquals(statsReponse.getSumFileSizesBytes(), response.getSumFileSizesBytes());
		verify(mockPermissionsManager).hasAccess(childRequest.getParentId(),
				ACCESS_TYPE.READ, mockUser);
		verify(mockPermissionsManager).getNonvisibleChildren(mockUser,
				childRequest.getParentId());
		verify(mockNodeManager).getChildren(childRequest.getParentId(),
				childRequest.getIncludeTypes(), nonvisibleChildren,
				DEFAULT_SORT_BY, DEFAULT_SORT_DIRECTION, DEFAULT_LIMIT+1,
				DEFAULT_OFFSET);
		
		verify(mockNodeManager).getChildrenStats(statsRequestCaptor.capture());
		ChildStatsRequest statsRequest = statsRequestCaptor.getValue();
		assertNotNull(statsRequest);
		assertEquals(childRequest.getParentId(), statsRequest.getParentId());
		assertEquals(childRequest.getIncludeTypes(), statsRequest.getIncludeTypes());
		assertEquals(nonvisibleChildren, statsRequest.getChildIdsToExclude());
		assertEquals(childRequest.getIncludeTotalChildCount(), statsRequest.getIncludeTotalChildCount());
		assertEquals(childRequest.getIncludeSumFileSizes(), statsRequest.getIncludeSumFileSizes());
	}
	
	@Test
	public void testGetChildrenNullUser(){
		mockUser = null;
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}
	
	@Test
	public void testGetChildrenNullRequest(){
		childRequest = null;
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}
	
	/**
	 * Null parentId is used to list projects.
	 */
	@Test
	public void testGetChildrenNullParentId(){
		
		when(mockNodeManager.getChildren(anyString(),
						anyListOf(EntityType.class), anySetOf(Long.class),
						any(SortBy.class), any(Direction.class), anyLong(),
						anyLong())).thenReturn(childPage);

		ChildStatsResponse statsReponse = new ChildStatsResponse().withSumFileSizesBytes(123L).withTotalChildCount(4L);
		when(mockNodeManager.getChildrenStats(any(ChildStatsRequest.class))).thenReturn(statsReponse);

		
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
	
	@Test
	public void testGetChildrenNullIncludeTypes(){
		childRequest.setIncludeTypes(null);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}
	
	@Test
	public void testGetChildrenEmptyIncludeTypes(){
		childRequest.setIncludeTypes(new LinkedList<EntityType>());
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}
	
	@Test
	public void testGetChildrenCannotReadParent(){
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}
	
	@Test
	public void testGetChildrenNextPage(){
		
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		when(mockPermissionsManager.getNonvisibleChildren(mockUser, childRequest.getParentId())).thenReturn(nonvisibleChildren);
		
		when(mockNodeManager.getChildren(anyString(),
						anyListOf(EntityType.class), anySetOf(Long.class),
						any(SortBy.class), any(Direction.class), anyLong(),
						anyLong())).thenReturn(childPage);

		ChildStatsResponse statsReponse = new ChildStatsResponse().withSumFileSizesBytes(123L).withTotalChildCount(4L);
		when(mockNodeManager.getChildrenStats(any(ChildStatsRequest.class))).thenReturn(statsReponse);

		
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

	@Test
	public void testLookupChildWithNullUserInfo() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			entityManager.lookupChild(null, request);
		});
	}

	@Test
	public void testLookupChildWithNullRequest() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			entityManager.lookupChild(mockUser, null);
		});
	}

	@Test
	public void testLookupChildWithNullEntityName() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildCannotReadParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildNotFound() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenThrow(new NotFoundException());
		
		Assertions.assertThrows(NotFoundException.class, ()-> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildUnauthorized() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChild() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}

	
	@Test
	public void testLookupChildWithNullParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockNodeManager.lookupChild(EntityManagerImpl.ROOT_ID, request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(AuthorizationStatus.authorized());
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}
	
	@Test
	public void testChangeEntityDataType() {
		String entityId = "syn123";
		DataType dataType = DataType.SENSITIVE_DATA;
		// call under test
		entityManager.changeEntityDataType(mockUser, entityId, dataType);
		verify(mockObjectTypeManger).changeObjectsDataType(mockUser, entityId, ObjectType.ENTITY, dataType);
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInSameStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Does not throw.
		entityManager.validateFileEntityStsRestrictions(mockUser, fileEntity);
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInDifferentStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(DIFFERENT_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.validateFileEntityStsRestrictions(mockUser,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInNonStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(NON_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.validateFileEntityStsRestrictions(mockUser,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInParentWithoutProjectSettings() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(null);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.validateFileEntityStsRestrictions(mockUser,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	// branch coverage: This is theoretically possible if someone forgets to specify the parent ID. In this case, the
	// file gets added to the root project (which is not STS enabled) in a later step.
	@Test
	public void validateFileEntityStsRestrictions_StsFileWithNoParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		fileEntity.setParentId(null);
		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.validateFileEntityStsRestrictions(mockUser,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_NonStsFileInStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(NON_STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(false);

		projectSetting.setLocations(ImmutableList.of(STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> entityManager.validateFileEntityStsRestrictions(mockUser,
				fileEntity), "Folders with STS-enabled storage locations can only accept " +
				"files with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_NonStsFileInNonStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(NON_STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(false);

		projectSetting.setLocations(ImmutableList.of(NON_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(projectSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Method under test - Does not throw.
		entityManager.validateFileEntityStsRestrictions(mockUser, fileEntity);
	}

	@Test
	public void testValidateEntityNull() {
		Project project = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			EntityManagerImpl.validateEntity(project);
		});
	}
	
	@Test
	public void testValidateEntityDescriptionNull() {
		Project project = new Project();
		project.setDescription(null);
		// call under test
		EntityManagerImpl.validateEntity(project);
	}
	
	@Test
	public void testValidateEntityDescriptionAtLimit() {
		Project project = new Project();
		String description = StringUtils.repeat("b", EntityManagerImpl.MAX_DESCRIPTION_CHARS);
		project.setDescription(description);
		// call under test
		EntityManagerImpl.validateEntity(project);
	}
	
	@Test
	public void testValidateEntityNameNull() {
		Project project = new Project();
		project.setName(null);
		// call under test
		EntityManagerImpl.validateEntity(project);
	}
	
	@Test
	public void testValidateEntityNameAtLimit() {
		Project project = new Project();
		String name = StringUtils.repeat("b", EntityManagerImpl.MAX_NAME_CHARS);
		project.setName(name);
		// call under test
		EntityManagerImpl.validateEntity(project);
	}
	
	@Test
	public void testValidateEntityNameOverLimit() {
		Project project = new Project();
		String name = StringUtils.repeat("b", EntityManagerImpl.MAX_NAME_CHARS+1);
		project.setName(name);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			EntityManagerImpl.validateEntity(project);
		}).getMessage();
		assertEquals("Name must be "+EntityManagerImpl.MAX_NAME_CHARS+" characters or less", message);
	}
	
	@Test
	public void testValidateEntityDescriptionOverLimit() {
		Project project = new Project();
		String description = StringUtils.repeat("b", EntityManagerImpl.MAX_DESCRIPTION_CHARS+1);
		project.setDescription(description);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			EntityManagerImpl.validateEntity(project);
		}).getMessage();
		assertEquals("Description must be "+EntityManagerImpl.MAX_DESCRIPTION_CHARS+" characters or less", message);

	}
	
	@Test
	public void testCreateEntityDescriptionOverLimit() {
		String activityId = null;
		Project project = new Project();
		String description = StringUtils.repeat("b", EntityManagerImpl.MAX_DESCRIPTION_CHARS+1);
		project.setDescription(description);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManager.createEntity(mockUser, project, activityId);
		}).getMessage();
		assertTrue(message.startsWith("Description must be"));
	}
	
	@Test
	public void testUpdateEntityDescriptionOverLimit() {
		String activityId = null;
		boolean newVersion = true;
		Project project = new Project();
		String description = StringUtils.repeat("b", EntityManagerImpl.MAX_DESCRIPTION_CHARS+1);
		project.setDescription(description);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManager.updateEntity(mockUser, project, newVersion, activityId);
		}).getMessage();
		assertTrue(message.startsWith("Description must be"));
	}
	
	@Test
	public void testEntityUpdateUnderVersionLimit() {
		String activityId = null;
		boolean newVersion = true;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.get(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		// still have room for one more version
		when(mockNodeManager.getCurrentRevisionNumber(project.getId())).thenReturn((long) EntityManagerImpl.MAX_NUMBER_OF_REVISIONS-1);
		// call under test
		entityManager.updateEntity(mockUser, project, newVersion, activityId);
		verify(mockNodeManager).update(eq(mockUser),any(Node.class), any(Annotations.class), eq(newVersion));
		verify(mockNodeManager).getCurrentRevisionNumber(project.getId());
	}
	
	@Test
	public void testEntityUpdateUnderVersionOverLimit() {
		String activityId = null;
		boolean newVersion = true;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.get(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		long currentRevisionNumber = (long) EntityManagerImpl.MAX_NUMBER_OF_REVISIONS;
		// still have room for one more version
		when(mockNodeManager.getCurrentRevisionNumber(project.getId())).thenReturn(currentRevisionNumber);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManager.updateEntity(mockUser, project, newVersion, activityId);
		}).getMessage();
		assertEquals("Exceeded the maximum number of "+EntityManagerImpl.MAX_NUMBER_OF_REVISIONS+" versions for a single Entity", message);
		verify(mockNodeManager, never()).update(any(UserInfo.class),any(Node.class), any(Annotations.class), any(Boolean.class));
		verify(mockNodeManager).getCurrentRevisionNumber(project.getId());
	}
	
	@Test
	public void testEntityUpdateAtLimitNewVersionFalse() {
		String activityId = null;
		boolean newVersion = false;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.get(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		// call under test
		entityManager.updateEntity(mockUser, project, newVersion, activityId);
		verify(mockNodeManager).update(eq(mockUser),any(Node.class), any(Annotations.class), eq(newVersion));
		verify(mockNodeManager, never()).getCurrentRevisionNumber(project.getId());
	}
}
