package org.sagebionetworks.tool.migration.v3;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.client.exceptions.SynapseException;

import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Migration client test.
 * 
 * @author jmhill
 *
 */
public class MigrationClientTest {
	
	StubSynapseAdministration destSynapse;
	StubSynapseAdministration sourceSynapse;
	SynapseClientFactory mockFactory;
	MigrationClient migrationClient;
	
	@Before
	public void before() throws SynapseException{
		// Create the two stubs
		destSynapse = new StubSynapseAdministration();
		sourceSynapse = new StubSynapseAdministration();
		mockFactory = Mockito.mock(SynapseClientFactory.class);
		when(mockFactory.createNewDestinationClient()).thenReturn(destSynapse);
		when(mockFactory.createNewSourceClient()).thenReturn(sourceSynapse);
		migrationClient = new MigrationClient(mockFactory);
	}
	
	@Test
	public void testSetDestinationStatus() throws SynapseException, JSONObjectAdapterException{
		// Set the status to down
		migrationClient.setDestinationStatus(StatusEnum.READ_ONLY, "Test message");
		// Only the destination should be changed
		StackStatus status = destSynapse.getCurrentStackStatus();
		StackStatus expected = new StackStatus();
		expected.setCurrentMessage("Test message");
		expected.setStatus(StatusEnum.READ_ONLY);
		assertEquals(expected, status);
		// The source should remain unmodified
		status = sourceSynapse.getCurrentStackStatus();
		expected = new StackStatus();
		expected.setCurrentMessage("Synapse is read for read/write");
		expected.setStatus(StatusEnum.READ_WRITE);
		assertEquals(expected, status);
	}
	
	@Test
	public void testMigrateFromSourceToDestinationStackStatus(){
		// When data is migrated from the source to destination, the destination stack must be set to down, then read/write while the source is un-modified.
	}

}
