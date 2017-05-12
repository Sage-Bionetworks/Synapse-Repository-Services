package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class NodeObjectRecordWriterTest {

	@Mock
	private NodeDAO mockNodeDAO;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AccessRequirementManager mockAccessRequirementManager;
	@Mock
	private EntityPermissionsManager mockEntityPermissionManager;
	private List<AccessRequirement> args;
	@Mock
	private UserEntityPermissions mockPermissions;
	@Mock
	private UserInfo mockUserInfo;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private ProgressCallback<Void> mockCallback;

	private NodeObjectRecordWriter writer;
	private NodeRecord node;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		writer = new NodeObjectRecordWriter();
		ReflectionTestUtils.setField(writer, "nodeDAO", mockNodeDAO);
		ReflectionTestUtils.setField(writer, "userManager", mockUserManager);
		ReflectionTestUtils.setField(writer, "accessRequirementManager", mockAccessRequirementManager);
		ReflectionTestUtils.setField(writer, "entityPermissionManager", mockEntityPermissionManager);
		ReflectionTestUtils.setField(writer, "objectRecordDAO", mockObjectRecordDao);

		node = new NodeRecord();
		node.setId("123");
		when(mockNodeDAO.getNode("123")).thenReturn(node);
		
		args = new ArrayList<>();

		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		when(mockAccessRequirementManager
				.getAllAccessRequirementsForSubject((UserInfo) Mockito.any(), (RestrictableObjectDescriptor) Mockito.any()))
				.thenReturn(args);
		when(mockEntityPermissionManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
	}

	@Test
	public void deleteChangeMessage() throws IOException {
		Long timestamp = System.currentTimeMillis();
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, timestamp);;
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(expected)), Mockito.eq(expected.getJsonClassName()));
	}

	@Test (expected=IllegalArgumentException.class)
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
	}

	@Test
	public void notPublicTest() throws IOException {
		// not restricted, not controlled
		args = new ArrayList<AccessRequirement>();
		// not public
		when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(false);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
	}

	@Test
	public void publicTest() throws IOException {
		// not restricted, not controlled
		args = new ArrayList<AccessRequirement>();
		// not public
		when(mockPermissions.getCanPublicRead()).thenReturn(true);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(true);
		node.setIsControlled(false);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
	}

	@Test
	public void restrictedTest() throws IOException {
		// Restricted
		args = new ArrayList<AccessRequirement>();
		args.add(new PostMessageContentAccessRequirement());
		when(mockAccessRequirementManager
				.getAllAccessRequirementsForSubject((UserInfo) Mockito.any(), (RestrictableObjectDescriptor) Mockito.any()))
				.thenReturn(args);
		// not public
		when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(false);
		node.setIsRestricted(true);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
	}

	@Test
	public void controlledTest() throws IOException {
		// controlled
		args = new ArrayList<AccessRequirement>();
		args.add(new ACTAccessRequirement());
		when(mockAccessRequirementManager
				.getAllAccessRequirementsForSubject((UserInfo) Mockito.any(), (RestrictableObjectDescriptor) Mockito.any()))
				.thenReturn(args);
		
		// not public
		when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(true);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
	}

	@Test
	public void publicRestrictedAndControlledTest() throws IOException {
		// controlled
		args = new ArrayList<AccessRequirement>();
		args.add(new ACTAccessRequirement());
		args.add(new TermsOfUseAccessRequirement());
		when(mockAccessRequirementManager
				.getAllAccessRequirementsForSubject((UserInfo) Mockito.any(), (RestrictableObjectDescriptor) Mockito.any()))
				.thenReturn(args);
		// not public
		when(mockPermissions.getCanPublicRead()).thenReturn(true);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(true);
		node.setIsControlled(true);
		node.setIsRestricted(true);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockCallback, times(2)).progressMade(null);
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
	}

	@Test
	public void buildNodeRecordTest() {
		Node node = new Node();
		node.setId("id");
		node.setParentId("parentId");
		node.setNodeType(EntityType.file);
		node.setCreatedOn(new Date(0));
		node.setCreatedByPrincipalId(1L);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(2L);
		node.setVersionNumber(3L);
		node.setFileHandleId("fileHandleId");
		node.setName("name");
		NodeRecord record = NodeObjectRecordWriter.buildNodeRecord(node,"benefactorId", "projectId");
		assertEquals(node.getId(), record.getId());
		assertEquals("benefactorId", record.getBenefactorId());
		assertEquals("projectId", record.getProjectId());
		assertEquals(node.getParentId(), record.getParentId());
		assertEquals(node.getNodeType(), record.getNodeType());
		assertEquals(node.getCreatedOn(), record.getCreatedOn());
		assertEquals(node.getCreatedByPrincipalId(), record.getCreatedByPrincipalId());
		assertEquals(node.getModifiedOn(), record.getModifiedOn());
		assertEquals(node.getModifiedByPrincipalId(), record.getModifiedByPrincipalId());
		assertEquals(node.getVersionNumber(), record.getVersionNumber());
		assertEquals(node.getFileHandleId(), record.getFileHandleId());
		assertEquals(node.getName(), record.getName());
	}

	@Test
	public void testNodeInTrashCan() throws IOException {
		EntityInTrashCanException exception = new EntityInTrashCanException("");
		when(mockPermissions.getCanPublicRead()).thenThrow(exception);

		Long timestamp = System.currentTimeMillis();
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, timestamp);;
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
		verify(mockCallback, times(2)).progressMade(null);
	}
}
