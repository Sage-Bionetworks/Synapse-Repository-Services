package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItemStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.storagelocation.processors.ProxyStorageLocationProcessor;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleManagerImplAutowireTest {

	private List<File> filesToDelete;
	private List<S3FileHandle> toDelete;
	private List<WikiPageKey> wikisToDelete;
	private final List<String> entitiesToDelete = Lists.newArrayList();

	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private FileHandleManager fileUploadManager;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	public UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	private V2WikiManager v2WikiManager;
	
	private UserInfo userInfo;
	private UserInfo userInfo2;
	private UserInfo anonymousUserInfo;
	private String username;
	
	/**
	 * This is the metadata about the files we uploaded.
	 */
	private List<S3FileHandle> expectedMetadata;
	private String[] fileContents;
	private List<FileItemStream> fileStreams;

	private String projectId;
	private String uploadFolder;

	private String projectName;

	@BeforeEach
	public void before() throws Exception{
		wikisToDelete = new LinkedList<>();
		NewUser user = new NewUser();
		username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		user = new NewUser();
		String username2 = UUID.randomUUID().toString();
		user.setEmail(username2 + "@test.com");
		user.setUserName(username2);
		userInfo2 = userManager.getUserInfo(userManager.createUser(user));
		userInfo2.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		anonymousUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		Project project = new Project();
		projectName = "project" + new Random().nextInt();
		project.setName(projectName);
		projectId = entityManager.createEntity(userInfo, project, null);
		entitiesToDelete.add(projectId);

		addAcl(projectId, userInfo2.getId());

		Folder child = new Folder();
		child.setName("child");
		child.setParentId(projectId);
		String childId = entityManager.createEntity(userInfo, child, null);
		entitiesToDelete.add(childId);
		Folder child2 = new Folder();
		child2.setName("child2  a_-nd.+more()");
		child2.setParentId(childId);
		uploadFolder = entityManager.createEntity(userInfo, child2, null);
		entitiesToDelete.add(uploadFolder);

		filesToDelete = new ArrayList<>();
		toDelete = new LinkedList<>();
		// Setup the mock file to upload.
		int numberFiles = 2;
		expectedMetadata = new LinkedList<>();
		fileStreams = new LinkedList<>();
		fileContents = new String[numberFiles];
		for(int i=0; i<numberFiles; i++){
			fileContents[i] = "This is the contents for file: "+i;
			byte[] fileBytes = fileContents[i].getBytes();
			String fileName = "foo-"+i+".txt";
			String contentType = "text/plain";
			FileItemStream fis = Mockito.mock(FileItemStream.class);
			when(fis.getContentType()).thenReturn(contentType);
			when(fis.getName()).thenReturn(fileName);
			when(fis.openStream()).thenReturn(new StringInputStream(fileContents[i]));
			fileStreams.add(fis);
			// Set the expected metadata for this file.
			S3FileHandle metadata = new S3FileHandle();
			metadata.setContentType(contentType);
			metadata.setContentMd5( BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(fileBytes))));
			metadata.setContentSize(new Long(fileBytes.length));
			metadata.setFileName(fileName);
			expectedMetadata.add(metadata);
		}
	}
	
	@AfterEach
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for(WikiPageKey key: wikisToDelete) {
			v2WikiManager.deleteWiki(adminUserInfo, key);
		}

		for (File file : filesToDelete) {
			file.delete();
		}

		for (String id : Lists.reverse(entitiesToDelete)) {
			entityManager.deleteEntity(userInfo, id);
		}

		if(toDelete != null && s3Client != null){
			// Delete any files created
			for(S3FileHandle meta: toDelete){
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				if (meta.getId() != null) {
					// We also need to delete the data from the database
					fileHandleDao.delete(meta.getId());
				}
			}
		}

		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo.getId().toString()));
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo2.getId().toString()));
	}

	/**
	 * The cross-origin resource sharing (CORS) setting are required so the browser javascript code can talk directly to S3.
	 * 
	 */
	@Test
	public void testCORSSettings(){
		BucketCrossOriginConfiguration bcoc = fileUploadManager.getBucketCrossOriginConfiguration();
		assertNotNull(bcoc);
		assertNotNull(bcoc.getRules());
		assertEquals(1, bcoc.getRules().size());
		CORSRule rule = bcoc.getRules().get(0);
		assertNotNull(rule);
		assertEquals(FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID, rule.getId());
		assertNotNull(rule.getAllowedOrigins());
		assertEquals(1, rule.getAllowedOrigins().size());
		assertEquals("*", rule.getAllowedOrigins().get(0));
		assertNotNull(rule.getAllowedMethods());
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.GET));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.PUT));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.POST));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.HEAD));
		assertEquals(300, rule.getMaxAgeSeconds());
		// the wildcard headers in not working
		assertNotNull(rule.getAllowedHeaders());
		assertEquals(1, rule.getAllowedHeaders().size());
		assertEquals("*", rule.getAllowedHeaders().get(0));
	}
	
	@Test
	public void testExternalUploadDestinationOld() throws Exception {
		final String URL = "sftp://www.sftpsite.com/base/basefolder";

		ExternalStorageLocationSetting externalLocationSetting = new ExternalStorageLocationSetting();
		externalLocationSetting.setBanner("upload here");
		externalLocationSetting.setSupportsSubfolders(true);
		externalLocationSetting.setUploadType(UploadType.SFTP);
		externalLocationSetting.setUrl(URL);
		externalLocationSetting.setDescription("external");
		externalLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalLocationSetting);

		UploadDestinationListSetting uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(projectId);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));

		projectSettingsManager.createProjectSetting(userInfo, uploadDestinationListSetting);

		@SuppressWarnings("deprecation")
		List<UploadDestination> uploadDestinations = fileUploadManager.getUploadDestinations(userInfo, uploadFolder);

		assertEquals(1, uploadDestinations.size());
		assertEquals(ExternalUploadDestination.class, uploadDestinations.get(0).getClass());
		ExternalUploadDestination externalUploadDestination = (ExternalUploadDestination) uploadDestinations.get(0);
		assertEquals(UploadType.SFTP, externalUploadDestination.getUploadType());
		assertEquals("upload here", externalUploadDestination.getBanner());
		String expectedStart = URL + "/" + projectName + "/child/child2%20%20a_-nd.%2Bmore%28%29/";
		assertEquals(expectedStart, externalUploadDestination.getUrl().substring(0, expectedStart.length()));
	}

	@Test
	public void testExternalUploadDestination() throws Exception {
		final String URL = "sftp://www.sftpsite.com/base/basefolder";

		ExternalStorageLocationSetting externalLocationSetting = new ExternalStorageLocationSetting();
		externalLocationSetting.setBanner("upload here");
		externalLocationSetting.setSupportsSubfolders(true);
		externalLocationSetting.setUploadType(UploadType.SFTP);
		externalLocationSetting.setUrl(URL);
		externalLocationSetting.setDescription("external");
		externalLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalLocationSetting);

		UploadDestinationListSetting uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(projectId);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));

		projectSettingsManager.createProjectSetting(userInfo, uploadDestinationListSetting);

		List<UploadDestinationLocation> uploadDestinationLocations = fileUploadManager.getUploadDestinationLocations(userInfo, uploadFolder);
		assertEquals(1, uploadDestinationLocations.size());
		assertEquals("external", uploadDestinationLocations.get(0).getDescription());
		assertEquals(UploadType.SFTP, uploadDestinationLocations.get(0).getUploadType());

		UploadDestination uploadDestination = fileUploadManager.getUploadDestination(userInfo, uploadFolder, uploadDestinationLocations
				.get(0).getStorageLocationId());
		assertEquals(ExternalUploadDestination.class, uploadDestination.getClass());
		ExternalUploadDestination externalUploadDestination = (ExternalUploadDestination) uploadDestination;
		assertEquals(UploadType.SFTP, externalUploadDestination.getUploadType());
		assertEquals("upload here", externalUploadDestination.getBanner());
		String expectedStart = URL + "/" + projectName + "/child/child2%20%20a_-nd.%2Bmore%28%29/";
		assertEquals(expectedStart, externalUploadDestination.getUrl().substring(0, expectedStart.length()));
	}
	
	@Test
	public void testExternalUploadDestinationUrlCheckFail() {
		ExternalStorageLocationSetting externalLocationSetting = new ExternalStorageLocationSetting();
		externalLocationSetting.setBanner("upload here");
		externalLocationSetting.setSupportsSubfolders(true);
		externalLocationSetting.setUploadType(UploadType.SFTP);
		externalLocationSetting.setDescription("external");

		externalLocationSetting.setUrl("sftp://www.sftpsite.com/base/base folder with spaces");
		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, externalLocationSetting));
	}

	@Test
	public void testExternalS3UploadDestinationSecurityCheck() throws Exception {
		String testBase = "test-base-" + UUID.randomUUID();
		ExternalS3StorageLocationSetting externalS3LocationSetting = new ExternalS3StorageLocationSetting();
		externalS3LocationSetting.setBanner("upload here");
		externalS3LocationSetting.setDescription("external");
		externalS3LocationSetting.setUploadType(UploadType.S3);
		externalS3LocationSetting.setBucket(StackConfigurationSingleton.singleton().getExternalS3TestBucketName());
		externalS3LocationSetting.setBaseKey(testBase);

		s3Client.createBucket(externalS3LocationSetting.getBucket());

		String nothing = "";
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(nothing.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "/owner.txt", new StringInputStream(nothing), metadata);
		
		S3FileHandle fauxHandle = new S3FileHandle();
		fauxHandle.setBucketName(externalS3LocationSetting.getBucket());
		fauxHandle.setKey(testBase + "/owner.txt");
		toDelete.add(fauxHandle);

		try {
			projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("No username found") != -1);
		}

		String wrongName = "not me";
		metadata.setContentLength(wrongName.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "/owner.txt", new StringInputStream(wrongName), metadata);

		try {
			projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("is not what was expected") != -1);
		}

		metadata.setContentLength(username.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "/owner.txt", new StringInputStream(username), metadata);

		externalS3LocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);

		// make sure only owner can use this s3 setting
		UploadDestinationListSetting uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(projectId);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setLocations(Lists.newArrayList(externalS3LocationSetting.getStorageLocationId()));

		try {
			projectSettingsManager.createProjectSetting(userInfo2, uploadDestinationListSetting);
			fail();
		} catch (UnauthorizedException e) {
			assertTrue(e.getMessage().indexOf("Only the owner") != -1);
		}

		projectSettingsManager.createProjectSetting(userInfo, uploadDestinationListSetting);
	}

	@Test
	public void uploadWithSts() throws Exception {
		// Set up bucket and owner.txt.
		String externalS3Bucket = StackConfigurationSingleton.singleton().getExternalS3TestBucketName();
		String externalS3StorageBaseKey = "test-base-" + UUID.randomUUID();
		s3Client.createBucket(externalS3Bucket);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(username.length());
		s3Client.putObject(externalS3Bucket, externalS3StorageBaseKey + "/owner.txt",
				new StringInputStream(username), metadata);

		// Create Synapse Storage and External S3 Storage w/ STS.
		S3StorageLocationSetting synapseStorageLocationSetting = new S3StorageLocationSetting();
		synapseStorageLocationSetting.setStsEnabled(true);
		synapseStorageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo,
				synapseStorageLocationSetting);
		long synapseStorageLocationId = synapseStorageLocationSetting.getStorageLocationId();
		String synapseStorageBaseKey = synapseStorageLocationSetting.getBaseKey();
		assertNotNull(synapseStorageBaseKey);
		assertNotEquals(externalS3StorageBaseKey, synapseStorageBaseKey);

		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(externalS3Bucket);
		externalS3StorageLocationSetting.setBaseKey(externalS3StorageBaseKey);
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo,
				externalS3StorageLocationSetting);
		long externalS3StorageLocationId = externalS3StorageLocationSetting.getStorageLocationId();
		assertNotEquals(synapseStorageLocationId, externalS3StorageLocationId);
		assertEquals(externalS3StorageBaseKey, externalS3StorageLocationSetting.getBaseKey());

		// Upload to Synapse Storage.
		File synapseFile = File.createTempFile("uploadWithSts-Synapse", ".txt");
		filesToDelete.add(synapseFile);
		Files.asCharSink(synapseFile, StandardCharsets.UTF_8).write(
				"Test file in Synapse storage location with STS");
		LocalFileUploadRequest synapseUploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(synapseFile).withStorageLocationId(synapseStorageLocationId)
				.withUserId(userInfo.getId().toString());

		S3FileHandle synapseFileHandle = fileUploadManager.multipartUploadLocalFile(synapseUploadRequest);
		assertNotNull(synapseFileHandle);
		assertEquals(synapseStorageLocationId, synapseFileHandle.getStorageLocationId());
		toDelete.add(synapseFileHandle);
		assertNotNull(synapseFileHandle.getBucketName());
		assertTrue(synapseFileHandle.getKey().startsWith(synapseStorageBaseKey));

		// Verify file exists in S3.
		assertTrue(s3Client.doesObjectExist(synapseFileHandle.getBucketName(), synapseFileHandle.getKey()));

		// Upload to External S3 Storage.
		File externalS3File = File.createTempFile("uploadWithSts-ExternalS3", ".txt");
		filesToDelete.add(externalS3File);
		Files.asCharSink(externalS3File, StandardCharsets.UTF_8).write(
				"Test file in external S3 storage location with STS");
		LocalFileUploadRequest externalS3UploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(externalS3File).withStorageLocationId(externalS3StorageLocationId)
				.withUserId(userInfo.getId().toString());

		S3FileHandle externalS3FileHandle = fileUploadManager.multipartUploadLocalFile(externalS3UploadRequest);
		assertNotNull(externalS3FileHandle);
		assertEquals(externalS3StorageLocationId, externalS3FileHandle.getStorageLocationId());
		toDelete.add(externalS3FileHandle);
		assertEquals(externalS3Bucket, externalS3FileHandle.getBucketName());
		assertTrue(externalS3FileHandle.getKey().startsWith(externalS3StorageBaseKey));

		// Verify file exists in S3.
		assertTrue(s3Client.doesObjectExist(externalS3FileHandle.getBucketName(), externalS3FileHandle.getKey()));

		// Attempt to create a new file handle that points at the same file. Even though there's a copy API that does
		// exactly this, we're specifically testing the more general createExternalS3FileHandle() API.)
		S3FileHandle externalS3FileHandleCopy = (S3FileHandle) fileUploadManager.getRawFileHandle(userInfo,
				externalS3FileHandle.getId());
		externalS3FileHandleCopy = fileUploadManager.createExternalS3FileHandle(userInfo, externalS3FileHandleCopy);
		assertNotNull(externalS3FileHandleCopy);
		toDelete.add(externalS3FileHandleCopy);
		assertNotEquals(externalS3FileHandle.getId(), externalS3FileHandleCopy.getId());
		assertEquals(externalS3Bucket, externalS3FileHandleCopy.getBucketName());
		assertEquals(externalS3FileHandle.getKey(), externalS3FileHandleCopy.getKey());

		// Create folders for the project.
		Folder synapseFolder = new Folder();
		synapseFolder.setParentId(projectId);
		String synapseFolderId = entityManager.createEntity(userInfo, synapseFolder, null);
		entitiesToDelete.add(synapseFolderId);

		Folder externalS3Folder = new Folder();
		externalS3Folder.setParentId(projectId);
		String externalS3FolderId = entityManager.createEntity(userInfo, externalS3Folder, null);
		entitiesToDelete.add(externalS3FolderId);

		// Add storage locations to the folders.
		UploadDestinationListSetting synapseProjectSetting = new UploadDestinationListSetting();
		synapseProjectSetting.setProjectId(synapseFolderId);
		synapseProjectSetting.setSettingsType(ProjectSettingsType.upload);
		synapseProjectSetting.setLocations(ImmutableList.of(synapseStorageLocationId));
		projectSettingsManager.createProjectSetting(userInfo, synapseProjectSetting);

		UploadDestinationListSetting externalS3ProjectSetting = new UploadDestinationListSetting();
		externalS3ProjectSetting.setProjectId(externalS3FolderId);
		externalS3ProjectSetting.setSettingsType(ProjectSettingsType.upload);
		externalS3ProjectSetting.setLocations(ImmutableList.of(externalS3StorageLocationId));
		projectSettingsManager.createProjectSetting(userInfo, externalS3ProjectSetting);

		// Before we can create file entities, we must agree to terms of use.
		authManager.setTermsOfUseAcceptance(userInfo.getId(), true);

		// Create file entities for each file handle.
		FileEntity synapseFileEntity = new FileEntity();
		synapseFileEntity.setDataFileHandleId(synapseFileHandle.getId());
		synapseFileEntity.setParentId(synapseFolderId);
		String synapseFileEntityId = entityManager.createEntity(userInfo, synapseFileEntity, null);
		entitiesToDelete.add(synapseFileEntityId);

		FileEntity externalS3FileEntity = new FileEntity();
		externalS3FileEntity.setDataFileHandleId(externalS3FileHandle.getId());
		externalS3FileEntity.setParentId(externalS3FolderId);
		String externalS3FileEntityId = entityManager.createEntity(userInfo, externalS3FileEntity, null);
		entitiesToDelete.add(externalS3FileEntityId);

		// Create a file handle and file entity in the default storage location (such as project root).
		File nonStsFile = File.createTempFile("nonStsFile", ".txt");
		filesToDelete.add(nonStsFile);
		Files.asCharSink(nonStsFile, StandardCharsets.UTF_8).write("Test file in without STS");
		LocalFileUploadRequest nonStsUploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(nonStsFile).withStorageLocationId(null).withUserId(userInfo.getId().toString());

		S3FileHandle nonStsFileHandle = fileUploadManager.multipartUploadLocalFile(nonStsUploadRequest);
		toDelete.add(nonStsFileHandle);

		FileEntity nonStsFileEntity = new FileEntity();
		nonStsFileEntity.setDataFileHandleId(nonStsFileHandle.getId());
		nonStsFileEntity.setParentId(projectId);
		String nonStsFileEntityId = entityManager.createEntity(userInfo, nonStsFileEntity, null);
		entitiesToDelete.add(nonStsFileEntityId);
	}

	@Test
	public void testProxyStorageLocationSettings() throws DatastoreException, NotFoundException, IOException{
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl("https://host.org:8080/path");
		proxy.setSecretKey(UUID.randomUUID().toString());
		proxy.setUploadType(UploadType.SFTP);
		//call under test
		ProxyStorageLocationSettings result = projectSettingsManager.createStorageLocationSetting(userInfo, proxy);
		assertNotNull(result);
		assertEquals(proxy.getProxyUrl(), result.getProxyUrl());
		assertEquals(proxy.getSecretKey(), result.getSecretKey());
		assertNotNull(result.getStorageLocationId());
	}
	
	@Test
	public void testProxyStorageLocationSettingsUrlNull() throws DatastoreException, NotFoundException {
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl(null);
		proxy.setSecretKey(UUID.randomUUID().toString());
		proxy.setUploadType(UploadType.SFTP);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, proxy));
	}
	
	@Test
	public void testProxyStorageLocationSettingsSecretNull() throws DatastoreException, NotFoundException {
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl("https://host.org:8080/path");
		proxy.setSecretKey(null);
		proxy.setUploadType(UploadType.SFTP);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, proxy));
	}
	
	@Test
	public void testProxyStorageLocationSettingsProtocolHttp() throws DatastoreException, NotFoundException {
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl("http://host.org:8080/path");
		proxy.setSecretKey(UUID.randomUUID().toString());
		proxy.setUploadType(UploadType.SFTP);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, proxy));
	}
	
	@Test
	public void testProxyStorageLocationSettingsUrlMalformed() throws DatastoreException, NotFoundException {
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl("host.org:8080/path");
		proxy.setSecretKey(UUID.randomUUID().toString());
		proxy.setUploadType(UploadType.SFTP);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, proxy));
	}
	
	@Test
	public void testProxyStorageLocationSettingsSecretTooSmall() throws DatastoreException, NotFoundException {
		ProxyStorageLocationSettings proxy = new ProxyStorageLocationSettings();
		proxy.setProxyUrl("https://host.org:8080/path");
		proxy.setSecretKey(new String(new char[ProxyStorageLocationProcessor.MIN_SECRET_KEY_CHARS-1]));
		// call under test
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, proxy));
	}

	@Test
	public void testCreateCompressedFileFromString() throws Exception{
		String fileContents = "This will be compressed";
		String userId = ""+userInfo.getId();
		Date createdOn = new Date(System.currentTimeMillis());
		S3FileHandle handle = fileUploadManager.createCompressedFileFromString(userId, createdOn, fileContents);
		assertNotNull(handle);
		toDelete.add(handle);
		assertEquals(StackConfigurationSingleton.singleton().getS3Bucket(), handle.getBucketName());
		assertEquals("compressed.txt.gz", handle.getFileName());
		assertNotNull(handle.getContentMd5());
		assertTrue(handle.getContentSize() > 1);
		assertNotNull(handle.getId());
		assertTrue(handle.getKey().contains(userId));
		
		// Read back the file and confirm the contents
		S3Object s3Object =s3Client.getObject(handle.getBucketName(), handle.getKey());
		assertNotNull(s3Object);
		InputStream input = s3Object.getObjectContent();
		try{
			String back = FileUtils.readStreamAsString(input, null, /*gunzip*/true);
			assertEquals(fileContents, back);
		}finally{
			input.close();
		}
	}
	
	/**
	 * Test for PLFM-4689.  Uses should be able to anonymously download wiki attachments
	 * if the wiki is publicly readable.
	 * @throws IOException 
	 * @throws Exception 
	 */
	@Test
	public void testGetWikiFileHandleAnonymous() throws Exception {
		Date now = new Date();
		S3FileHandle markdownHandle = fileUploadManager.createFileFromByteArray(userInfo
				.getId().toString(), now, "markdown contents".getBytes(StandardCharsets.UTF_8), "markdown.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		toDelete.add(markdownHandle);
		S3FileHandle attachmentFileHandle = fileUploadManager.createFileFromByteArray(userInfo
				.getId().toString(), now, "attachment data".getBytes(StandardCharsets.UTF_8), "attachment.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		toDelete.add(attachmentFileHandle);

		// add a wiki to the project
		V2WikiPage wiki = new V2WikiPage();
		wiki.setTitle("new wiki");
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setAttachmentFileHandleIds(Lists.newArrayList(attachmentFileHandle.getId()));
		wiki = v2WikiManager.createWikiPage(userInfo, projectId, ObjectType.ENTITY, wiki);
		WikiPageKey wikiKey = new WikiPageKey();
		wikiKey.setOwnerObjectId(projectId);
		wikiKey.setOwnerObjectType(ObjectType.ENTITY);
		wikiKey.setWikiPageId(wiki.getId());
		wikisToDelete.add(wikiKey);
				
		// setup a wiki file download.
		FileHandleAssociation association = new FileHandleAssociation();
		association.setAssociateObjectId(wiki.getId());
		association.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		association.setFileHandleId(attachmentFileHandle.getId());
		
		BatchFileRequest batchRequest = new BatchFileRequest();
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(true);
		batchRequest.setIncludePreviewPreSignedURLs(true);
		batchRequest.setRequestedFiles(Lists.newArrayList(association));
		
		// call under test - anonymous should not have access yet
		BatchFileResult results = fileUploadManager.getFileHandleAndUrlBatch(anonymousUserInfo, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(1, results.getRequestedFiles().size());
		FileResult result = results.getRequestedFiles().get(0);
		assertNotNull(result);
		assertEquals(FileResultFailureCode.UNAUTHORIZED, result.getFailureCode());
		
		// grant public read on the project
		addAcl(projectId, anonymousUserInfo.getId());
		
		// call under test - anonymous should now have access.
		results = fileUploadManager.getFileHandleAndUrlBatch(anonymousUserInfo, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(1, results.getRequestedFiles().size());
		result = results.getRequestedFiles().get(0);
		assertNotNull(result);
		assertEquals(null, result.getFailureCode());
		assertEquals(attachmentFileHandle, result.getFileHandle());
		assertNotNull(result.getPreSignedURL());
		
	}

	private void addAcl(String projectId, Long principalId) throws Exception {
		AccessControlList acl = accessControlListDAO.get(projectId, ObjectType.ENTITY);
		Set<ACCESS_TYPE> accessTypes = Sets.newHashSet(ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.UPLOAD);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		ra.setAccessType(accessTypes);
		acl.getResourceAccess().add(ra);
		accessControlListDAO.update(acl, ObjectType.ENTITY);
	}
}
