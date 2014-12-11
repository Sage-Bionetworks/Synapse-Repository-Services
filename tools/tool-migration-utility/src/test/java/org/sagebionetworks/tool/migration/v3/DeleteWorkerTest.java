package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.tool.progress.BasicProgress;

/**
 * Test for DeleteWorker
 * 
 * @author John
 *
 */
public class DeleteWorkerTest {
	
	private SynapseAdminClientMockState mockStack;
	private SynapseAdminClient stubSynapse;

	private MigrationType type;
	private int rowCount = 15;

	@Before
	public void before() throws Exception {
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
	public void testDelete() throws Exception{
		List<RowMetadata> toDelete = buildList(new Long[][] {
				new Long[] { 1l, null }, 
				new Long[] { 5l, null },
				new Long[] { 6l, 30l }, 
				new Long[] { 10l, 4l },
				new Long[] { 11l, 4l }, });
		DeleteWorker worker = new DeleteWorker(type, toDelete.size(), toDelete.iterator(), new BasicProgress(), stubSynapse, 2);
		Long result = worker.call();
		assertEquals(new Long(toDelete.size()), result);
		List<RowMetadata> endingData = mockStack.metadata.get(type);
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
		// Have an exception be thrown on Node 6
		mockStack.exceptionNodes.add(6L);
		
		List<RowMetadata> toDelete = buildList(new Long[][]{
				new Long[]{1l,null},
				new Long[]{5l,null},
				new Long[]{6l,30l},
				new Long[]{10l,4l},
				new Long[]{11l,4l},
		});
		DeleteWorker worker = new DeleteWorker(type, toDelete.size(), toDelete.iterator(), new BasicProgress(), stubSynapse, 2);
		try {
			worker.call();
		} catch (Exception e) {
			System.out.println(mockStack.deleteRequestsHistory);
			assertEquals(4, mockStack.deleteRequestsHistory.size());
		}
	}
	
	
	/**
	 * Buildup a list from an simple array. The first long is the id the second long
	 * is the parent id
	 */
	private List<RowMetadata> buildList(Long[][] data){
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
