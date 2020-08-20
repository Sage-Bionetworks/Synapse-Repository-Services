package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;
import org.sagebionetworks.repo.model.FileEntity;
import org.springframework.http.HttpStatus;

public class ITUnhandledException {
	
	private static TempSynapseClient synapseClient;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();

		// Set up admin.
		synapseClient = new TempSynapseClient();
		
		synapseClient.setUsername(config.getMigrationAdminUsername());
		synapseClient.setApiKey(config.getMigrationAdminAPIKey());


		SynapseClientHelper.setEndpoints(synapseClient);
	}
	
	@Test
	public void testWithBadRequestException() {
		
		String expectedError = "SomeError";

		SynapseBadRequestException ex = assertThrows(SynapseBadRequestException.class, () -> {
			// Call under test
			synapseClient.raiseException(IllegalArgumentException.class, expectedError);
		});

		assertEquals(expectedError, ex.getMessage());
		
	}
	
	@Test
	public void testWithUnknownServerException() {
		
		String expectedError = "SomeError";

		UnknownSynapseServerException ex = assertThrows(UnknownSynapseServerException.class, () -> {
			// Call under test
			synapseClient.raiseException(IllegalStateException.class, expectedError);
		});

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getStatusCode());
		assertEquals("Status Code: 500 message: " + expectedError, ex.getMessage());
		
	}
	
	// Fallback test for exceptions thrown in the controllers
	@Test
	public void testWithNormalExecution() {
		
		SynapseBadRequestException ex = assertThrows(SynapseBadRequestException.class, () -> {
			// Call under test
			synapseClient.getEntity("synNonValidId", FileEntity.class);
		});
		
		assertEquals("synNonValidId is not a valid Synapse ID.", ex.getMessage());
		
	}
	
	public static class TempSynapseClient extends SynapseAdminClientImpl {
		
		public void raiseException(Class<? extends RuntimeException> clazz, String message) throws SynapseException {
			getEntity("/testing/exception?ex=" + clazz.getName() + "&exMsg=" + message);
		}
		
	}
	
}
