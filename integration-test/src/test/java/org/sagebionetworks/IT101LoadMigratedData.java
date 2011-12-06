package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Validate that we can load data as expected from the repository services.
 */
public class IT101LoadMigratedData {
	
	private static Synapse synapse = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		
//		InputStream in = IT101LoadMigratedData.class.getClassLoader().getResourceAsStream("samplePreview2.txt");
//		String preivewExample = readToString(in);
//		System.out.println(preivewExample);
//		String encoded = new String(Base64.encodeBase64(preivewExample.getBytes("UTF-8")),"UTF-8");
//		System.out.println(encoded);
		
	}
	
	/**
	 * Read an input stream into a string.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
	/**
	 * This is a test for PLFM-775.
	 * @throws JSONObjectAdapterException 
	 * @throws SynapseServiceException 
	 * @throws SynapseUserException 
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 */
	@Test
	public void testLoadPreview() throws ClientProtocolException, IOException, JSONException, SynapseUserException, SynapseServiceException, JSONObjectAdapterException{
		// Load preview 149
		Preview preview = synapse.getEntity("149", Preview.class);
		assertNotNull(preview);
		assertNotNull(preview.getPreviewString());
		assertNotNull(preview.getHeaders());
		assertEquals(38, preview.getHeaders().size());
		assertNotNull(preview.getRows());
		assertEquals(5, preview.getRows().size());
	}

	@Test
	public void testLoadAgreement() throws ClientProtocolException, IOException, JSONException, SynapseUserException, SynapseServiceException, JSONObjectAdapterException{
		// Load preview 149
		Eula agreement = synapse.getEntity("5", Eula.class);
		assertNotNull(agreement);
		assertNotNull(agreement.getAgreement());
	}
}
