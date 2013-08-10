package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class IT055WikiPageTest {
	
	public static final long MAX_WAIT_MS = 1000*20; // 10 sec
	
	private static String FILE_NAME = "LittleImage.png";

	private List<WikiPageKey> toDelete = null;

	private static Synapse synapse = null;
	File imageFile;
	S3FileHandle fileHandle;
	Project project;
	
	private static Synapse createSynapseClient(String user, String pw) throws SynapseException {
		Synapse synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		synapse.login(user, pw);
		
		return synapse;
	}
	
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		synapse = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		// Create a 
	}
	
	@Before
	public void before() throws SynapseException {
		toDelete = new ArrayList<WikiPageKey>();
		// Get the image file from the classpath.
		URL url = IT055WikiPageTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		// Create the image file handle
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		fileHandle = (S3FileHandle) results.getList().get(0);
		// Create a project, this will own the wiki page.
		project = new Project();
		project = synapse.createEntity(project);
	}

	/**
	 * @throws Exception 
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@After
	public void after() throws Exception {
		if(fileHandle != null){
			try {
				synapse.deleteFileHandle(fileHandle.getId());
			} catch (Exception e) {}
		}
		if(project != null){
			synapse.deleteAndPurgeEntity(project);
		}
		for(WikiPageKey key: toDelete){
			synapse.deleteWikiPage(key);
		}
	}
	
	@Test
	public void testWikiRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		// Now create a WikPage that has this file as an attachment
		WikiPage wiki = new WikiPage();
		wiki.setTitle("IT055WikiPageTest.testWikiRoundTrip");
		// Create it
		wiki = synapse.createWikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		// Add the file attachment and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki = synapse.updateWikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		// test get
		wiki = synapse.getWikiPage(key);
		
		WikiPage root = synapse.getRootWikiPage(project.getId(), ObjectType.ENTITY);
		assertEquals(wiki, root);
		// Get the tree
		PaginatedResults<WikiHeader> tree = synapse.getWikiHeaderTree(key.getOwnerObjectId(), key.getOwnerObjectType());
		assertNotNull(tree);
		assertNotNull(tree.getResults());
		assertEquals(1, tree.getResults().size());
		assertEquals(1, tree.getTotalNumberOfResults());
		// Delete the wiki
		synapse.deleteWikiPage(key);
		// Now try to get it
		try{
			synapse.getWikiPage(key);
			fail("This should have failed as the wiki was deleted");
		}catch(SynapseException e){
			// expected;
		}
	}
	
	@Test
	public void testGetWikiPageAttachments() throws Exception{
		WikiPage wiki = new WikiPage();
		wiki.setTitle("IT055WikiPageTest.testGetWikiPageAttachmentFileHandles");
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		// Create it
		wiki = synapse.createWikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		// Since we expect the preview file handle to be returned we need to wait for it.
		waitForPreviewToBeCreated(fileHandle);
		// There should be two handles for this WikiPage, one for the origin file
		// and the other for the preview.
		FileHandleResults results = synapse.getWikiAttachmenthHandles(key);
		assertNotNull(results);
		assertNotNull(results.getList());
		// there should be two things on the list, the original file and its preview.
		assertEquals(2, results.getList().size());
		FileHandle one = results.getList().get(0);
		assertTrue(one instanceof S3FileHandle);
		S3FileHandle handle = (S3FileHandle) one;
		FileHandle two = results.getList().get(1);
		assertTrue(two instanceof PreviewFileHandle);
		PreviewFileHandle preview = (PreviewFileHandle) two;
		assertTrue(handle.getPreviewId().equals(preview.getId()));
		// Make sure we can download
		File mainFile = null;
		File previewFile = null;
		try{
			// Download the files from Synapse:
			mainFile = synapse.downloadWikiAttachment(key, handle.getFileName());
			assertNotNull(mainFile);
			// Make sure we can also just get the temporary URL
			URL tempUrl = synapse.getWikiAttachmentTemporaryUrl(key, handle.getFileName());
			assertNotNull(tempUrl);
//			assertTrue("Did not get the expected URL",tempUrl.toString().indexOf(handle.getKey()) > 0);
			// the file should be the expected size
			assertEquals(handle.getContentSize().longValue(), mainFile.length());
			// Check the MD5
			String md5 = MD5ChecksumHelper.getMD5Checksum(mainFile);
			assertEquals(handle.getContentMd5(), md5);
			// download the preview
			previewFile = synapse.downloadWikiAttachmentPreview(key, handle.getFileName());
			assertNotNull(previewFile);
			// the file should be the expected size
			assertEquals(preview.getContentSize().longValue(), previewFile.length());
			// Make sure we can also just get the temporary URL
			tempUrl = synapse.getWikiAttachmentPreviewTemporaryUrl(key, handle.getFileName());
			assertNotNull(tempUrl);
//			assertTrue("Did not get the expected URL",tempUrl.toString().indexOf(preview.getKey()) > 0);
			// The preview FileHandle md5 is currently null
//			md5 = MD5ChecksumHelper.getMD5Checksum(previewFile);
//			assertEquals(preview.getContentMd5(), md5);
		}finally{
			if(mainFile != null){
				mainFile.delete();
			}
			if(previewFile != null){
				previewFile.delete();
			}
		}
	}

	/**
	 * Wait for a preview to be generated for the given file handle.
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	private void waitForPreviewToBeCreated(S3FileHandle fileHandle) throws InterruptedException,
			SynapseException {
		long start = System.currentTimeMillis();
		while(fileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview file to be created");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview to be created",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			fileHandle = (S3FileHandle) synapse.getRawFileHandle(fileHandle.getId());
		}
	}
	
}
