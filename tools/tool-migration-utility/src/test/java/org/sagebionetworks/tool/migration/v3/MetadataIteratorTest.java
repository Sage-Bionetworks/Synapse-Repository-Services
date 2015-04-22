package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.progress.BasicProgress;

/**
 * Test for the MetadataIterator
 * 
 * @author John
 *
 */
public class MetadataIteratorTest {

	private SynapseAdminClientMockState mockStack;
	private SynapseAdminClient stubSynapse;
	
	private SynapseAdminClient mockSynapse;
	
	private MigrationType type;
	private int rowCount = 101;

	@Before
	public void before() throws Exception {
		// This test uses both stubs and mocks.
		mockSynapse = Mockito.mock(SynapseAdminClient.class);
		
		mockStack = new SynapseAdminClientMockState();
		mockStack.endpoint = "destination";

		// just used the first type for this test as they type used does not matter for this test.
		type = MigrationType.values()[0];
		
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for (int i = 0; i < rowCount; i++) {
			RowMetadata row = new RowMetadata();
			row.setId(new Long(i));
			row.setEtag("etag" + i);
			list.add(row);
		}
		mockStack.metadata.put(type, list);
		
		stubSynapse = SynapseAdminClientMocker.createMock(mockStack);
	}
	
	@Test
	public void testHappyCase(){
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		MetadataIterator iterator = new MetadataIterator(type, stubSynapse, 7 , progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		RowMetadata row = null;
		do{
			row = iterator.next();
			System.out.println(progress.getCurrentStatus());
			if(row != null){
				results.add(row);
			}
		}while(row != null);
		// Did we get what we expected?
		assertEquals(mockStack.metadata.get(type), results);
	}
	
	@Test (expected=RuntimeException.class)
	public void testFailure() throws SynapseException, JSONObjectAdapterException{
		// Throw exceptions
		BasicProgress progress = new BasicProgress();
		when(mockSynapse.getRowMetadata(any(MigrationType.class), any(Long.class), any(Long.class))).thenThrow(new IllegalStateException("one"));
		MetadataIterator iterator = new MetadataIterator(type, mockSynapse, 7, progress);
		iterator.next();
	}
}
