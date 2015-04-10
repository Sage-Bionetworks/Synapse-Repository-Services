package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.S3TokenManagerImpl;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleManagerImplAutowireTest {
	
	public static final long MAX_UPLOAD_WORKER_TIME_MS = 20*1000;
	
	private List<S3FileHandle> toDelete;
	private final List<String> entitiesToDelete = Lists.newArrayList();
	
	@Autowired
	private FileHandleManager fileUploadManager;
	
	@Autowired
	private AmazonS3Client s3Client;
	
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
	
	private UserInfo userInfo;
	private UserInfo userInfo2;
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
	
	@Before
	public void before() throws Exception{
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

		toDelete = new LinkedList<S3FileHandle>();
		// Setup the mock file to upload.
		int numberFiles = 2;
		expectedMetadata = new LinkedList<S3FileHandle>();
		fileStreams = new LinkedList<FileItemStream>();
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
	
	@After
	public void after() throws Exception {
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
		for (String id : Lists.reverse(entitiesToDelete)) {
			entityManager.deleteEntity(userInfo, id);
		}
		
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo.getId().toString()));
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo2.getId().toString()));
	}
	

	@Test
	public void testUploadfiles() throws FileUploadException, IOException, ServiceUnavailableException, NoSuchAlgorithmException, DatastoreException, NotFoundException{
		FileItemIterator mockIterator = Mockito.mock(FileItemIterator.class);
		// The first file.
		// Mock two streams
		when(mockIterator.hasNext()).thenReturn(true, true, false);
		// Use the first two files.
		when(mockIterator.next()).thenReturn(fileStreams.get(0), fileStreams.get(1));
		// Upload the files.
		FileUploadResults results = fileUploadManager.uploadfiles(userInfo, new HashSet<String>(), mockIterator);
		assertNotNull(results);
		assertNotNull(results.getFiles());
		toDelete.addAll(results.getFiles());
		assertEquals(2, results.getFiles().size());
		// Now verify the results
		for(int i=0; i<2; i++){
			S3FileHandle metaResult = results.getFiles().get(i);
			assertNotNull(metaResult);
			S3FileHandle expected = expectedMetadata.get(i);
			assertNotNull(expected);
			// Validate the expected values
			assertEquals(expected.getFileName(), metaResult.getFileName());
			assertEquals(expected.getContentMd5(), metaResult.getContentMd5());
			assertEquals(expected.getContentSize(), metaResult.getContentSize());
			assertEquals(expected.getContentType(), metaResult.getContentType());
			assertNotNull("An id should have been assigned to this file", metaResult.getId());
			assertNotNull("CreatedOn should have been filled in.", metaResult.getCreatedOn());
			assertEquals("CreatedBy should match the user that created the file.", userInfo.getId().toString(), metaResult.getCreatedBy());
			assertEquals(StackConfiguration.getS3Bucket(), metaResult.getBucketName());
			assertNotNull(metaResult.getKey());
			assertTrue("The key should start with the userID", metaResult.getKey().startsWith(userInfo.getId().toString()));			
			// Validate this is in the database
			S3FileHandle fromDB = (S3FileHandle) fileHandleDao.get(metaResult.getId());
			assertEquals(metaResult, fromDB);
			// Test the Pre-Signed URL
			String presigned = fileUploadManager.getRedirectURLForFileHandle(metaResult.getId());
			assertNotNull(presigned);
			// This was added as a regression test for PLFM-1925.  When we upgraded to AWS client 1.4.3, it changes how the URLs were prepared and broke
			// both file upload and download.
			assertTrue("If the presigned url does not start with https://s3.amazonaws.com it will cause SSL failures. See PLFM-1925",
					presigned.startsWith("https://s3.amazonaws.com", 0));
		}
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
	public void testChunckedFileUpload() throws Exception {
		String fileBody = "This is the body of the file!!!!!";
		byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
		String md5 = TransferUtils.createMD5(fileBodyBytes);
		// First create a chunked file token
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		String fileName = "foo.bar";
		ccftr.setFileName(fileName);
		ccftr.setContentType("text/plain");
		ccftr.setContentMD5(md5);
		ChunkedFileToken token = fileUploadManager.createChunkedFileUploadToken(userInfo, ccftr);
		assertNotNull(token);
		assertNotNull(token.getKey());
		assertNotNull(token.getUploadId());
		assertNotNull(md5, token.getContentMD5());
		// the key must start with the user's id
		assertTrue(token.getKey().startsWith(userInfo.getId().toString()));
		// Now create a pre-signed URL for the first part
		ChunkRequest cpr = new ChunkRequest();
		cpr.setChunkedFileToken(token);
		cpr.setChunkNumber(1l);
		URL preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		String urlString = preSigned.toString();
		// This was added as a regression test for PLFM-1925.  When we upgraded to AWS client 1.4.3, it changes how the URLs were prepared and broke
		// both file upload and download.
		assertTrue("If the presigned url does not start with https://s3.amazonaws.com it will cause SSL failures. See PLFM-1925",urlString.startsWith("https://s3.amazonaws.com", 0));
		String text = putStringToPresignedURL(fileBody, ccftr, preSigned);
		System.out.println(text);
	
		// Make sure we can get the pre-signed url again if we need to.
		preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		
		// Next add the part
		ChunkResult part = fileUploadManager.addChunkToFile(userInfo, cpr);
		
		// We need a lsit of parts
		List<ChunkResult> partList = new LinkedList<ChunkResult>();
		partList.add(part);
		CompleteChunkedFileRequest ccfr = new CompleteChunkedFileRequest();
		ccfr.setChunkedFileToken(token);
		ccfr.setChunkResults(partList);
		// We are now read to create our file handle from the parts
		S3FileHandle multiPartHandle = fileUploadManager.completeChunkFileUpload(userInfo, ccfr);
		assertNotNull(multiPartHandle);
		toDelete.add(multiPartHandle);
		System.out.println(multiPartHandle);
		assertNotNull(multiPartHandle.getBucketName());
		assertNotNull(multiPartHandle.getKey());
		assertNotNull(multiPartHandle.getContentSize());
		assertNotNull(multiPartHandle.getContentType());
		assertNotNull(multiPartHandle.getCreatedOn());
		assertNotNull(multiPartHandle.getCreatedBy());
		assertEquals(md5, multiPartHandle.getContentMd5());
		// Delete the file
		s3Client.deleteObject(multiPartHandle.getBucketName(), multiPartHandle.getKey());
		
	}
	
	@Test
	public void testChunckedFileUploadAsynch() throws Exception{
		String fileBody = "This is the body of the file!!!!!";
		byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
		String md5 = TransferUtils.createMD5(fileBodyBytes);
		// First create a chunked file token
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		String fileName = "foo.bar";
		ccftr.setFileName(fileName);
		ccftr.setContentType("text/plain");
		ccftr.setContentMD5(md5);
		ChunkedFileToken token = fileUploadManager.createChunkedFileUploadToken(userInfo, ccftr);
		assertNotNull(token);
		assertNotNull(token.getKey());
		assertNotNull(token.getUploadId());
		assertNotNull(md5, token.getContentMD5());
		// the key must start with the user's id
		assertTrue(token.getKey().startsWith(userInfo.getId().toString()));
		// Now create a pre-signed URL for the first part
		ChunkRequest cpr = new ChunkRequest();
		cpr.setChunkedFileToken(token);
		cpr.setChunkNumber(1l);
		URL preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		// Use the URL to upload a part.
		String text = putStringToPresignedURL(fileBody, ccftr, preSigned);
		System.out.println(text);
			
		// Start the asynch multi-part complete.
		CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
		cacr.setChunkedFileToken(token);
		cacr.setChunkNumbers(new LinkedList<Long>());
		cacr.getChunkNumbers().add(1l);
		UploadDaemonStatus daemonStatus = fileUploadManager.startUploadDeamon(userInfo, cacr);
		assertNotNull(daemonStatus);
		assertEquals(State.PROCESSING, daemonStatus.getState());
		assertEquals(null, daemonStatus.getFileHandleId());
		System.out.println(daemonStatus.toString());
		// Wait for the daemon to finish
		daemonStatus = waitForUploadDaemon(daemonStatus);
		assertNotNull(daemonStatus);
		System.out.println(daemonStatus.toString());
		assertEquals(State.COMPLETED, daemonStatus.getState());
		assertEquals(100, daemonStatus.getPercentComplete(), 0.0001);
		assertEquals(userInfo.getId().toString(), daemonStatus.getStartedBy());
		assertEquals(null, daemonStatus.getErrorMessage());
		assertNotNull(daemonStatus.getFileHandleId());
		// Get the file handle
		S3FileHandle multiPartHandle = (S3FileHandle) fileUploadManager.getRawFileHandle(userInfo, daemonStatus.getFileHandleId());
		assertNotNull(multiPartHandle);
		toDelete.add(multiPartHandle);
		System.out.println(multiPartHandle);
		assertNotNull(multiPartHandle.getBucketName());
		assertNotNull(multiPartHandle.getKey());
		assertNotNull(multiPartHandle.getContentSize());
		assertNotNull(multiPartHandle.getContentType());
		assertNotNull(multiPartHandle.getCreatedOn());
		assertNotNull(multiPartHandle.getCreatedBy());
		assertEquals(md5, multiPartHandle.getContentMd5());
		// Delete the file
		s3Client.deleteObject(multiPartHandle.getBucketName(), multiPartHandle.getKey());
		
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
	public void testExternalS3UploadDestinationSecurityCheck() throws Exception {
		String testBase = "test-base-" + UUID.randomUUID();
		ExternalS3StorageLocationSetting externalS3LocationSetting = new ExternalS3StorageLocationSetting();
		externalS3LocationSetting.setBanner("upload here");
		externalS3LocationSetting.setDescription("external");
		externalS3LocationSetting.setUploadType(UploadType.S3);
		externalS3LocationSetting.setBucket(StackConfiguration.singleton().getExternalS3TestBucketName());
		externalS3LocationSetting.setBaseKey(testBase);

		s3Client.createBucket(externalS3LocationSetting.getBucket());

		externalS3LocationSetting.setEndpointUrl("https://someurl");
		try {
			projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
			fail();
		} catch (NotImplementedException e) {
		}

		// null, empty or us-east-1 should all not give NotImplementedException
		for (String host : new String[] { null, "", "https://" + Constants.S3_HOSTNAME }) {
			externalS3LocationSetting.setEndpointUrl(host);

			try {
				projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
				fail();
			} catch (IllegalArgumentException e) {
				assertTrue(e.getMessage().indexOf("Did not find S3 object") != -1);
			}
		}

		String nothing = "";
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(nothing.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "owner.txt", new StringInputStream(nothing), metadata);
		
		S3FileHandle fauxHandle = new S3FileHandle();
		fauxHandle.setBucketName(externalS3LocationSetting.getBucket());
		fauxHandle.setKey(testBase + "owner.txt");
		toDelete.add(fauxHandle);

		try {
			projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("No username found") != -1);
		}

		String wrongName = "not me";
		metadata.setContentLength(wrongName.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "owner.txt", new StringInputStream(wrongName), metadata);

		try {
			projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("is not what was expected") != -1);
		}

		metadata.setContentLength(username.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), testBase + "owner.txt", new StringInputStream(username), metadata);

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
	public void testCreateCompressedFileFromString() throws Exception{
		String fileContents = "This will be compressed";
		String userId = ""+userInfo.getId();
		Date createdOn = new Date(System.currentTimeMillis());
		S3FileHandle handle = fileUploadManager.createCompressedFileFromString(userId, createdOn, fileContents);
		assertNotNull(handle);
		toDelete.add(handle);
		assertEquals(StackConfiguration.getS3Bucket(), handle.getBucketName());
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
			String back = FileUtils.readCompressedStreamAsString(input);
			assertEquals(fileContents, back);
		}finally{
			input.close();
		}
	}
	
	@Test
	public void testCreateFileHandleFromAttachment()throws Exception{
		// First create a real file handle
		String fileContents = "This will be compressed";
		String userId = ""+userInfo.getId();
		Date createdOn = new Date(System.currentTimeMillis());
		String fileName = "sample.txt";
		String entityId = "syn1234";
		// Create an attachment from the filehandle
		AttachmentData ad = fileUploadManager.createAttachmentInS3(fileContents, fileName, userId, entityId, createdOn);
		S3FileHandle h2 = fileUploadManager.createFileHandleFromAttachmentIfExists(entityId, userId, createdOn, ad);
		assertNotNull(h2);
		assertEquals(StackConfiguration.getS3Bucket(), h2.getBucketName());
		assertEquals(S3TokenManagerImpl.createAttachmentPathNoSlash(entityId, ad.getTokenId()), h2.getKey());
		assertNotNull(h2.getContentMd5());
		assertEquals(new Long(fileContents.getBytes("UTF-8").length), h2.getContentSize());
		assertEquals("text/plain", h2.getContentType());
		assertEquals(userId, h2.getCreatedBy());
		assertNotNull(h2.getCreatedOn());
		assertEquals(fileName, h2.getFileName());
		assertNotNull(h2.getId());
		toDelete.add(h2);	
	}
	
	
	@Test
	public void testCreateFileHandleFromAttachmentNotFound()throws Exception{
		// Create an attachment from the filehandle
		String userId = ""+userInfo.getId();
		String entityId = "syn1234";
		Date createdOn = new Date(System.currentTimeMillis());
		AttachmentData ad = new AttachmentData();
		ad.setContentType("text/plain");
		ad.setMd5("md5");
		ad.setName("some name");
		ad.setPreviewId(null);
		ad.setTokenId("123/fake-id/does_not_exist.txt");
		assertNull(fileUploadManager.createFileHandleFromAttachmentIfExists(entityId, userId, createdOn, ad));
	}
	
	@Test
	public void testCreateNeverUploadedPlaceHolderFileHandle() throws UnsupportedEncodingException, IOException{
		String userId = ""+userInfo.getId();
		Date createdOn = new Date(System.currentTimeMillis());
		String name = "archive.zip";
		S3FileHandle handle = fileUploadManager.createNeverUploadedPlaceHolderFileHandle(userId, createdOn, name);
		assertNotNull(handle);
		toDelete.add(handle);
		assertEquals("text/plain", handle.getContentType());
		assertEquals("archive_zip_placeholder.txt", handle.getFileName());
		assertNotNull(handle.getContentMd5());
	}

	/**
	 * Helper to wait for an upload deamon to finish.
	 * @param daemonStatus
	 * @return
	 * @throws InterruptedException
	 * @throws NotFoundException
	 */
	private UploadDaemonStatus waitForUploadDaemon(UploadDaemonStatus daemonStatus)
			throws InterruptedException, NotFoundException {
		// Wait for the daemon status
		long start = System.currentTimeMillis();
		while(State.COMPLETED != daemonStatus.getState()){
			assertFalse("Upload daemon failed: "+daemonStatus.getErrorMessage(), State.FAILED == daemonStatus.getState());
			System.out.println("Waiting for upload daemon to complete multi-part upload...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for upload to finish",System.currentTimeMillis() - start < MAX_UPLOAD_WORKER_TIME_MS);
			daemonStatus = fileUploadManager.getUploadDaemonStatus(userInfo, daemonStatus.getDaemonId());
		}
		return daemonStatus;
	}

	/**
	 * PUT a string to pre-siged URL.
	 * @param fileBody
	 * @param ccftr
	 * @param preSigned
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private String putStringToPresignedURL(String fileBody,	CreateChunkedFileTokenRequest ccftr, URL preSigned)
			throws UnsupportedEncodingException, IOException,
			ClientProtocolException {
		HttpPut httppost = new HttpPut(preSigned.toString());
		StringEntity entity = new StringEntity(fileBody, "UTF-8");
		entity.setContentType(ccftr.getContentType());
		httppost.setEntity(entity);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(httppost);
		String text = EntityUtils.toString(response.getEntity());
		assertEquals(200, response.getStatusLine().getStatusCode());
		return text;
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
