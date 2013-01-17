package org.sagebionetworks;

import static org.junit.Assert.*;

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
import org.sagebionetworks.repo.model.file.FileHandleResults;

public class IT125FileHandleTest {

	private List<String> toDelete = null;

	private static Synapse synapse = null;
	File imageFile;
	
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
	}
	
	@Before
	public void before() throws SynapseException {
		toDelete = new ArrayList<String>();
		URL url = IT100BackupRestoration.class.getClassLoader()
				.getResource("images/LittleImage.png");
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
		if (toDelete != null) {
			for (String id: toDelete) {
				try {
					synapse.deleteEntityById(id);
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testImageFiles() throws SynapseException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		// Create the image
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list);
		assertNotNull(results);
	}
}
