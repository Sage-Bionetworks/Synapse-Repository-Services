package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_BY;
import static org.sagebionetworks.repo.manager.EntityManagerImpl.DEFAULT_SORT_DIRECTION;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.model.NextPageToken.DEFAULT_OFFSET;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
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
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.SchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityManagerImplUnitTest {
	private static final String ACTIVITY_ID = "activity-id";
	private static final String ENTITY_ID = "entity-id";
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String PARENT_ENTITY_ID = "parent-entity-id";

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
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private AnnotationsTranslator mockAnnotationTranslator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	SchemaValidationResultDao mockSchemaValidationResultDao;

	@Captor
	private ArgumentCaptor<ChildStatsRequest> statsRequestCaptor;

	@InjectMocks
	private EntityManagerImpl entityManager;

	@Spy
	@InjectMocks
	private EntityManagerImpl entityManagerSpy;

	Long userId = 007L;

	EntityChildrenRequest childRequest;
	Set<Long> nonvisibleChildren;
	List<EntityHeader> childPage;

	private FileEntity fileEntity;

	BindSchemaToEntityRequest schemaBindRequest;
	JsonSchemaObjectBinding schemaBinding;

	@BeforeEach
	public void before() {

		childRequest = new EntityChildrenRequest();
		childRequest.setParentId("syn123");
		childRequest.setIncludeTypes(Lists.newArrayList(EntityType.file, EntityType.folder));
		childRequest.setIncludeTotalChildCount(true);
		childRequest.setIncludeSumFileSizes(false);

		nonvisibleChildren = Sets.newHashSet(555L, 777L);
		childPage = Lists.newArrayList(new EntityHeader());

		fileEntity = new FileEntity();
		fileEntity.setId(ENTITY_ID);
		fileEntity.setDataFileHandleId(FILE_HANDLE_ID);
		fileEntity.setParentId(PARENT_ENTITY_ID);

		schemaBindRequest = new BindSchemaToEntityRequest();
		schemaBindRequest.setEntityId("syn123");
		schemaBindRequest.setSchema$id("my.org/foo.bar/1.1.2");
		schemaBinding = new JsonSchemaObjectBinding();
		schemaBinding.setObjectId(123L);
		schemaBinding.setObjectType(BoundObjectType.entity);
	}

	@Test
	public void createEntity() {
		// Mock dependencies.
		when(mockNodeManager.createNewNode(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		// Method under test.
		// Note that the ID generator would be called to fill in the ID, but that
		// doesn't happen here because of our
		// test environment.
		entityManager.createEntity(mockUser, fileEntity, ACTIVITY_ID);

		ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
		verify(mockNodeManager).createNewNode(nodeCaptor.capture(), notNull(), same(mockUser));
		Node node = nodeCaptor.getValue();
		assertEquals(EntityType.file, node.getNodeType());
		assertEquals(ACTIVITY_ID, node.getActivityId());
	}

	@Test
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException {
		String entityId = "abc";
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			entityManager.validateReadAccess(mockUser, entityId);
		});

		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);

	}

	@Test
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException {
		String entityId = "abc";

		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());

		entityManager.validateReadAccess(mockUser, entityId);

		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
	}

	@Test
	public void testUpdateEntityActivityId() throws Exception {
		String id = "123";
		String parentId = "456";

		Node node = mock(Node.class);
		Annotations annos = new Annotations();
		when(mockNodeManager.getNode(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		when(node.getParentId()).thenReturn(parentId);

		Entity entity = new Project();
		entity.setId(id);
		entity.setParentId(parentId);

		String activityId;

		// Update: same version, null activity id. IMPORTANT: Do not overwrite activity
		// id with null!
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
		String parentId = "456";

		Node node = mock(Node.class);
		Annotations annos = new Annotations();
		when(mockNodeManager.getNode(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		when(node.getParentId()).thenReturn(parentId);

		Entity entity = new Project();
		entity.setId(id);
		entity.setParentId(parentId);

		String activityId;

		// Update: same version, null activity id. IMPORTANT: Do not overwrite activity
		// id with null!
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
		// Mock dependencies.
		String id = "123";
		String parentId = "456";

		Node node = new Node();
		node.setFileHandleId("101");
		Annotations annos = new Annotations();
		when(mockNodeManager.getNode(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);

		// Make file entity to update.
		FileEntity entity = new FileEntity();
		entity.setId(id);
		entity.setParentId(parentId);
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
	public void testGetChildren() {

		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockPermissionsManager.getNonvisibleChildren(mockUser, childRequest.getParentId()))
				.thenReturn(nonvisibleChildren);

		when(mockNodeManager.getChildren(anyString(), anyListOf(EntityType.class), anySetOf(Long.class),
				any(SortBy.class), any(Direction.class), anyLong(), anyLong())).thenReturn(childPage);

		ChildStatsResponse statsReponse = new ChildStatsResponse().withSumFileSizesBytes(123L).withTotalChildCount(4L);
		when(mockNodeManager.getChildrenStats(any(ChildStatsRequest.class))).thenReturn(statsReponse);

		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		assertEquals(statsReponse.getTotalChildCount(), response.getTotalChildCount());
		assertEquals(statsReponse.getSumFileSizesBytes(), response.getSumFileSizesBytes());
		verify(mockPermissionsManager).hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser);
		verify(mockPermissionsManager).getNonvisibleChildren(mockUser, childRequest.getParentId());
		verify(mockNodeManager).getChildren(childRequest.getParentId(), childRequest.getIncludeTypes(),
				nonvisibleChildren, DEFAULT_SORT_BY, DEFAULT_SORT_DIRECTION, DEFAULT_LIMIT + 1, DEFAULT_OFFSET);

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
	public void testGetChildrenNullUser() {
		mockUser = null;
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	@Test
	public void testGetChildrenNullRequest() {
		childRequest = null;
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	/**
	 * Null parentId is used to list projects.
	 */
	@Test
	public void testGetChildrenNullParentId() {

		when(mockNodeManager.getChildren(anyString(), anyListOf(EntityType.class), anySetOf(Long.class),
				any(SortBy.class), any(Direction.class), anyLong(), anyLong())).thenReturn(childPage);

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
		verify(mockNodeManager).getChildren(EntityManagerImpl.ROOT_ID, EntityManagerImpl.PROJECT_ONLY,
				new HashSet<Long>(), SortBy.NAME, Direction.ASC, NextPageToken.DEFAULT_LIMIT + 1,
				NextPageToken.DEFAULT_OFFSET);
	}

	@Test
	public void testGetChildrenNullIncludeTypes() {
		childRequest.setIncludeTypes(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	@Test
	public void testGetChildrenEmptyIncludeTypes() {
		childRequest.setIncludeTypes(new LinkedList<EntityType>());

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	@Test
	public void testGetChildrenCannotReadParent() {
		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	@Test
	public void testGetChildrenNextPage() {

		when(mockPermissionsManager.hasAccess(childRequest.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockPermissionsManager.getNonvisibleChildren(mockUser, childRequest.getParentId()))
				.thenReturn(nonvisibleChildren);

		when(mockNodeManager.getChildren(anyString(), anyListOf(EntityType.class), anySetOf(Long.class),
				any(SortBy.class), any(Direction.class), anyLong(), anyLong())).thenReturn(childPage);

		ChildStatsResponse statsReponse = new ChildStatsResponse().withSumFileSizesBytes(123L).withTotalChildCount(4L);
		when(mockNodeManager.getChildrenStats(any(ChildStatsRequest.class))).thenReturn(statsReponse);

		long limit = 10L;
		long offset = 10L;
		childPage.clear();
		for (int i = 0; i < limit + 1; i++) {
			childPage.add(new EntityHeader());
		}
		NextPageToken token = new NextPageToken(limit, offset);
		childRequest.setNextPageToken(token.toToken());
		// call under test
		EntityChildrenResponse response = entityManager.getChildren(mockUser, childRequest);
		assertNotNull(response);
		verify(mockNodeManager).getChildren(childRequest.getParentId(), childRequest.getIncludeTypes(),
				nonvisibleChildren, DEFAULT_SORT_BY, DEFAULT_SORT_DIRECTION, limit + 1, offset);
		assertEquals(new NextPageToken(limit, offset + limit).toToken(), response.getNextPageToken());
	}

	@Test
	public void testLookupChildWithNullUserInfo() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			entityManager.lookupChild(null, request);
		});
	}

	@Test
	public void testLookupChildWithNullRequest() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			entityManager.lookupChild(mockUser, null);
		});
	}

	@Test
	public void testLookupChildWithNullEntityName() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildCannotReadParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildNotFound() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName()))
				.thenThrow(new NotFoundException());

		Assertions.assertThrows(NotFoundException.class, () -> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChildUnauthorized() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			entityManager.lookupChild(mockUser, request);
		});
	}

	@Test
	public void testLookupChild() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setParentId("syn1");
		request.setEntityName("entityName");
		String entityId = "syn2";
		when(mockPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
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
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser))
				.thenReturn(AuthorizationStatus.authorized());
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
	public void testEntityUpdateUnderVersionLimit() {
		String activityId = null;
		boolean newVersion = true;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.getNode(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		// still have room for one more version
		when(mockNodeManager.getCurrentRevisionNumber(project.getId()))
				.thenReturn((long) EntityManagerImpl.MAX_NUMBER_OF_REVISIONS - 1);
		// call under test
		entityManager.updateEntity(mockUser, project, newVersion, activityId);
		verify(mockNodeManager).update(eq(mockUser), any(Node.class), any(Annotations.class), eq(newVersion));
		verify(mockNodeManager).getCurrentRevisionNumber(project.getId());
	}

	@Test
	public void testEntityUpdateUnderVersionOverLimit() {
		String activityId = null;
		boolean newVersion = true;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.getNode(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		long currentRevisionNumber = (long) EntityManagerImpl.MAX_NUMBER_OF_REVISIONS;
		// still have room for one more version
		when(mockNodeManager.getCurrentRevisionNumber(project.getId())).thenReturn(currentRevisionNumber);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.updateEntity(mockUser, project, newVersion, activityId);
		}).getMessage();
		assertEquals("Exceeded the maximum number of " + EntityManagerImpl.MAX_NUMBER_OF_REVISIONS
				+ " versions for a single Entity", message);
		verify(mockNodeManager, never()).update(any(UserInfo.class), any(Node.class), any(Annotations.class),
				any(Boolean.class));
		verify(mockNodeManager).getCurrentRevisionNumber(project.getId());
	}

	@Test
	public void testEntityUpdateAtLimitNewVersionFalse() {
		String activityId = null;
		boolean newVersion = false;
		Project project = new Project();
		project.setId("syn123");
		project.setParentId("syn456");
		when(mockNodeManager.getNode(mockUser, project.getId())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, project.getId())).thenReturn(new Annotations());
		// call under test
		entityManager.updateEntity(mockUser, project, newVersion, activityId);
		verify(mockNodeManager).update(eq(mockUser), any(Node.class), any(Annotations.class), eq(newVersion));
		verify(mockNodeManager, never()).getCurrentRevisionNumber(project.getId());
	}

	// Test for both PLFM-6362 and PLFM-5702
	@Test
	public void testEntityUpdateWithNewVersionAndTableType() {

		// Creates a spy so that we can mock some internal static method calls that
		// would fail
		EntityManagerImpl entityManagerSpy = Mockito.spy(entityManager);

		// We mock this call since the mock we use is not a real instance
		doNothing().when(entityManagerSpy).updateNodeAndAnnotationsFromEntity(any(), any(), any());

		when(mockNodeManager.getNode(any(), any())).thenReturn(new Node());
		when(mockNodeManager.getEntityPropertyAnnotations(any(), any())).thenReturn(new Annotations());

		String activityId = null;
		boolean newVersion = true;

		Table entity = Mockito.mock(Table.class);

		// call under test
		boolean newVersionCreated = entityManagerSpy.updateEntity(mockUser, entity, newVersion, activityId);

		// Despite the newVersion flag true, a new version for Table types cannot be
		// created this way
		assertFalse(newVersionCreated);
		verify(mockNodeManager).update(eq(mockUser), any(Node.class), any(Annotations.class), eq(false));
		verify(mockNodeManager, never()).getCurrentRevisionNumber(any());
	}

	@Test
	public void testBindSchemaToEntity() {
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockJsonSchemaManager.bindSchemaToObject(any(), any(), any(), any())).thenReturn(schemaBinding);

		// call under test
		JsonSchemaObjectBinding result = entityManagerSpy.bindSchemaToEntity(mockUser, schemaBindRequest);
		assertEquals(schemaBinding, result);
		verify(mockPermissionsManager).hasAccess("syn123", ACCESS_TYPE.UPDATE, mockUser);
		verify(mockJsonSchemaManager).bindSchemaToObject(mockUser.getId(), schemaBindRequest.getSchema$id(), 123L,
				BoundObjectType.entity);
		verify(entityManagerSpy).sendEntityUpdateNotifications("syn123");
	}

	@Test
	public void testBindSchemaToEntityWithUnauthorized() {
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
		verify(mockPermissionsManager).hasAccess("syn123", ACCESS_TYPE.UPDATE, mockUser);
		verify(mockJsonSchemaManager, never()).bindSchemaToObject(any(), any(), any(), any());
	}

	@Test
	public void testBindSchemaToEntityWithNullUser() {
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
	}

	@Test
	public void testBindSchemaToEntityWithNullRequest() {
		schemaBindRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
	}

	@Test
	public void testBindSchemaToEntityWithNullEntityId() {
		schemaBindRequest.setEntityId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
	}

	@Test
	public void testBindSchemaToEntityWithNullSchema$id() {
		schemaBindRequest.setSchema$id(null);
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
	}

	@Test
	public void testGetBoundSchema() {
		String entityId = "syn123";
		Long boundId = 456L;
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.findFirstBoundJsonSchema(any())).thenReturn(boundId);
		when(mockJsonSchemaManager.getJsonSchemaObjectBinding(any(), any())).thenReturn(schemaBinding);
		// call under test
		JsonSchemaObjectBinding result = entityManager.getBoundSchema(mockUser, entityId);
		assertNotNull(result);
		assertEquals(schemaBinding, result);
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verify(mockNodeManager).findFirstBoundJsonSchema(KeyFactory.stringToKey(entityId));
		verify(mockJsonSchemaManager).getJsonSchemaObjectBinding(boundId, BoundObjectType.entity);
	}

	@Test
	public void testGetBoundSchemaWithUnauthorized() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			entityManager.getBoundSchema(mockUser, entityId);
		});

		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verify(mockNodeManager, never()).findFirstBoundJsonSchema(any());
	}

	@Test
	public void testGetBoundSchemaWithNullEntityId() {
		String entityId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.getBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testGetBoundSchemaWithNullUserId() {
		String entityId = "syn123";
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.getBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testClearBoundSchema() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		// call under test
		entityManagerSpy.clearBoundSchema(mockUser, entityId);
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.DELETE, mockUser);
		verify(mockJsonSchemaManager).clearBoundSchema(123L, BoundObjectType.entity);
		verify(entityManagerSpy).sendEntityUpdateNotifications(entityId);
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithFile() {
		String entityId = "syn123";
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.file);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(any(), eq(ObjectType.ENTITY_CONTAINER), any());
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithFolder() {
		String entityId = "syn123";
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.folder);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithProject() {
		String entityId = "syn123";
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.folder);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}

	@Test
	public void testClearBoundSchemaWithUnauthorized() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.DELETE, mockUser);
		verify(mockJsonSchemaManager, never()).clearBoundSchema(any(), any());
	}

	@Test
	public void testClearBoundSchemaWithNullEntityId() {
		String entityId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testClearBoundSchemaWithNullUser() {
		String entityId = "syn123";
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testGetEntityJson() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		Project project = new Project();
		project.setId(entityId);
		doReturn(project).when(entityManagerSpy).getEntity(any(String.class), any());
		org.sagebionetworks.repo.model.annotation.v2.Annotations annos = new org.sagebionetworks.repo.model.annotation.v2.Annotations();
		when(mockNodeManager.getUserAnnotations(any())).thenReturn(annos);
		JSONObject jsonResult = new JSONObject();
		when(mockAnnotationTranslator.writeToJsonObject(any(), any())).thenReturn(jsonResult);
		// call under test
		JSONObject object = entityManagerSpy.getEntityJson(mockUser, entityId);
		assertNotNull(object);
		assertEquals(jsonResult, object);
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verify(entityManagerSpy).getEntity(entityId, null);
		verify(mockNodeManager).getUserAnnotations(entityId);
		verify(mockAnnotationTranslator).writeToJsonObject(project, annos);
	}

	@Test
	public void testGetEntityJsonWithUnauthrorized() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManagerSpy.getEntityJson(mockUser, entityId);
		});
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verifyNoMoreInteractions(mockNodeManager);
		verifyNoMoreInteractions(mockAnnotationTranslator);
	}
	
	@Test
	public void testGetEntityJsonWithNullUser() {
		String entityId = "syn123";
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManagerSpy.getEntityJson(mockUser, entityId);
		});
	}
	
	@Test
	public void testGetEntityJsonWithNullEntityId() {
		String entityId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManagerSpy.getEntityJson(mockUser, entityId);
		});
	}
	
	@Test
	public void testGetEntityJsonSubject() {
		String entityId = "syn123";
		Project project = new Project();
		project.setId(entityId);
		project.setEtag("some-etag");
		doReturn(project).when(entityManagerSpy).getEntity(any(String.class), any());
		org.sagebionetworks.repo.model.annotation.v2.Annotations annos = new org.sagebionetworks.repo.model.annotation.v2.Annotations();
		when(mockNodeManager.getUserAnnotations(any())).thenReturn(annos);
		JSONObject jsonResult = new JSONObject();
		when(mockAnnotationTranslator.writeToJsonObject(any(), any())).thenReturn(jsonResult);
		// call under test
		JsonSubject subject = entityManagerSpy.getEntityJsonSubject(entityId);
		assertNotNull(subject);
		assertEquals(project.getId(), subject.getObjectId());
		assertEquals(project.getEtag(), subject.getObjectEtag());
		assertEquals(org.sagebionetworks.repo.model.schema.ObjectType.entity, subject.getObjectType());
		verify(entityManagerSpy).getEntity(entityId, null);
		verify(mockNodeManager).getUserAnnotations(entityId);
		verify(mockAnnotationTranslator).writeToJsonObject(project, annos);
	}
	
	@Test
	public void testGetEntityJsonSubjectWithNullEntityId() {
		String entityId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.getEntityJsonSubject(entityId);
		});
		verifyNoMoreInteractions(mockNodeManager);
		verifyNoMoreInteractions(mockAnnotationTranslator);
	}
	
	@Test
	public void testUpdateEntityJson() {
		String entityId = "syn123";
		JSONObject inputJson = new JSONObject();
		JSONObject resultJson = new JSONObject();
		doReturn(resultJson).when(entityManagerSpy).getEntityJson(any());
		when(mockNodeManager.getNodeType(any(), any())).thenReturn(EntityType.project);
		org.sagebionetworks.repo.model.annotation.v2.Annotations annos = new org.sagebionetworks.repo.model.annotation.v2.Annotations();
		when(mockAnnotationTranslator.readFromJsonObject(any(), any())).thenReturn(annos);
		// call under test
		JSONObject result = entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		assertEquals(resultJson, result);
		verify(mockNodeManager).getNodeType(mockUser, entityId);
		verify(mockNodeManager).updateUserAnnotations(mockUser, entityId, annos);
		verify(mockAnnotationTranslator).readFromJsonObject(Project.class, inputJson);
	}
	
	@Test
	public void testUpdateEntityJsonWithNullUser() {
		mockUser = null;
		String entityId = "syn123";
		JSONObject inputJson = new JSONObject();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testUpdateEntityJsonWithNullEntityId() {
		String entityId = null;
		JSONObject inputJson = new JSONObject();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testUpdateEntityJsonWithNullJson() {
		String entityId = "syn123";
		JSONObject inputJson = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testGetEntityValidationResults() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		ValidationResults expected = new ValidationResults();
		expected.setObjectId(entityId);
		when(mockSchemaValidationResultDao.getValidationResults(any(), any())).thenReturn(expected);
		// call under test
		ValidationResults results = entityManager.getEntityValidationResults(mockUser, entityId);
		assertEquals(expected, results);
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verify(mockSchemaValidationResultDao).getValidationResults(entityId, org.sagebionetworks.repo.model.schema.ObjectType.entity);
	}
	
	@Test
	public void testGetEntityValidationResultsWithUnauthorized() {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, ()->{
			entityManager.getEntityValidationResults(mockUser, entityId);
		});
		verify(mockPermissionsManager).hasAccess(entityId, ACCESS_TYPE.READ, mockUser);
		verify(mockSchemaValidationResultDao, never()).getValidationResults(any(), any());
	}
	
	@Test
	public void testGetEntityValidationResultsWithNullUser() {
		String entityId = "syn123";
		mockUser = null;
		assertThrows(IllegalArgumentException.class, ()->{
			entityManager.getEntityValidationResults(mockUser, entityId);
		});
	}
	
	@Test
	public void testGetEntityValidationResultsWithNullEntityId() {
		String entityId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			entityManager.getEntityValidationResults(mockUser, entityId);
		});
	}
}
