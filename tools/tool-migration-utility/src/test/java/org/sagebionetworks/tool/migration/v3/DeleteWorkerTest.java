package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * Test for DeleteWorker
 * 
 * @author John
 *
 */
public class DeleteWorkerTest {

	int rowCount = 15;
	StubSynapseAdministration stubSynapse;
	StubSynapseAdministration mockSynapse;
	LinkedHashMap<MigrationType, List<RowMetadata>> rawData;
	MigrationType type;
	DeleteWorker worker;

	@Before
	public void before(){
		// This test uses both stubs and mocks.
		stubSynapse = new StubSynapseAdministration("destination");
		
		// just used the first type for this test as they type used does not matter for this test.
		type = MigrationType.values()[0];
		
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0; i<rowCount; i++){
			RowMetadata row = new RowMetadata();
			row.setId(new Long(i));
			row.setEtag("etag"+i);
			list.add(row);
		}
		rawData = new LinkedHashMap<MigrationType, List<RowMetadata>>();
		rawData.put(type, list);
		stubSynapse.setMetadata(rawData);

	}
	
	@Test
	public void testDelete() throws Exception{
		List<Long> toDelete = new LinkedList<Long>();
		toDelete.add(1l);
		toDelete.add(5l);
		toDelete.add(6l);
		toDelete.add(10l);
		toDelete.add(11l);
		DeleteWorker worker = new DeleteWorker(type, toDelete.size(), toDelete.iterator(), new BasicProgress(), stubSynapse, 2);
		Long result = worker.call();
		assertEquals(new Long(toDelete.size()), result);
		List<RowMetadata> endingData = stubSynapse.getMetadata().get(type);
		HashSet<String> deleteSet = new HashSet<String>();
		for(Long id: toDelete){
			deleteSet.add(id.toString());
		}
		System.out.println(deleteSet);
		System.out.println(endingData);
		for(RowMetadata row: endingData){
			assertFalse(deleteSet.contains(row.getId()));
		}
	}
}
