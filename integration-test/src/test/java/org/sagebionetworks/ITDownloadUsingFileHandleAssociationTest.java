package org.sagebionetworks;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.common.collect.Lists;

public class ITDownloadUsingFileHandleAssociationTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	private static final String FILE_NAME = "SmallTextFiles/TinyFile.txt";
	private static String MARKDOWN_NAME = "SmallTextFiles/markdown.txt";
	private Project project;
	private File imageFile;
	private FileHandle fileHandle;
	private FileHandle markdownHandle;
	private File markdownFile;
	private List<String> fileHandlesToDelete = Lists.newArrayList();

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
	public void before() throws SynapseException, FileNotFoundException, IOException {
		adminSynapse.clearAllLocks();
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
		// Get the image file from the classpath.
		URL url = IT054FileEntityTest.class.getClassLoader().getResource(FILE_NAME);
		URL markdownUrl = ITV2WikiPageTest.class.getClassLoader().getResource(MARKDOWN_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		markdownFile = new File(markdownUrl.getFile().replaceAll("%20", " "));
		
		fileHandle = synapse.multipartUpload(imageFile, null, false, false);
		markdownHandle = synapse.multipartUpload(markdownFile, null, false, false);
		fileHandlesToDelete.add(fileHandle.getId());
		fileHandlesToDelete.add(markdownHandle.getId());
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
		for (String handle : fileHandlesToDelete) {
			try {
				synapse.deleteFileHandle(handle);
			} catch (Exception e) {}
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}

	@Test
	public void testFileEntity() throws SynapseException {
		FileEntity file = new FileEntity();
		file.setName("IT054FileEntityTest.testFileEntityRoundTrip");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		// Create it
		file = synapse.createEntity(file);
		assertNotNull(file);
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId(file.getId());
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId(file.getDataFileHandleId());
		URL url = synapse.getFileURL(fha);
		assertNotNull(url);
	}

	@Test
	public void testWikiAttachment() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		V2WikiPage wiki = new V2WikiPage();
		wiki.setAttachmentFileHandleIds(Arrays.asList(fileHandle.getId()));
		wiki.setMarkdownFileHandleId(markdownHandle.getId());
		wiki.setTitle("ITDownloadUsingFileHandleAssociation.testWikiAttachment");
		// Create a V2WikiPage
		wiki = synapse.createV2WikiPage(project.getId(), ObjectType.ENTITY, wiki);
		assertNotNull(wiki);
		// get attachments
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId(wiki.getId());
		fha.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		fha.setFileHandleId(fileHandle.getId());
		URL url = synapse.getFileURL(fha);
		assertNotNull(url);
		// get markdown
		fha = new FileHandleAssociation();
		fha.setAssociateObjectId(wiki.getId());
		fha.setAssociateObjectType(FileHandleAssociateType.WikiMarkdown);
		fha.setFileHandleId(markdownHandle.getId());
		url = synapse.getFileURL(fha);
		assertNotNull(url);
	}

}
