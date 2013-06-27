package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
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
		destSynapse = new StubSynapseAdministration("destination");
		sourceSynapse = new StubSynapseAdministration("source");
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
	
	/**
	 * Test the full migration of data from the source to destination.
	 * @throws Exception 
	 * 
	 */
	@Test
	public void testMigrateAllTypes() throws Exception{
		// Setup the destination
		LinkedHashMap<MigrationType, List<RowMetadata>> metadata = new LinkedHashMap<MigrationType, List<RowMetadata>>();
		// The first element should get deleted and second should get updated.
		List<RowMetadata> list = createList(new Long[]{0L, 1L}, new String[]{"e0","e1"}, new Long[]{null, null});
		metadata.put(MigrationType.values()[0], list);
		// Setup a second type with no valuse
		list = createList(new Long[]{}, new String[]{}, new Long[]{});
		metadata.put(MigrationType.values()[1], list);
		destSynapse.setMetadata(metadata);
		Stack<Long> changeNumberStack = new Stack<Long>();
		changeNumberStack.push(11l);
		changeNumberStack.push(0l);
		destSynapse.setCurrentChangeNumberStack(changeNumberStack);
		destSynapse.setMaxChangeNumber(11l);
		
		// setup the source
		metadata = new LinkedHashMap<MigrationType, List<RowMetadata>>();
		// The first element should get trigger an update and the second should trigger an add
		list = createList(new Long[]{1L, 2L}, new String[]{"e1changed","e2"}, new Long[]{null, 1l});
		metadata.put(MigrationType.values()[0], list);
		// both values should get added
		list = createList(new Long[]{4L, 5L}, new String[]{"e4","e5"}, new Long[]{null, 4L});
		metadata.put(MigrationType.values()[1], list);
		sourceSynapse.setMetadata(metadata);
		
		// Migrate the data
		migrationClient.migrateAllTypes(1l, 1000*60, 2, false);
		// Now validate the results
		List<RowMetadata> expected0 = createList(new Long[]{1L, 2L}, new String[]{"e1changed","e2"}, new Long[]{null, 1l});
		List<RowMetadata> expected1 = createList(new Long[]{4L, 5L}, new String[]{"e4","e5"}, new Long[]{null, 4L});
		// check the state of the destination.
		assertEquals(expected0, destSynapse.getMetadata().get(MigrationType.values()[0]));
		assertEquals(expected1, destSynapse.getMetadata().get(MigrationType.values()[1]));
		// Check the state of the source
		assertEquals(expected0, sourceSynapse.getMetadata().get(MigrationType.values()[0]));
		assertEquals(expected1, sourceSynapse.getMetadata().get(MigrationType.values()[1]));
		// no messages should have been played on the destination.
		assertEquals(0, destSynapse.getReplayChangeNumbersHistory().size());
		// No messages should have been played on the source
		assertEquals(0, sourceSynapse.getReplayChangeNumbersHistory().size());
	}
	
	
	/**
	 * Helper to build up lists of metdata.
	 * @param ids
	 * @param etags
	 * @param mock
	 */
	public static List<RowMetadata> createList(Long[] ids, String[] etags, Long[] parentId){
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0;  i<ids.length; i++){
			if(ids[i] == null){
				list.add(null);
			}else{
				RowMetadata row = new RowMetadata();
				row.setId(ids[i]);
				row.setEtag(etags[i]);
				row.setParentId(parentId[i]);
				list.add(row);
			}
		}
		return list;
	}

}
