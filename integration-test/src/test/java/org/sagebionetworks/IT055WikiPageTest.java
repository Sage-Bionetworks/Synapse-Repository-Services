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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class IT055WikiPageTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private  static final long MAX_WAIT_MS = 1000*20; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";

	private List<WikiPageKey> toDelete;
	private List<String> handlesToDelete;
	private File imageFile;
	private String imageFileMD5;
	private S3FileHandle fileHandle;
	private Project project;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException, IOException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<WikiPageKey>();
		handlesToDelete = new ArrayList<String>();
		
		// Create a project, this will own the wiki page.
		project = new Project();
		project = synapse.createEntity(project);

		// Get the image file from the classpath.
		URL url = IT055WikiPageTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		imageFileMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);

		fileHandle = synapse.multipartUpload(imageFile, null, true, false);
		handlesToDelete.add(fileHandle.getId());
	}

	@After
	public void after() throws Exception {
		
		adminSynapse.deleteEntity(project, true);
		for (WikiPageKey key : toDelete) {
			adminSynapse.deleteWikiPage(key);
		}
		
		for (String id : handlesToDelete) {
			try {
				adminSynapse.deleteFileHandle(id);
			} catch (SynapseNotFoundException e) {
			} catch (SynapseClientException e) { }
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseClientException e) { }
	}
	
	@Test
	public void testWikiRoundTrip() throws Exception {
		// Now create a WikPage that has this file as an attachment
		WikiPage wiki = new WikiPage();
		wiki.setTitle("IT055WikiPageTest.testWikiRoundTrip");
		// Create it
		wiki = synapse.createWikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		handlesToDelete.add(synapse.getV2WikiPage(key).getMarkdownFileHandleId());
		
		// Add the file attachment and update the wiki
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		wiki = synapse.updateWikiPage(key.getOwnerObjectId(), key.getOwnerObjectType(), wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getAttachmentFileHandleIds());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		assertEquals(fileHandle.getId(), wiki.getAttachmentFileHandleIds().get(0));
		// test get
		wiki = synapse.getWikiPage(key);
		handlesToDelete.add(synapse.getV2WikiPage(key).getMarkdownFileHandleId());
		
		WikiPage root = synapse.getRootWikiPage(project.getId(), ObjectType.ENTITY);
		handlesToDelete.add(synapse.getV2RootWikiPage(project.getId(), ObjectType.ENTITY).getMarkdownFileHandleId());
		assertEquals(wiki, root);
		// Delete the wiki
		synapse.deleteWikiPage(key);
		// Now try to get it
		try{
			synapse.getWikiPage(key);
			fail("This should have failed as the wiki was deleted");
		}catch(SynapseException e){
			// expected;
		}
		
		// Make sure cleanup of the file handle can be performed without race conditions
		waitForPreviewToBeCreated(fileHandle);
	}
	
	@Test
	public void testGetWikiPageAttachments() throws Exception {
		WikiPage wiki = new WikiPage();
		wiki.setTitle("IT055WikiPageTest.testGetWikiPageAttachmentFileHandles");
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki.getAttachmentFileHandleIds().add(fileHandle.getId());
		// Create it
		wiki = synapse.createWikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, wiki.getId());
		toDelete.add(key);
		handlesToDelete.add(synapse.getV2WikiPage(key).getMarkdownFileHandleId());
		
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
		
		// also test the methods that get wiki attachments themselves	
		URL url = synapse.getWikiAttachmentTemporaryUrl(key, handle.getFileName());
		assertNotNull(url);
		
		File target = File.createTempFile("test", null);
		target.deleteOnExit();
		synapse.downloadWikiAttachment(key, handle.getFileName(), target);
		assertEquals(imageFileMD5, MD5ChecksumHelper.getMD5Checksum(target));
		
		url = synapse.getWikiAttachmentPreviewTemporaryUrl(key, handle.getFileName());
		assertNotNull(url);
		
		synapse.downloadWikiAttachmentPreview(key, handle.getFileName(), target);
		assertTrue(target.length()>0);
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
		handlesToDelete.add(fileHandle.getPreviewId());
	}
	
}
