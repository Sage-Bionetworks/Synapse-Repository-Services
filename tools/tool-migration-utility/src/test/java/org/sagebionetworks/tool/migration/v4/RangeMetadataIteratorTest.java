package org.sagebionetworks.tool.migration.v4;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.progress.BasicProgress;

public class RangeMetadataIteratorTest {

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
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 10, 0 , 7, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		RowMetadata row = null;
		do {
			row = iterator.next();
			System.out.println(progress.getCurrentStatus());
			if(row != null){
				results.add(row);
			}
		} while (row != null);
		// Did we get what we expected?
		List<RowMetadata> expectedResult = new LinkedList<RowMetadata>();
		for (int id = 0; id <= 7; id++) {
			expectedResult.add(mockStack.metadata.get(type).get(id));
		}
		assertEquals(expectedResult, results);
	}
	
	@Test (expected=RuntimeException.class)
	public void testFailure() throws SynapseException, JSONObjectAdapterException{
		// Throw exceptions
		BasicProgress progress = new BasicProgress();
		when(mockSynapse.getRowMetadataByRange(any(MigrationType.class), any(Long.class), any(Long.class))).thenThrow(new IllegalStateException("one"));
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, mockSynapse, 7, 0, rowCount-1, progress);
		iterator.next();
	}}
