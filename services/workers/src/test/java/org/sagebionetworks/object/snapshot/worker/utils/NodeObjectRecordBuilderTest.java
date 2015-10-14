package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class NodeObjectRecordBuilderTest {

	private NodeDAO mockNodeDAO;
	private NodeObjectRecordBuilder builder;
	private NodeRecord node;
	private UserManager mockUserManager;
	private AccessRequirementManager mockAccessRequirementManager;
	private EntityPermissionsManager mockEntityPermissionManager;
	private QueryResults<AccessRequirement> mockArs;
	private UserEntityPermissions mockPermissions;
	private UserInfo mockUserInfo;

	@Before
	public void setup() {
		mockNodeDAO = Mockito.mock(NodeDAO.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockAccessRequirementManager = Mockito.mock(AccessRequirementManager.class);
		mockEntityPermissionManager = Mockito.mock(EntityPermissionsManager.class);
		builder = new NodeObjectRecordBuilder(mockNodeDAO, mockUserManager,
				mockAccessRequirementManager, mockEntityPermissionManager);

		node = new NodeRecord();
		node.setId("123");
		Mockito.when(mockNodeDAO.getNode("123")).thenReturn(node);

		mockUserInfo = Mockito.mock(UserInfo.class);
		mockArs = Mockito.mock(QueryResults.class);
		mockPermissions = Mockito.mock(UserEntityPermissions.class);

		Mockito.when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		Mockito.when(mockAccessRequirementManager
				.getAccessRequirementsForSubject((UserInfo) Mockito.any(), (RestrictableObjectDescriptor) Mockito.any()))
				.thenReturn(mockArs);
		Mockito.when(mockEntityPermissionManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteChangeMessage() {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, "123", ObjectType.ENTITY, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test (expected=IllegalArgumentException.class)
	public void invalidObjectType() {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test
	public void notPublicTest() {
		// not restricted, not controlled
		List<AccessRequirement> ars = new ArrayList<AccessRequirement>();
		Mockito.when(mockArs.getResults()).thenReturn(ars);
		// not public
		Mockito.when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(false);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}

	@Test
	public void publicTest() {
		// not restricted, not controlled
		List<AccessRequirement> ars = new ArrayList<AccessRequirement>();
		Mockito.when(mockArs.getResults()).thenReturn(ars);
		// not public
		Mockito.when(mockPermissions.getCanPublicRead()).thenReturn(true);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(true);
		node.setIsControlled(false);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}

	@Test
	public void restrictedTest() {
		// Restricted
		List<AccessRequirement> ars = new ArrayList<AccessRequirement>();
		ars.add(new PostMessageContentAccessRequirement());
		Mockito.when(mockArs.getResults()).thenReturn(ars);
		// not public
		Mockito.when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(false);
		node.setIsRestricted(true);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}

	@Test
	public void controlledTest() {
		// controlled
		List<AccessRequirement> ars = new ArrayList<AccessRequirement>();
		ars.add(new ACTAccessRequirement());
		Mockito.when(mockArs.getResults()).thenReturn(ars);
		// not public
		Mockito.when(mockPermissions.getCanPublicRead()).thenReturn(false);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(false);
		node.setIsControlled(true);
		node.setIsRestricted(false);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}

	@Test
	public void publicRestrictedAndControlledTest() {
		// controlled
		List<AccessRequirement> ars = new ArrayList<AccessRequirement>();
		ars.add(new ACTAccessRequirement());
		ars.add(new TermsOfUseAccessRequirement());
		Mockito.when(mockArs.getResults()).thenReturn(ars);
		// not public
		Mockito.when(mockPermissions.getCanPublicRead()).thenReturn(true);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(true);
		node.setIsControlled(true);
		node.setIsRestricted(true);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}

	@Test
	public void buildNodeRecordTest() {
		Node node = new Node();
		node.setId("id");
		node.setBenefactorId("benefactorId");
		node.setProjectId("projectId");
		node.setParentId("parentId");
		node.setNodeType(EntityType.file);
		node.setCreatedOn(new Date(0));
		node.setCreatedByPrincipalId(1L);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(2L);
		node.setVersionNumber(3L);
		node.setFileHandleId("fileHandleId");
		node.setName("name");
		NodeRecord record = NodeObjectRecordBuilder.buildNodeRecord(node);
		assertEquals(node.getId(), record.getId());
		assertEquals(node.getBenefactorId(), record.getBenefactorId());
		assertEquals(node.getProjectId(), record.getProjectId());
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
}
