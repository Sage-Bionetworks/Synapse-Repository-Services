package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleAssociationManagerImplAutowireTest {

	@Autowired
	FileHandleAssociationManager fileHandleAssociationManager;
	
	@Test
	public void testAllTypes(){
		// Validate we get a provider for each type
		for(FileHandleAssociateType type: FileHandleAssociateType.values()){
			ObjectType objectType = fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type);
			assertNotNull(objectType);
		}
	}

	@Test
	public void testTypeMappingFileEntity(){
		FileHandleAssociateType type = FileHandleAssociateType.FileEntity;
		assertEquals(ObjectType.ENTITY, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingTableEntity(){
		FileHandleAssociateType type = FileHandleAssociateType.TableEntity;
		assertEquals(ObjectType.ENTITY, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingWikiAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.WikiAttachment;
		assertEquals(ObjectType.WIKI, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingWikiMarkdown(){
		FileHandleAssociateType type = FileHandleAssociateType.WikiMarkdown;
		assertEquals(ObjectType.WIKI, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserProfileAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.UserProfileAttachment;
		assertEquals(ObjectType.USER_PROFILE, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserMessageAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.MessageAttachment;
		assertEquals(ObjectType.MESSAGE, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserTeamAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.TeamAttachment;
		assertEquals(ObjectType.TEAM, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserSubmissionAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.SubmissionAttachment;
		assertEquals(ObjectType.EVALUATION_SUBMISSIONS, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserVerificationSubmission(){
		FileHandleAssociateType type = FileHandleAssociateType.VerificationSubmission;
		assertEquals(ObjectType.VERIFICATION_SUBMISSION, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
}
