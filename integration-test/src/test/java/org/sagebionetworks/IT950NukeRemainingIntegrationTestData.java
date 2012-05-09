package org.sagebionetworks;

import java.io.IOException;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;

/**
 * TODO PLFM-843
 * 
 * @author deflaux
 */
public class IT950NukeRemainingIntegrationTestData {

	private static Synapse synapse = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		/*
		 * TODO we need an admin user for this
		 * 
		 * 
		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
				*/
	}

	/**
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@AfterClass
	public static void afterClass() throws Exception {
	}

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testNukeEulas() throws Exception {
	}

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testNukeAgreements() throws Exception {
	}

}
