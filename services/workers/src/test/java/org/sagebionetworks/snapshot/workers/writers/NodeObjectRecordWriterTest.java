package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class NodeObjectRecordWriterTest {

	@Mock
	private NodeDAO mockNodeDAO;
	@Mock
	private DerivedAnnotationDao mockDerivedAnnotaionsDao;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private UserEntityPermissions mockPermissions;
	@Mock
	private UserInfo mockUserInfo;
	@Mock
	private AwsKinesisFirehoseLogger mockKinesisLogger;
	@Mock
	private ProgressCallback mockCallback;
	
	@InjectMocks
	private NodeObjectRecordWriter writer;
	
	private NodeRecord node;
	private AccessRequirementStats stats;
	private boolean canPublicRead;
	
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;

	@BeforeEach
	public void setup() {

		node = new NodeRecord();
		node.setId("123");
		node.setProjectId("1");
		
		stats = new AccessRequirementStats();
		stats.setHasACT(true);
		stats.setHasToU(false);
		stats.setRequirementIdSet(Set.of("2", "1", "3"));
		canPublicRead = true;

	}

	@Test
	public void deleteChangeMessage() throws IOException {
		Long timestamp = Instant.now().toEpochMilli();
		
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
		
		KinesisObjectSnapshotRecord<?> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, new NodeRecord().setId(nodeId));
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(List.of(expectedRecord), recordCaptor.getValue());
	}

	@Test
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
		
		verifyZeroInteractions(mockKinesisLogger);
	}

	@Test
	public void publicRestrictedAndControlledTest() throws IOException {
		when(mockNodeDAO.getNode("123")).thenReturn(node);
		when(mockNodeDAO.getProjectId("123")).thenReturn(Optional.of("1"));
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
		when(mockNodeDAO.getEntityPathIds(node.getId())).thenReturn(Arrays.asList(KeyFactory.stringToKey(node.getId())));
		when(mockAccessRequirementDao.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY))
				.thenReturn(stats);
		when(mockPermissions.getCanPublicRead()).thenReturn(canPublicRead);
		
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(canPublicRead);
		node.setIsControlled(stats.getHasACT());
		node.setIsRestricted(stats.getHasToU());
		node.setEffectiveArs(List.of(1L, 2L, 3L));
		
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(mockNodeDAO).getNode(eq("123"));
		
		KinesisObjectSnapshotRecord<NodeRecord> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, node);
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(List.of(expectedRecord), recordCaptor.getValue());
	}
	
	@Test
	public void testBuildAndWriteRecordsWithAnnotations() throws IOException {
		when(mockNodeDAO.getNode(any())).thenReturn(node);
		when(mockNodeDAO.getProjectId(any())).thenReturn(Optional.of("1"));
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(any(), any())).thenReturn(mockPermissions);
		when(mockAccessRequirementDao.getAccessRequirementStats(any(), any())).thenReturn(stats);
		
		Annotations baseAnnotations = AnnotationsV2Utils.emptyAnnotations();
		
		baseAnnotations.getAnnotations().put(
			"baseAnnotation", new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("1"))
		);
		
		Annotations derivedAnnotations = AnnotationsV2Utils.emptyAnnotations();
		
		derivedAnnotations.getAnnotations().put(
			"derivedAnnotations", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("value"))
		);
		
		when(mockDerivedAnnotaionsDao.getDerivedAnnotations(any())).thenReturn(Optional.of(baseAnnotations));
				
		when(mockNodeDAO.getUserAnnotations(any())).thenReturn(baseAnnotations);
		when(mockDerivedAnnotaionsDao.getDerivedAnnotations(any())).thenReturn(Optional.of(derivedAnnotations));
		
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		node.setIsPublic(false);
		node.setIsControlled(true);
		node.setIsRestricted(false);
		node.setEffectiveArs(List.of(1L, 2L, 3L));
		node.setAnnotations(baseAnnotations);
		node.setDerivedAnnotations(derivedAnnotations);
		
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockNodeDAO).getUserAnnotations("123");
		verify(mockDerivedAnnotaionsDao).getDerivedAnnotations("123");
		
		KinesisObjectSnapshotRecord<NodeRecord> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, node);
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(List.of(expectedRecord), recordCaptor.getValue());
	}
	
	@Test
	public void testBuildAndWriteRecordsWithEntityProperties() throws IOException {
		when(mockNodeDAO.getNode(any())).thenReturn(node);
		when(mockNodeDAO.getProjectId(any())).thenReturn(Optional.of("1"));
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(any(), any())).thenReturn(mockPermissions);
		when(mockAccessRequirementDao.getAccessRequirementStats(any(), any())).thenReturn(stats);
		
		org.sagebionetworks.repo.model.Annotations entityProperties = new org.sagebionetworks.repo.model.Annotations();
		entityProperties.addAnnotation("testString", "value");
		entityProperties.addAnnotation("testDate", new Date(123000L));
		entityProperties.addAnnotation("testDouble", 2.0);
		entityProperties.addAnnotation("testLong", 123L);
		entityProperties.addAnnotation("testObject", Boolean.TRUE);
		entityProperties.addAnnotation("testList", List.of("1", "2", "3"));
		
		when(mockNodeDAO.getEntityPropertyAnnotations(any())).thenReturn(entityProperties);
		
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		node.setIsPublic(false);
		node.setIsControlled(true);
		node.setIsRestricted(false);
		node.setEffectiveArs(List.of(1L, 2L, 3L));
		node.setInternalAnnotations(new Annotations().setAnnotations(Map.of(
			"testString", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("value")),
			"testDate", new AnnotationsValue().setType(AnnotationsValueType.TIMESTAMP_MS).setValue(List.of("123000")),
			"testDouble", new AnnotationsValue().setType(AnnotationsValueType.DOUBLE).setValue(List.of("2.0")),
			"testLong", new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("123")),
			"testObject", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("true")),
			"testList", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("1", "2", "3"))
		)));
		
		// Call under test
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockNodeDAO).getUserAnnotations("123");
		verify(mockDerivedAnnotaionsDao).getDerivedAnnotations("123");
		
		KinesisObjectSnapshotRecord<NodeRecord> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, node);
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(List.of(expectedRecord), recordCaptor.getValue());
	}

	@Test
	public void testNodeInTrashCan() throws IOException {
		when(mockNodeDAO.getProjectId("123")).thenReturn(Optional.of("1"));
		when(mockNodeDAO.getNode("123")).thenReturn(node);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
				
		EntityInTrashCanException exception = new EntityInTrashCanException("");
		when(mockPermissions.getCanPublicRead()).thenThrow(exception);

		Long timestamp = System.currentTimeMillis();
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
				
		KinesisObjectSnapshotRecord<?> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, new NodeRecord().setId(nodeId));
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(List.of(expectedRecord), recordCaptor.getValue());
	}
}
