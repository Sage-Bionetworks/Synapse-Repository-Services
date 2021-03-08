package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleAssociationManagerImplAutowireTest {
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private FileHandleAssociationManager fileHandleAssociationManager;
	
	private UserInfo adminUserInfo;
	private List<String> fileHandlesToDelete;
	private List<String> entitiesToDelete;
	
	private FileHandleAssociateType associationType;
	private String associateObjectId;
	private String fileHandleId;
	private String fileHandlePreviewId;
	
	@BeforeEach
	public void before(){
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		fileHandlesToDelete = new LinkedList<>();
		entitiesToDelete = new LinkedList<>();
		
		S3FileHandle fileHandle = TestUtils.createS3FileHandle(adminUserInfo.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle fileHandlePreview = TestUtils.createPreviewFileHandle(adminUserInfo.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		
		fileHandleToCreate.add(fileHandle);
		fileHandleToCreate.add(fileHandlePreview);
		
		fileHandleDao.createBatch(fileHandleToCreate);
		
		fileHandle = (S3FileHandle) fileHandleDao.get(fileHandle.getId());
		fileHandlePreview = (S3FileHandle) fileHandleDao.get(fileHandlePreview.getId());
		
		fileHandleDao.setPreviewId(fileHandle.getId(), fileHandlePreview.getId());

		fileHandlesToDelete.add(fileHandle.getId());
		fileHandlesToDelete.add(fileHandlePreview.getId());
		
		FileEntity file = new FileEntity();
		
		file.setDataFileHandleId(fileHandle.getId());

		file.setName("testFileEntity");
		
		String id = entityManager.createEntity(adminUserInfo, file, null);
		
		entitiesToDelete.add(id);
		
		associationType = FileHandleAssociateType.FileEntity;
		associateObjectId = id;
		fileHandleId = fileHandle.getId();
		fileHandlePreviewId = fileHandlePreview.getId();
		
	}
	
	@AfterEach
	public void after(){
		if (entitiesToDelete != null) {
			for (String id : entitiesToDelete) {
				entityManager.deleteEntity(adminUserInfo, id);
			}
		}
		if(fileHandlesToDelete != null){
			for(String id: fileHandlesToDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
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
	public void testTypeMappingEvaluationSubmissionAttachment(){
		FileHandleAssociateType type = FileHandleAssociateType.SubmissionAttachment;
		assertEquals(ObjectType.EVALUATION_SUBMISSIONS, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testTypeMappingUserVerificationSubmission(){
		FileHandleAssociateType type = FileHandleAssociateType.VerificationSubmission;
		assertEquals(ObjectType.VERIFICATION_SUBMISSION, fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(type));
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		
		List<String> allFileHandleIds = Arrays.asList(fileHandleId);
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(allFileHandleIds, associateObjectId, associationType);
		
		assertEquals(new HashSet<>(allFileHandleIds), result);
		
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithPreview() {
		
		List<String> allFileHandleIds = Arrays.asList(fileHandleId, fileHandlePreviewId);
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(allFileHandleIds, associateObjectId, associationType);
		
		assertEquals(new HashSet<>(allFileHandleIds), result);
		
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectOnlyPreview() {
		
		List<String> allFileHandleIds = Arrays.asList(fileHandlePreviewId);
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(allFileHandleIds, associateObjectId, associationType);
		
		assertEquals(new HashSet<>(allFileHandleIds), result);
		
	}
	
	@Test
	public void testGetIdRange() {
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			
			IdRange idRange = fileHandleAssociationManager.getIdRange(type);
			
			assertNotNull(idRange);
		}
	}
	
	@Test
	public void testScanRange() {
		
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			IdRange idRange = fileHandleAssociationManager.getIdRange(type);
			
			Iterator<ScannedFileHandleAssociation> it = fileHandleAssociationManager.scanRange(associationType, idRange).iterator();
			
			assertNotNull(it);
			
			if (it.hasNext()) {
				ScannedFileHandleAssociation association = it.next();
				
				assertNotNull(association);
			}
		}
	}
}

