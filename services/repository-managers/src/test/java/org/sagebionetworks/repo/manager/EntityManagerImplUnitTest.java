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

import java.util.ArrayList;
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
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
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
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.web.NotFoundException;

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
	private EntityAclManager mockEntityAclManager;
	@Mock
	private EntityAuthorizationManager mockAuthorizationManger;
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
	EntitySchemaValidationResultDao mockEntitySchemaValidationResultDao;

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
	String entityId;

	@BeforeEach
	public void before() {
		entityId = "syn123";
		childRequest = new EntityChildrenRequest();
		childRequest.setParentId(entityId);
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
		schemaBindRequest.setEntityId(entityId);
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
		when(mockAuthorizationManger.hasAccess(mockUser, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			entityManager.validateReadAccess(mockUser, entityId);
		});

		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);

	}

	@Test
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException {
		String entityId = "abc";

		// Say now to this
		when(mockAuthorizationManger.hasAccess(mockUser, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());

		entityManager.validateReadAccess(mockUser, entityId);

		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
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
	public void testUpdateEntityAndNoNewVersionWithNewFileHandleId() throws Exception {
		// Mock dependencies.
		String id = "123";
		String parentId = "456";
		String sourceFileHandleId = "101";

		Node node = new Node();
		node.setFileHandleId(sourceFileHandleId);
		Annotations annos = new Annotations();
		
		when(mockNodeManager.getNode(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		
		boolean matchingMD5 = false;
		
		when(mockFileHandleManager.isMatchingMD5(any(), any())).thenReturn(matchingMD5);
		
		// Make file entity to update.
		FileEntity entity = new FileEntity();
		entity.setId(id);
		entity.setParentId(parentId);
		String targetFileHandleId = "202"; // i.e. we are updating from 101 to 202
		entity.setDataFileHandleId(targetFileHandleId);
		entity.setVersionComment("a comment for the original version");
		entity.setVersionLabel("a label for the original version");

		boolean newVersion = false; // even though new version is false the
		// modified file handle will trigger a version update since the MD5 does not match

		// method under test
		entityManager.updateEntity(mockUser, entity, newVersion, null);
		
		verify(mockNodeManager).getNode(mockUser, id);
		verify(mockNodeManager).getEntityPropertyAnnotations(mockUser, id);
		verify(mockFileHandleManager).isMatchingMD5(sourceFileHandleId, targetFileHandleId);
		verify(mockNodeManager).update(mockUser, node, annos, !newVersion);

		assertNull(node.getVersionComment());
		assertNull(node.getVersionLabel());
	}
	
	@Test
	public void testUpdateEntityAndNoNewVersionWithNewFileHandleIdSameMD5() throws Exception {
		// Mock dependencies.
		String id = "123";
		String parentId = "456";
		String sourceFileHandleId = "101";

		Node node = new Node();
		node.setFileHandleId(sourceFileHandleId);
		Annotations annos = new Annotations();
		
		when(mockNodeManager.getNode(mockUser, id)).thenReturn(node);
		when(mockNodeManager.getEntityPropertyAnnotations(mockUser, id)).thenReturn(annos);
		
		boolean matchingMD5 = true;
		
		when(mockFileHandleManager.isMatchingMD5(any(), any())).thenReturn(matchingMD5);
		
		// Make file entity to update.
		FileEntity entity = new FileEntity();
		entity.setId(id);
		entity.setParentId(parentId);
		String targetFileHandleId = "202"; // i.e. we are updating from 101 to 202
		entity.setDataFileHandleId(targetFileHandleId);
		entity.setVersionComment("a comment for the original version");
		entity.setVersionLabel("a label for the original version");

		boolean newVersion = false;

		// method under test
		entityManager.updateEntity(mockUser, entity, newVersion, null);
		
		verify(mockNodeManager).getNode(mockUser, id);
		verify(mockNodeManager).getEntityPropertyAnnotations(mockUser, id);
		verify(mockFileHandleManager).isMatchingMD5(sourceFileHandleId, targetFileHandleId);
		verify(mockNodeManager).update(mockUser, node, annos, newVersion);

		assertEquals(entity.getVersionComment(), node.getVersionComment());
		assertEquals(node.getVersionLabel(), node.getVersionLabel());
	}

	@Test
	public void testGetChildren() {

		when(mockAuthorizationManger.hasAccess(mockUser, childRequest.getParentId(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEntityAclManager.getNonvisibleChildren(mockUser, childRequest.getParentId()))
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
		verify(mockAuthorizationManger).hasAccess(mockUser, childRequest.getParentId(), ACCESS_TYPE.READ);
		verify(mockEntityAclManager).getNonvisibleChildren(mockUser, childRequest.getParentId());
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
		verify(mockAuthorizationManger, never()).hasAccess(any(UserInfo.class), anyString(), any(ACCESS_TYPE.class));
		verify(mockEntityAclManager).getNonvisibleChildren(mockUser, EntityManagerImpl.ROOT_ID);
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
		when(mockAuthorizationManger.hasAccess(mockUser, childRequest.getParentId(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManager.getChildren(mockUser, childRequest);
		});
	}

	@Test
	public void testGetChildrenNextPage() {

		when(mockAuthorizationManger.hasAccess(mockUser, childRequest.getParentId(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEntityAclManager.getNonvisibleChildren(mockUser, childRequest.getParentId()))
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
		when(mockAuthorizationManger.hasAccess(mockUser, request.getParentId(), ACCESS_TYPE.READ))
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
		when(mockAuthorizationManger.hasAccess(mockUser, request.getParentId(), ACCESS_TYPE.READ))
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
		when(mockAuthorizationManger.hasAccess(mockUser, request.getParentId(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockAuthorizationManger.hasAccess(mockUser, entityId, ACCESS_TYPE.READ))
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
		when(mockAuthorizationManger.hasAccess(mockUser, request.getParentId(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.lookupChild(request.getParentId(), request.getEntityName())).thenReturn(entityId);
		when(mockAuthorizationManger.hasAccess(mockUser, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}

	@Test
	public void testLookupChildWithNullParentId() {
		EntityLookupRequest request = new EntityLookupRequest();
		request.setEntityName("entityName");
		when(mockNodeManager.lookupChild(EntityManagerImpl.ROOT_ID, request.getEntityName())).thenReturn(entityId);
		when(mockAuthorizationManger.hasAccess(mockUser, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		EntityId result = entityManager.lookupChild(mockUser, request);
		assertNotNull(result);
		assertEquals(entityId, result.getId());
	}

	@Test
	public void testChangeEntityDataType() {
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
		project.setId(entityId);
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
		project.setId(entityId);
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
		project.setId(entityId);
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
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockJsonSchemaManager.bindSchemaToObject(any(), any(), any(), any())).thenReturn(schemaBinding);

		// call under test
		JsonSchemaObjectBinding result = entityManagerSpy.bindSchemaToEntity(mockUser, schemaBindRequest);
		assertEquals(schemaBinding, result);
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.UPDATE);
		verify(mockJsonSchemaManager).bindSchemaToObject(mockUser.getId(), schemaBindRequest.getSchema$id(), 123L,
				BoundObjectType.entity);
		verify(entityManagerSpy).sendEntityUpdateNotifications(entityId);
	}

	@Test
	public void testBindSchemaToEntityWithUnauthorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, () -> {
			entityManager.bindSchemaToEntity(mockUser, schemaBindRequest);
		});
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.UPDATE);
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
		Long boundId = 456L;
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeManager.findFirstBoundJsonSchema(any())).thenReturn(boundId);
		when(mockJsonSchemaManager.getJsonSchemaObjectBinding(any(), any())).thenReturn(schemaBinding);
		// call under test
		JsonSchemaObjectBinding result = entityManager.getBoundSchema(mockUser, entityId);
		assertNotNull(result);
		assertEquals(schemaBinding, result);
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(mockNodeManager).findFirstBoundJsonSchema(KeyFactory.stringToKey(entityId));
		verify(mockJsonSchemaManager).getJsonSchemaObjectBinding(boundId, BoundObjectType.entity);
	}

	@Test
	public void testGetBoundSchemaWithUnauthorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			entityManager.getBoundSchema(mockUser, entityId);
		});

		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
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
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entityManager.getBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testClearBoundSchema() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		// call under test
		entityManagerSpy.clearBoundSchema(mockUser, entityId);
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.DELETE);
		verify(mockJsonSchemaManager).clearBoundSchema(123L, BoundObjectType.entity);
		verify(entityManagerSpy).sendEntityUpdateNotifications(entityId);
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithFile() {
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.file);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(any(), eq(ObjectType.ENTITY_CONTAINER), any());
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithFolder() {
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.folder);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testSendEntityUpdateNotificationsWithProject() {
		when(mockNodeManager.getNodeType(entityId)).thenReturn(EntityType.folder);
		// call under test
		entityManager.sendEntityUpdateNotifications(entityId);
		verify(mockNodeManager).getNodeType(entityId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}

	@Test
	public void testClearBoundSchemaWithUnauthorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.DELETE);
		verify(mockJsonSchemaManager, never()).clearBoundSchema(any(), any());
	}

	@Test
	public void testClearBoundSchemaWithNullEntityId() {
		entityId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testClearBoundSchemaWithNullUser() {
		mockUser = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityManager.clearBoundSchema(mockUser, entityId);
		});
	}

	@Test
	public void testGetEntityJson() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(entityManagerSpy).getEntity(entityId, null);
		verify(mockNodeManager).getUserAnnotations(entityId);
		verify(mockAnnotationTranslator).writeToJsonObject(project, annos);
	}

	@Test
	public void testGetEntityJsonWithUnauthrorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityManagerSpy.getEntityJson(mockUser, entityId);
		});
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verifyNoMoreInteractions(mockNodeManager);
		verifyNoMoreInteractions(mockAnnotationTranslator);
	}
	
	@Test
	public void testGetEntityJsonWithNullUser() {
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
		JSONObject inputJson = new JSONObject();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testUpdateEntityJsonWithNullEntityId() {
		entityId = null;
		JSONObject inputJson = new JSONObject();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testUpdateEntityJsonWithNullJson() {
		JSONObject inputJson = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.updateEntityJson(mockUser, entityId, inputJson);
		});
	}
	
	@Test
	public void testGetEntityValidationResults() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		ValidationResults expected = new ValidationResults();
		expected.setObjectId(entityId);
		when(mockEntitySchemaValidationResultDao.getValidationResults(any())).thenReturn(expected);
		// call under test
		ValidationResults results = entityManager.getEntityValidationResults(mockUser, entityId);
		assertEquals(expected, results);
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(mockEntitySchemaValidationResultDao).getValidationResults(entityId);
	}
	
	@Test
	public void testGetEntityValidationResultsWithUnauthorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, ()->{
			entityManager.getEntityValidationResults(mockUser, entityId);
		});
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(mockEntitySchemaValidationResultDao, never()).getValidationResults(any());
	}
	
	@Test
	public void testGetEntityValidationResultsWithNullUser() {
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
	
	@Test
	public void testAuthorizedListChildren() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		Set<Long> nonVisible = Sets.newHashSet(543L);
		when(mockEntityAclManager.getNonvisibleChildren(any(), any())).thenReturn(nonVisible);
		// call under test
		Set<Long> results = entityManager.authorizedListChildren(mockUser, entityId);
		assertEquals(nonVisible, results);
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(mockEntityAclManager).getNonvisibleChildren(mockUser, entityId);
	}
	
	@Test
	public void testAuthorizedListChildrenWithUnauthorized() {
		when(mockAuthorizationManger.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied("no access"));
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			entityManager.authorizedListChildren(mockUser, entityId);
		});
		verify(mockAuthorizationManger).hasAccess(mockUser, entityId, ACCESS_TYPE.READ);
		verify(mockEntityAclManager, never()).getNonvisibleChildren(any(), any());
	}
	
	
	@Test
	public void testAuthorizedListChildrenWithRootId() {
		entityId = NodeUtils.ROOT_ENTITY_ID;
		Set<Long> nonVisible = Sets.newHashSet(543L);
		when(mockEntityAclManager.getNonvisibleChildren(any(), any())).thenReturn(nonVisible);
		// call under test
		Set<Long> results = entityManager.authorizedListChildren(mockUser, entityId);
		assertEquals(nonVisible, results);
		verify(mockAuthorizationManger, never()).hasAccess(any(), any(), any(ACCESS_TYPE.class));
		verify(mockEntityAclManager).getNonvisibleChildren(mockUser, entityId);
	}
	
	@Test
	public void testGetEntityValidationStatistics() {
		Set<Long> nonVisibleChildren = Sets.newHashSet(111L,222L);
		doReturn(nonVisibleChildren).when(entityManagerSpy).authorizedListChildren(any(), any());
		ValidationSummaryStatistics stats = new ValidationSummaryStatistics();
		stats.setContainerId(entityId);
		when(mockEntitySchemaValidationResultDao.getEntityValidationStatistics(any(), any())).thenReturn(stats);
		// call under test
		ValidationSummaryStatistics results = entityManagerSpy.getEntityValidationStatistics(mockUser, entityId);
		assertEquals(stats, results);
		verify(entityManagerSpy).authorizedListChildren(mockUser, entityId);
		verify(mockEntitySchemaValidationResultDao).getEntityValidationStatistics(entityId, nonVisibleChildren);
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationResults() {
		Set<Long> nonVisibleChildren = Sets.newHashSet(111L, 222L);
		doReturn(nonVisibleChildren).when(entityManagerSpy).authorizedListChildren(any(), any());
		List<ValidationResults> page = createValidationResultsListOfSize(5);
		when(mockEntitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(any(), any(), anyLong(), anyLong()))
				.thenReturn(page);
		ListValidationResultsRequest request = new ListValidationResultsRequest();
		request.setContainerId(entityId);

		// call under test
		ListValidationResultsResponse response = entityManagerSpy.getInvalidEntitySchemaValidationResults(mockUser,
				request);
		assertNotNull(response);
		assertEquals(page, response.getPage());
		assertNull(response.getNextPageToken());
		verify(entityManagerSpy).authorizedListChildren(mockUser, entityId);
		long expectedLimit = NextPageToken.DEFAULT_LIMIT+1;
		long expectedOffset = 0L;
		verify(mockEntitySchemaValidationResultDao).getInvalidEntitySchemaValidationPage(entityId, nonVisibleChildren,
				expectedLimit, expectedOffset);
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationResultsWithReturnedNextPageToken() {
		Set<Long> nonVisibleChildren = Sets.newHashSet(111L, 222L);
		doReturn(nonVisibleChildren).when(entityManagerSpy).authorizedListChildren(any(), any());
		// first page results includes one more item than the default limit.
		List<ValidationResults> page = createValidationResultsListOfSize((int) (NextPageToken.DEFAULT_LIMIT+1));
		when(mockEntitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(any(), any(), anyLong(), anyLong()))
				.thenReturn(page);
		ListValidationResultsRequest request = new ListValidationResultsRequest();
		request.setContainerId(entityId);

		// call under test
		ListValidationResultsResponse response = entityManagerSpy.getInvalidEntitySchemaValidationResults(mockUser,
				request);
		assertNotNull(response);
		assertEquals(page, response.getPage());
		String expectedNextPage = new NextPageToken(NextPageToken.DEFAULT_LIMIT, NextPageToken.DEFAULT_LIMIT).toToken();
		assertEquals(expectedNextPage, response.getNextPageToken());
		verify(entityManagerSpy).authorizedListChildren(mockUser, entityId);
		long expectedLimit = NextPageToken.DEFAULT_LIMIT+1;
		long expectedOffset = 0L;
		verify(mockEntitySchemaValidationResultDao).getInvalidEntitySchemaValidationPage(entityId, nonVisibleChildren,
				expectedLimit, expectedOffset);
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationResultsWithInputNextPage() {
		Set<Long> nonVisibleChildren = Sets.newHashSet(111L, 222L);
		doReturn(nonVisibleChildren).when(entityManagerSpy).authorizedListChildren(any(), any());
		List<ValidationResults> page = createValidationResultsListOfSize(2);
		when(mockEntitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(any(), any(), anyLong(), anyLong()))
				.thenReturn(page);
		ListValidationResultsRequest request = new ListValidationResultsRequest();
		request.setContainerId(entityId);
		long inputLimit = 25;
		long inputOffset = 250;
		request.setNextPageToken( new NextPageToken(inputLimit, inputOffset).toToken());

		// call under test
		ListValidationResultsResponse response = entityManagerSpy.getInvalidEntitySchemaValidationResults(mockUser,
				request);
		assertNotNull(response);
		assertEquals(page, response.getPage());
		String expectedNextPage = null;
		assertEquals(expectedNextPage, response.getNextPageToken());
		verify(entityManagerSpy).authorizedListChildren(mockUser, entityId);
		long expectedLimit = inputLimit+1;
		long expectedOffset = inputOffset;
		verify(mockEntitySchemaValidationResultDao).getInvalidEntitySchemaValidationPage(entityId, nonVisibleChildren,
				expectedLimit, expectedOffset);
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationResultsWithNullRequest() {
		ListValidationResultsRequest request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.getInvalidEntitySchemaValidationResults(mockUser, request);
		});
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationResultsWithNullContainerId() {
		ListValidationResultsRequest request = new ListValidationResultsRequest();
		request.setContainerId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityManagerSpy.getInvalidEntitySchemaValidationResults(mockUser, request);
		});
	}
	
	@Test
	public void testUpdateEntityFileHandle() {
		Long versionNumber = 1L;
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		updateRequest.setOldFileHandleId("123");
		updateRequest.setNewFileHandleId("456");
		
		doNothing().when(mockNodeManager).updateNodeFileHandle(any(), any(), any(), any());
		
		// Call under test
		entityManager.updateEntityFileHandle(mockUser, entityId, versionNumber, updateRequest);
		
		verify(mockNodeManager).updateNodeFileHandle(mockUser, entityId, versionNumber, updateRequest);
	}
	
	static public List<ValidationResults> createValidationResultsListOfSize(int size){
		List<ValidationResults> list = new ArrayList<ValidationResults>(size);
		for(int i=0; i<size; i++) {
			ValidationResults r = new ValidationResults();
			r.setObjectId(Integer.toString(i));
			r.setObjectType(org.sagebionetworks.repo.model.schema.ObjectType.entity);
			list.add(r);
		}
		return list;
	}
	
}
