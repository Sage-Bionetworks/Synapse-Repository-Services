package org.sagebionetworks.repo.web.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.StorageQuotaManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.StorageQuotaAdminDao;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.ListBucketProvider;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationUtils;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.DispatchServletSingleton;
import org.sagebionetworks.repo.web.controller.EntityServletTestHelper;
import org.sagebionetworks.repo.web.controller.ServletTestHelper;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is an integration test to test the migration of all tables from start to finish.
 * 
 * The test does the following:
 * 1. the before() method creates at least one object for every type object that must migrate.
 * 2. Create a backup copy of all data.
 * 3. Delete all data in the system.
 * 4. Restore all data from the backup.
 * 
 * NOTE: Whenever a new migration type is added this test must be extended to test that objects migration.
 * 
 * 
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationIntegrationAutowireTest {
	
	public static final long MAX_WAIT_MS = 10*1000; // 10 sec.
	
	@Autowired
	private EntityServletTestHelper entityServletHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private ServiceProvider serviceProvider;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	private MigrationManager migrationManager;
	
	@Autowired
	private StorageQuotaManager storageQuotaManager;
	
	@Autowired
	private StorageQuotaAdminDao storageQuotaAdminDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private AuthenticationDAO authDAO;

	@Autowired
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;

	@Autowired
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;

	UserInfo userInfo;
	private String userName;
	private String adminId;
	
	// To delete
	List<String> entityToDelete;
	List<WikiPageKey> wikiToDelete;
	List<WikiPageKey> v2WikiToDelete;
	List<String> fileHandlesToDelete;
	// Activity
	Activity activity;
	
	// Entities
	Project project;
	FileEntity fileEntity;
	Folder folderToTrash;
	// requirement
	AccessRequirement accessRequirement;
	// approval
	AccessApproval accessApproval;
	
	// Wiki pages
	WikiPage rootWiki;
	WikiPage subWiki;
	WikiPageKey rootWikiKey;
	WikiPageKey subWikiKey;
	
	// V2 Wiki page
	V2WikiPage v2RootWiki;
	V2WikiPage v2SubWiki;
	WikiPageKey v2RootWikiKey;
	WikiPageKey v2SubWikiKey;

	// File Handles
	S3FileHandle handleOne;
	S3FileHandle markdownOne;
	PreviewFileHandle preview;
	
	// Evaluation
	Evaluation evaluation;
	Participant participant;
	Submission submission;
	SubmissionStatus submissionStatus;
	// Doi
	Doi doi;
	// Favorite
	Favorite favorite;
	
	HttpServletRequest mockRequest;
	
	@Before
	public void before() throws Exception{
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		// get user IDs
		userName = AuthorizationConstants.MIGRATION_USER_NAME;
		userInfo = userManager.getUserInfo(userName);
		adminId = userInfo.getIndividualGroup().getId();
		resetDatabase();
		createFileHandles();
		createActivity();
		createEntities();
		createFavorite();
		createEvaluation();
		createAccessRequirement();
		createAccessApproval();
		creatWikiPages();
		createV2WikiPages();
		createDoi();
		createStorageQuota();
		UserGroup sampleGroup = createUserGroups();
		createTeamsRequestsAndInvitations(sampleGroup);
		createCredentials(sampleGroup);
	}


	private void resetDatabase() throws Exception {
		// Before we start this test we want to start with a clean database
		migrationManager.deleteAllData(userInfo);
		// bootstrap to put back the bootstrap data
		entityBootstrapper.bootstrapAll();
		storageQuotaAdminDao.clear();
	}


	private void createFavorite() {
		favorite =  userProfileManager.addFavorite(userInfo, fileEntity.getId());
	}


	private void createDoi() throws Exception {
		doi = serviceProvider.getDoiService().createDoi(userName, project.getId(), ObjectType.ENTITY, 1L);
	}


	private void createActivity() throws Exception {
		activity = new Activity();
		activity.setDescription("some desc");
		activity = serviceProvider.getActivityService().createActivity(userName, activity);
	}


	private void createEvaluation() throws Exception {
		// initialize Evaluations
		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setDescription("description");
		evaluation.setContentSource(project.getId());
		evaluation.setStatus(EvaluationStatus.PLANNED);
		evaluation.setSubmissionInstructionsMessage("instructions");
		evaluation.setSubmissionReceiptMessage("receipt");
		evaluation = serviceProvider.getEvaluationService().createEvaluation(userName, evaluation);	
		evaluation = new Evaluation();
		evaluation.setName("name2");
		evaluation.setDescription("description");
		evaluation.setContentSource(project.getId());
		evaluation.setStatus(EvaluationStatus.OPEN);		
		evaluation.setSubmissionInstructionsMessage("instructions");
		evaluation.setSubmissionReceiptMessage("receipt");
        evaluation = serviceProvider.getEvaluationService().createEvaluation(userName, evaluation);

        // initialize Participants
		participant = serviceProvider.getEvaluationService().addParticipant(userName, evaluation.getId());
        
        // initialize Submissions
		submission = new Submission();
		submission.setName("submission1");
		submission.setVersionNumber(1L);
		submission.setEntityId(fileEntity.getId());
		submission.setUserId(userName);
		submission.setEvaluationId(evaluation.getId());
		submission = entityServletHelper.createSubmission(submission, userName, fileEntity.getEtag());
		submissionStatus = serviceProvider.getEvaluationService().getSubmissionStatus(userName, submission.getId());
	}


	public void createAccessApproval() throws Exception {
		accessApproval = newToUAccessApproval(accessRequirement.getId(), adminId);
		accessApproval = ServletTestHelper.createAccessApproval(
				DispatchServletSingleton.getInstance(), accessApproval, userName, new HashMap<String, String>());
	}


	public void createAccessRequirement() throws Exception {
		// Add an access requirement to this entity
		accessRequirement = newAccessRequirement();
		String entityId = project.getId();
		RestrictableObjectDescriptor entitySubjectId = new RestrictableObjectDescriptor();
		entitySubjectId.setId(entityId);
		entitySubjectId.setType(RestrictableObjectType.ENTITY);
		RestrictableObjectDescriptor evaluationSubjectId = new RestrictableObjectDescriptor();
		assertNotNull(evaluation);
		assertNotNull(evaluation.getId());
		evaluationSubjectId.setId(evaluation.getId());
		evaluationSubjectId.setType(RestrictableObjectType.EVALUATION);
		
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{entitySubjectId, evaluationSubjectId})); 
		accessRequirement = ServletTestHelper.createAccessRequirement(DispatchServletSingleton.getInstance(), accessRequirement, userName, new HashMap<String, String>());
	}
	
	private TermsOfUseAccessApproval newToUAccessApproval(Long requirementId, String accessorId) {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(requirementId);
		return aa;
	}


	public void creatWikiPages() throws Exception {
		wikiToDelete = new LinkedList<WikiPageKey>();
		// Create a wiki page
		rootWiki = new WikiPage();
		rootWiki.setAttachmentFileHandleIds(new LinkedList<String>());
		rootWiki.getAttachmentFileHandleIds().add(handleOne.getId());
		rootWiki.setTitle("Root title");
		rootWiki.setMarkdown("Root markdown");
		rootWiki = serviceProvider.getWikiService().createWikiPage(userName, fileEntity.getId(), ObjectType.ENTITY, rootWiki);
		rootWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, rootWiki.getId());
		wikiToDelete.add(rootWikiKey);
		
		subWiki = new WikiPage();
		subWiki.setParentWikiId(rootWiki.getId());
		subWiki.setTitle("Sub-wiki-title");
		subWiki.setMarkdown("sub-wiki markdown");
		subWiki = serviceProvider.getWikiService().createWikiPage(userName, fileEntity.getId(), ObjectType.ENTITY, subWiki);
		subWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, subWiki.getId());
	}
	
	public void createV2WikiPages() throws NotFoundException {
		// Using wikiPageDao until wiki service is created
		
		v2WikiToDelete = new LinkedList<WikiPageKey>();
		// Create a V2 Wiki page
		v2RootWiki = new V2WikiPage();
		v2RootWiki.setCreatedBy(adminId);
		v2RootWiki.setModifiedBy(adminId);
		v2RootWiki.setAttachmentFileHandleIds(new LinkedList<String>());
		v2RootWiki.getAttachmentFileHandleIds().add(handleOne.getId());
		v2RootWiki.setTitle("Root title");
		v2RootWiki.setMarkdownFileHandleId(markdownOne.getId());
		
		Map<String, FileHandle> map = new HashMap<String, FileHandle>();
		map.put(handleOne.getFileName(), handleOne);
		v2RootWiki = wikiPageDao.create(v2RootWiki, map, fileEntity.getId(), ObjectType.ENTITY);
		v2RootWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, v2RootWiki.getId());
		wikiToDelete.add(v2RootWikiKey);
		
		// Create a child
		v2SubWiki = new V2WikiPage();
		v2SubWiki.setCreatedBy(adminId);
		v2SubWiki.setModifiedBy(adminId);
		v2SubWiki.setParentWikiId(v2RootWiki.getId());
		v2SubWiki.setTitle("V2 Sub-wiki-title");
		v2SubWiki.setMarkdownFileHandleId(markdownOne.getId());
		v2SubWiki = wikiPageDao.create(v2SubWiki, new HashMap<String, FileHandle>(), fileEntity.getId(), ObjectType.ENTITY);
		v2SubWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, v2SubWiki.getId()); 
	}


	/**
	 * Create the entities used by this test.
	 * @throws JSONObjectAdapterException
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void createEntities() throws JSONObjectAdapterException,
			ServletException, IOException, NotFoundException {
		entityToDelete = new LinkedList<String>();
		// Create a project
		project = new Project();
		project.setName("MigrationIntegrationAutowireTest.Project");
		project.setEntityType(Project.class.getName());
		project = serviceProvider.getEntityService().createEntity(userName, project, null, mockRequest);
		entityToDelete.add(project.getId());
		
		// Create a file entity
		fileEntity = new FileEntity();
		fileEntity.setName("MigrationIntegrationAutowireTest.FileEntity");
		fileEntity.setEntityType(FileEntity.class.getName());
		fileEntity.setParentId(project.getId());
		fileEntity.setDataFileHandleId(handleOne.getId());
		fileEntity = serviceProvider.getEntityService().createEntity(userName, fileEntity, activity.getId(),mockRequest);
		
		// Create a folder to trash
		folderToTrash = new Folder();
		folderToTrash.setName("boundForTheTrashCan");
		folderToTrash.setParentId(project.getId());
		folderToTrash = serviceProvider.getEntityService().createEntity(userName, folderToTrash, null, mockRequest);
		// Send it to the trash can
		serviceProvider.getTrashService().moveToTrash(userName, folderToTrash.getId());
	}
	
	private AccessRequirement newAccessRequirement() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setEntityType(dto.getClass().getName());
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);
		dto.setTermsOfUse("foo");
		return dto;
	}


	/**
	 * Create the file handles used by this test.
	 * @throws NotFoundException
	 */
	public void createFileHandles() throws NotFoundException {
		fileHandlesToDelete = new LinkedList<String>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create markdown content
		markdownOne = new S3FileHandle();
		markdownOne.setCreatedBy(adminId);
		markdownOne.setCreatedOn(new Date());
		markdownOne.setBucketName("bucket");
		markdownOne.setKey("markdownFileKey");
		markdownOne.setEtag("etag");
		markdownOne.setFileName("markdown1");
		markdownOne = fileMetadataDao.createFile(markdownOne);
		// Create a preview
		preview = new PreviewFileHandle();
		preview.setCreatedBy(adminId);
		preview.setCreatedOn(new Date());
		preview.setBucketName("bucket");
		preview.setKey("previewFileKey");
		preview.setEtag("etag");
		preview.setFileName("bar.txt");
		preview = fileMetadataDao.createFile(preview);
		fileHandlesToDelete.add(preview.getId());
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), preview.getId());
		fileHandlesToDelete.add(handleOne.getId());
	}

	private void createStorageQuota() {
		storageQuotaManager.setQuotaForUser(userInfo, userInfo, 3000);
	}
	
	// returns a group for use in a team
	private UserGroup createUserGroups() throws NotFoundException {
		String groupNamePrefix = "Caravan-";
		String userNamePrefix = "GoinOnTheOregonTrail@";
		List<String> adder = new ArrayList<String>();
		
		// Make two groups
		UserGroup parentGroup = new UserGroup();
		parentGroup.setIsIndividual(false);
		parentGroup.setName(groupNamePrefix+"1");
		parentGroup.setId(userGroupDAO.create(parentGroup));
		
		UserGroup nestedGroup = new UserGroup();
		nestedGroup.setIsIndividual(false);
		nestedGroup.setName(groupNamePrefix+"2");
		nestedGroup.setId(userGroupDAO.create(nestedGroup));
		
		// Make three users
		UserGroup parentUser = new UserGroup();
		parentUser.setIsIndividual(true);
		parentUser.setName(userNamePrefix+"1");
		parentUser.setId(userGroupDAO.create(parentUser));
		
		UserGroup siblingUser = new UserGroup();
		siblingUser.setIsIndividual(true);
		siblingUser.setName(userNamePrefix+"2");
		siblingUser.setId(userGroupDAO.create(siblingUser));
		
		UserGroup childUser = new UserGroup();
		childUser.setIsIndividual(true);
		childUser.setName(userNamePrefix+"3");
		childUser.setId(userGroupDAO.create(childUser));
		
		// Nest one group and two users within the parent group
		adder.add(nestedGroup.getId());
		adder.add(parentUser.getId());
		adder.add(siblingUser.getId());
		groupMembersDAO.addMembers(parentGroup.getId(), adder);
		
		// Nest two users within the child group
		adder.clear();
		adder.add(siblingUser.getId());
		adder.add(childUser.getId());
		groupMembersDAO.addMembers(nestedGroup.getId(), adder);
		
		return parentGroup;
	}
	
	private void createCredentials(UserGroup group) throws Exception {
		String principalId = group.getId();
		authDAO.changePassword(principalId, "ThisIsMySuperSecurePassword");
		authDAO.changeSecretKey(principalId);
		authDAO.changeSessionToken(principalId, null);
	}
	
	private void createTeamsRequestsAndInvitations(UserGroup group) {
		Team team = new Team();
		team.setId(group.getId());
		team.setName(group.getName());
		team.setDescription("test team");
		teamDAO.create(team);
		
		// create a MembershipRqstSubmission
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		Date expiresOn = new Date();
		mrs.setExpiresOn(expiresOn);
		mrs.setMessage("Please let me join the team.");
		mrs.setTeamId(""+group.getId());
		// need another valid user group
		UserGroup individUser = userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		mrs.setUserId(individUser.getId());
		membershipRqstSubmissionDAO.create(mrs);
		
		
		// create a MembershipInvtnSubmission
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setExpiresOn(expiresOn);
		mis.setMessage("Please join the team.");
		mis.setTeamId(""+group.getId());
		
		// need another valid user group
		mis.setInvitees(Arrays.asList(new String[]{individUser.getId()}));
		Long.parseLong(individUser.getId());
		
		membershipInvtnSubmissionDAO.create(mis);
	}

	@After
	public void after() throws Exception{
		// to cleanup for this test we delete all in the database
		resetDatabase();
	}
	
	/**
	 * This is the actual test.  The rest of the class is setup and tear down.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception{
		// Get the list of primary types
		MigrationTypeList primaryTypesList = entityServletHelper.getPrimaryMigrationTypes(userName);
		assertNotNull(primaryTypesList);
		assertNotNull(primaryTypesList.getList());
		assertTrue(primaryTypesList.getList().size() > 0);
		// Get the counts before we start
		MigrationTypeCounts startCounts = entityServletHelper.getMigrationTypeCounts(userName);
		validateStartingCount(startCounts);
		
		// This test will backup all data, delete it, then restore it.
		List<BackupInfo> backupList = new ArrayList<BackupInfo>();
		for (MigrationType type : primaryTypesList.getList()) {
			// Backup each type
			backupList.addAll(backupAllOfType(type));
		}
		
		// Now delete all data in reverse order
		for (int i = primaryTypesList.getList().size() - 1; i >= 0; i--) {
			MigrationType type = primaryTypesList.getList().get(i);
			deleteAllOfType(type);
		}
		
		// After deleting, the counts should be 0 except for a few special cases
		MigrationTypeCounts afterDeleteCounts = entityServletHelper.getMigrationTypeCounts(userName);
		assertNotNull(afterDeleteCounts);
		assertNotNull(afterDeleteCounts.getList());
		
		for (int i = 0; i < afterDeleteCounts.getList().size(); i++) {
			MigrationTypeCount afterDelete = afterDeleteCounts.getList().get(i);

			// Special cases for the not-deleted migration admin
			if (afterDelete.getType() == MigrationType.PRINCIPAL) {
				assertEquals("There should be 4 UserGroups remaining after the delete: "
								+ AuthorizationConstants.MIGRATION_USER_NAME + ", "
								+ AuthorizationConstants.ADMIN_GROUP_NAME + ", "
								+ AuthorizationConstants.DEFAULT_GROUPS.PUBLIC + ", and "
								+ AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS,
						new Long(4), afterDelete.getCount());
			} else if (afterDelete.getType() == MigrationType.GROUP_MEMBERS 
					|| afterDelete.getType() == MigrationType.CREDENTIAL) {
				assertEquals("Counts do not match for: " + afterDelete.getType().name(), new Long(1), afterDelete.getCount());
				
			} else {
				assertEquals("Counts are non-zero for: " + afterDelete.getType().name(), new Long(0), afterDelete.getCount());
			}
		}
		
		// Now restore all of the data
		for(BackupInfo info: backupList){
			String fileName = info.getFileName();
			assertNotNull("Did not find a backup file name for type: "+info.getType(), fileName);
			restoreFromBackup(info.getType(), fileName);
		}
		
		// The counts should all be back
		MigrationTypeCounts finalCounts = entityServletHelper.getMigrationTypeCounts(userName);
		for (int i = 1; i < finalCounts.getList().size(); i++) {
			MigrationTypeCount startCount = startCounts.getList().get(i);
			MigrationTypeCount afterRestore = finalCounts.getList().get(i);
			assertEquals("Count for " + startCount.getType().name() + " does not match", startCount.getCount(), afterRestore.getCount());
		}
	}
	
	private static class BackupInfo {
		MigrationType type;
		String fileName;
		public BackupInfo(MigrationType type, String fileName) {
			super();
			this.type = type;
			this.fileName = fileName;
		}
		public MigrationType getType() {
			return type;
		}
		public String getFileName() {
			return fileName;
		}
	}
	
	/**
	 * There must be at least one object for every type of migratable object.
	 * @param startCounts
	 */
	private void validateStartingCount(MigrationTypeCounts startCounts) {
		assertNotNull(startCounts);
		assertNotNull(startCounts.getList());
		assertEquals("This test requires at least one object to exist for each MigrationType.  Please create a new object of the new MigrationType in the before() method of this test.",MigrationType.values().length, startCounts.getList().size());
		for(MigrationTypeCount count: startCounts.getList()){
			assertTrue("This test requires at least one object to exist for each MigrationType.  Please create a new object of type: "+count.getType()+" in the before() method of this test.", count.getCount() > 0);
		}
	}


	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}
	
	/**
	 * Backup all data
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private List<BackupInfo> backupAllOfType(MigrationType type) throws Exception {
		RowMetadataResult list = entityServletHelper.getRowMetadata(userName, type, Long.MAX_VALUE, 0);
		if(list == null) return null;
		// Backup batches by their level in the tree
		ListBucketProvider provider = new ListBucketProvider();
		MigrationUtils.bucketByTreeLevel(list.getList().iterator(), provider);
		List<BackupInfo> result = new ArrayList<BackupInfo>();
		List<List<Long>> listOfBuckets = provider.getListOfBuckets();
		for(List<Long> batch: listOfBuckets){
			if(batch.size() > 0){
				String fileName = backup(type, batch);
				result.add(new BackupInfo(type, fileName));
			}
		}
		return result;
	}
	
	private String backup(MigrationType type, List<Long> tobackup) throws Exception {
		// Start the backup job
		IdList ids = new IdList();
		ids.setList(tobackup);
		BackupRestoreStatus status = entityServletHelper.startBackup(userName, type, ids);
		// wait for it..
		waitForDaemon(status);
		status = entityServletHelper.getBackupRestoreStatus(userName, status.getId());
		assertNotNull(status.getBackupUrl());
		return getFileNameFromUrl(status.getBackupUrl());
	}
	
	private void restoreFromBackup(MigrationType type, String fileName) throws Exception{
		RestoreSubmission sub = new RestoreSubmission();
		sub.setFileName(fileName);
		BackupRestoreStatus status = entityServletHelper.startRestore(userName, type, sub);
		// wait for it
		waitForDaemon(status);
	}
	
	/**
	 * Delete all data for a type.
	 * @param type
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private void deleteAllOfType(MigrationType type) throws Exception{
		IdList idList = getIdListOfAllOfType(type);
		if(idList == null) return;
		MigrationTypeCount result = entityServletHelper.deleteMigrationType(userName, type, idList);
		System.out.println("Deleted: "+result);
	}
	
	/**
	 * List all of the IDs for a type.
	 * @param type
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private IdList getIdListOfAllOfType(MigrationType type) throws Exception{
		RowMetadataResult list = entityServletHelper.getRowMetadata(userName, type, Long.MAX_VALUE, 0);
		if(list.getTotalCount() < 1) return null;
		// Create the backup list
		List<Long> toBackup = new LinkedList<Long>();
		for(RowMetadata row: list.getList()){
			toBackup.add(row.getId());
		}
		IdList idList = new IdList();
		idList.setList(toBackup);
		return idList;
	}
	
	/**
	 * Wait for a deamon to process a a job.
	 * @param status
	 * @throws InterruptedException 
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void waitForDaemon(BackupRestoreStatus status) throws Exception{
		long start = System.currentTimeMillis();
		while(DaemonStatus.COMPLETED != status.getStatus()){
			assertFalse("Daemon failed "+status.getErrorDetails(), DaemonStatus.FAILED == status.getStatus());
			System.out.println("Waiting for backup/restore daemon.  Message: "+status.getProgresssMessage());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for a backup/restore daemon",elapse < MAX_WAIT_MS);
			status = entityServletHelper.getBackupRestoreStatus(userName, status.getId());
		}
	}

}
