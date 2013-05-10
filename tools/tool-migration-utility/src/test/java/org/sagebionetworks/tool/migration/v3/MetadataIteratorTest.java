package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * Test for the MetadataIterator
 * 
 * @author John
 *
 */
public class MetadataIteratorTest {
	
	int rowCount = 101;
	StubSynapseAdministration stubSynapse;
	StubSynapseAdministration mockSynapse;
	LinkedHashMap<MigrationType, List<RowMetadata>> rawData;
	MigrationType type;

	@Before
	public void before(){
		// This test uses both stubs and mocks.
		stubSynapse = new StubSynapseAdministration("destination");
		mockSynapse = Mockito.mock(StubSynapseAdministration.class);
		
		// just used the first type for this test as they type used does not matter for this test.
		type = MigrationType.values()[0];
		
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0; i<rowCount; i++){
			RowMetadata row = new RowMetadata();
			row.setId(""+i);
			row.setEtag("etag"+i);
			list.add(row);
		}
		rawData = new LinkedHashMap<MigrationType, List<RowMetadata>>();
		rawData.put(type, list);
		stubSynapse.setMetadata(rawData);
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
		assertEquals(rawData.get(type), results);
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
