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
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class IT055WikiPageTest {
	
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
			synapse.deleteEntity(project);
		}
		for(WikiPageKey key: toDelete){
			synapse.deleteWikiPage(key);
		}
	}
	
	@Test
	public void testWikiRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		// Create the image file handle
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		fileHandle = results.getList().get(0);
		// Create a project, this will own the wiki page.
		project = new Project();
		project = synapse.createEntity(project);

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
}
