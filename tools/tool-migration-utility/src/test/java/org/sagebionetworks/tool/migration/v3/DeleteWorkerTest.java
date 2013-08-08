package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
		List<RowMetadata> toDelete = buildList(new Long[][]{
				new Long[]{1l,null},
				new Long[]{5l,null},
				new Long[]{6l,30l},
				new Long[]{10l,4l},
				new Long[]{11l,4l},
		});
		DeleteWorker worker = new DeleteWorker(type, toDelete.size(), toDelete.iterator(), new BasicProgress(), stubSynapse, 2);
		Long result = worker.call();
		assertEquals(new Long(toDelete.size()), result);
		List<RowMetadata> endingData = stubSynapse.getMetadata().get(type);
		HashSet<Long> deleteSet = new HashSet<Long>();
		for(RowMetadata row: toDelete){
			deleteSet.add(row.getId());
		}
		System.out.println(deleteSet);
		System.out.println(endingData);
		for(RowMetadata row: endingData){
			assertFalse(deleteSet.contains(row.getId()));
		}
	}
	
	@Test
	public void testDeleteWithRetry() throws Exception{
		List<RowMetadata> toDelete = buildList(new Long[][]{
				new Long[]{1l,null},
				new Long[]{5l,null},
				new Long[]{6l,30l},
				new Long[]{10l,4l},
				new Long[]{11l,4l},
		});
		DeleteWorker worker = new DeleteWorker(type, toDelete.size(), toDelete.iterator(), new BasicProgress(), stubSynapse, 2);
		Long result = -1L;
		Set<Long> exceptionNodes = new HashSet<Long>();
		exceptionNodes.add(6L);
		stubSynapse.setExceptionNodes(exceptionNodes);
		try {
			result = worker.call();
		} catch (Exception e) {
			System.out.println(stubSynapse.getDeleteRequestsHistory());
			assertEquals(4, stubSynapse.getDeleteRequestsHistory().size());
		}
	}
	
	
	/**
	 * Buildup a list from an simple array. The first long is the id the second long
	 * is the parent id.
	 * @param data
	 * @return
	 */
	List<RowMetadata> buildList(Long[][] data){
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0; i<data.length; i++){
			RowMetadata row = new RowMetadata();
			row.setId(data[i][0]);
			row.setParentId(data[i][1]);
			list.add(row);
		}
		return list;
	}
}
