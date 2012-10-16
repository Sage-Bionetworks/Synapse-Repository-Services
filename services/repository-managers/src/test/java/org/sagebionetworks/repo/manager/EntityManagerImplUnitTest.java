package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.backup.NodeBackupManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpMethod;

import com.amazonaws.services.securitytoken.model.Credentials;

public class EntityManagerImplUnitTest {

	private UserManager mockUserManager;
	private PermissionsManager mockPermissionsManager;
	private UserInfo mockUser;
	private EntityManagerImpl entityManager;
	private NodeManager mockNodeManager;
	private NodeBackupManager mockNodeBackupManager;
	private S3TokenManager mockS3TokenManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	String userId = "007";
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(PermissionsManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNodeManager = Mockito.mock(NodeManager.class);
		mockNodeBackupManager = Mockito.mock(NodeBackupManager.class);
		mockS3TokenManager = Mockito.mock(S3TokenManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockUser = new UserInfo(false);
		mockUser.setUser(new User());
		mockUser.getUser().setId(userId);
		entityManager = new EntityManagerImpl(mockNodeManager, mockNodeBackupManager, mockS3TokenManager, mockPermissionsManager, mockUserManager);
	}

	@Test (expected=UnauthorizedException.class)
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
		
	}
	
	@Test 
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
	}
	

	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentUrlNoReadAccess() throws Exception{
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = S3TokenManagerImpl.createAttachmentPathSlash(entityId, tokenId.toString());
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Simulate a 
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mocKLocationHelper.presignS3GETUrlShortLived(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		// Make the actual call
		PresignedUrl url = entityManager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectePreSigneUrl, url.getPresignedUrl());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3AttachmentTokenNoUpdateAccess() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName(null);
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
		when(mocKLocationHelper.createFederationTokenForS3(userId,HttpMethod.PUT,expectedPath)).thenReturn(mockCreds);
		when(mocKLocationHelper.presignS3PUTUrl(mockCreds, expectedPath, almostMd5, "image/jpeg")).thenReturn(expectePreSigneUrl);
		// Make the actual call
		S3AttachmentToken endToken = entityManager.createS3AttachmentToken(userId, entityId, startToken);
		assertNotNull(endToken);
		assertEquals(expectePreSigneUrl, endToken.getPresignedUrl());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testChangeEntityInvalid1() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String targetTypeName = "genomicdata";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName, "");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeEntityInvalid2() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String targetTypeName = "invalidtype";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName, "");
	}

	// TODO: Check md5 issues and re-enable
	@Ignore
	@Test
	public void testChangeEntityType() throws Exception {
		
		NodeRevisionBackup expectedNodeRevisionBackup1, expectedNodeRevisionBackup2, srcNodeRevisionBackup1, srcNodeRevisionBackup2;
		List<Long> expectedRevisionNums = new ArrayList<Long>();
		List<Long> srcRevisionNums = new ArrayList<Long>();
		List<NodeRevisionBackup> expectedListNodeRevisionBackups = new ArrayList<NodeRevisionBackup>();
		List<NodeRevisionBackup> srcListNodeRevisionBackups = new ArrayList<NodeRevisionBackup>();
		NamedAnnotations namedAnnots;
		Annotations annots;
		
		Node srcNode = new Node();
		srcNode.setDescription("Node description");
		srcNode.setETag("1");
		srcNode.setId("syn1000000");
		srcNode.setName("nodeName");
		srcNode.setNodeType("phenotypedata");
		srcNode.setVersionComment("This is version 1");
		srcNode.setVersionLabel("1.0.0");
		
		// TODO: Implement clone() function for node
		Node expectedNode = new Node();
		expectedNode.setDescription("Node description");
		expectedNode.setETag("1");
		expectedNode.setId("syn1000000");
		expectedNode.setName("nodeName");
		expectedNode.setNodeType("layer");
		expectedNode.setVersionComment("This is version 1");
		expectedNode.setVersionLabel("1.0.0");
		
		// TODO: Implement clone() function for node revision
		srcNodeRevisionBackup1 = new NodeRevisionBackup();
		srcNodeRevisionBackup1.setNodeId("syn1000000");
		srcNodeRevisionBackup1.setRevisionNumber(new Long(1));
		namedAnnots = new NamedAnnotations();
		srcNodeRevisionBackup1.setNamedAnnotations(namedAnnots);
		// These should stay in primary
		srcNodeRevisionBackup1.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("md5", "m55String");
		// These should stay in additional
		srcNodeRevisionBackup1.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1AStringKey", "v1AStringValue");
		// These should move to additional
		srcNodeRevisionBackup1.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("v1StringKey", "v1StringValue");
		srcNodeRevisionBackup1.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("v1LongKey", new Long(123));
		srcListNodeRevisionBackups.add(srcNodeRevisionBackup1);
		srcRevisionNums.add(new Long(1));
		
		srcNodeRevisionBackup2 = new NodeRevisionBackup();
		srcNodeRevisionBackup2.setNodeId("syn1000000");
		srcNodeRevisionBackup2.setRevisionNumber(new Long(2));	
		namedAnnots = new NamedAnnotations();
		srcNodeRevisionBackup2.setNamedAnnotations(namedAnnots);
		// These should stay in additional
		srcNodeRevisionBackup2.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1ADoubleKey", new Double(3.1416));
		// These should move to additional
		srcNodeRevisionBackup2.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("v2StringKey", "v2StringValue");
		srcNodeRevisionBackup2.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("v2LongKey", new Long(456));
		srcListNodeRevisionBackups.add(srcNodeRevisionBackup2);
		srcRevisionNums.add(new Long(2));
		
		expectedNodeRevisionBackup1 = new NodeRevisionBackup();
		expectedNodeRevisionBackup1.setNodeId("syn1000000");
		expectedNodeRevisionBackup1.setRevisionNumber(new Long(1));		
		namedAnnots = new NamedAnnotations();
		expectedNodeRevisionBackup1.setNamedAnnotations(namedAnnots);
		// These should stay in primary
		expectedNodeRevisionBackup1.getNamedAnnotations().getPrimaryAnnotations().addAnnotation("md5", "m55String");
		// These should stay in additional
		expectedNodeRevisionBackup1.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1AStringKey", "v1AStringValue");
		// These should move to additional
		expectedNodeRevisionBackup1.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1StringKey", "v1StringValue");
		expectedNodeRevisionBackup1.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1LongKey", new Long(123));
		expectedListNodeRevisionBackups.add(expectedNodeRevisionBackup1);
		expectedRevisionNums.add(new Long(1));
		
		expectedNodeRevisionBackup2 = new NodeRevisionBackup();
		expectedNodeRevisionBackup2.setNodeId("syn1000000");
		expectedNodeRevisionBackup2.setRevisionNumber(new Long(2));		
		namedAnnots = new NamedAnnotations();
		expectedNodeRevisionBackup2.setNamedAnnotations(namedAnnots);
		// These should stay in additional
		expectedNodeRevisionBackup2.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v1ADoubleKey", new Double(3.1416));
		expectedNodeRevisionBackup2.setNamedAnnotations(namedAnnots);
		expectedNodeRevisionBackup2.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v2StringKey", "v2StringValue");
		expectedNodeRevisionBackup2.getNamedAnnotations().getAdditionalAnnotations().addAnnotation("v2LongKey", new Long(456));
		expectedListNodeRevisionBackups.add(expectedNodeRevisionBackup2);
		expectedRevisionNums.add(new Long(2));
		
		NodeBackup srcNodeBackup = new NodeBackup();
		srcNodeBackup.setNode(srcNode);
		srcNodeBackup.setRevisions(srcRevisionNums);
		
		NodeBackup expectedNodeBackup = new NodeBackup();
		expectedNodeBackup.setNode(expectedNode);
		expectedNodeBackup.setRevisions(expectedRevisionNums);
				
		when(mockPermissionsManager.hasAccess("syn1000000", ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess("syn1000000", ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mockNodeBackupManager.getNode("syn1000000")).thenReturn(srcNodeBackup);
		when(mockNodeBackupManager.getNodeRevision("syn1000000", 1L)).thenReturn(srcNodeRevisionBackup1);
		when(mockNodeBackupManager.getNodeRevision("syn1000000", 2L)).thenReturn(srcNodeRevisionBackup2);
		entityManager.changeEntityType(mockUser, "syn1000000", "layer", "");
		verify(mockNodeBackupManager).createOrUpdateNodeWithRevisions(expectedNodeBackup, expectedListNodeRevisionBackups);
	}
	
}
