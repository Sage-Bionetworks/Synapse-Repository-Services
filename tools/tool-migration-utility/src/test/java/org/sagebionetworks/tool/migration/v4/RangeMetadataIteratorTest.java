package org.sagebionetworks.tool.migration.v4;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v3.SynapseAdminClientMockState;
import org.sagebionetworks.tool.progress.BasicProgress;

public class RangeMetadataIteratorTest {

	private SynapseAdminClientMockState mockStack;
	private SynapseAdminClient stubSynapse;
	
	private SynapseAdminClient mockSynapse;
	
	private MigrationType type;
	private int rowCount = 100;
	List<RowMetadata> originals;

	@Before
	public void before() throws Exception {
		// This test uses both stubs and mocks.
		mockSynapse = Mockito.mock(SynapseAdminClient.class);
		
		mockStack = new SynapseAdminClientMockState();
		mockStack.endpoint = "destination";

		// just used the first type for this test as they type used does not matter for this test.
		type = MigrationType.values()[0];
		
		originals = new LinkedList<RowMetadata>();
		for (int i = 0; i < rowCount; i++) {
			RowMetadata row = new RowMetadata();
			row.setId(new Long(i));
			row.setEtag("etag" + i);
			originals.add(row);
		}
		mockStack.metadata.put(type, originals);
		
		stubSynapse = SynapseAdminClientMocker.createMock(mockStack);
	}
	
	@Test
	public void testHappyCaseSinglePage() {
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 5, 50 , 54, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		List<RowMetadata> expectedResult = new LinkedList<RowMetadata>();
		for (int id = 50; id <= 54; id++) {
			expectedResult.add(mockStack.metadata.get(type).get(id));
		}
		assertEquals(expectedResult, results);
		
		// Calling next() now should throw an exception
		try {
			iterator.next();
		} catch (NoSuchElementException e) {
			// hasNext() has been called in the loop, so this is the correct exception
			System.out.println("Caught NoSuchElementException.");
		}
	}
	
	@Test
	public void testHappyCaseMultiplePages() {
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 6, 50 , 64, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		List<RowMetadata> expectedResult = new LinkedList<RowMetadata>();
		for (int id = 50; id <= 64; id++) {
			expectedResult.add(mockStack.metadata.get(type).get(id));
		}
		assertEquals(expectedResult, results);
		
		// Calling next() now should throw an exception
		try {
			iterator.next();
		} catch (NoSuchElementException e) {
			// hasNext() has been called in the loop, so this is the correct exception
			System.out.println("Caught NoSuchElementException.");
		}
	}

	@Test
	public void testHappyCaseEven() {
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 2, 0 , rowCount, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		assertEquals(originals, results);
		
	}

	@Test
	public void testHappyCaseOdd() {
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 33, 0 , rowCount, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		assertEquals(originals, results);
		
	}

	@Test
	public void testHappyCaseFull() {
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, rowCount, 0 , rowCount, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		assertEquals(originals, results);
		
	}

	@Test
	public void testHappyCaseBatchGreaterThanRange() {
		long batchSize = rowCount+1;
		long minId = 0;
		long maxId = rowCount;
		// Iterate over all data
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, batchSize, minId , maxId, progress);
		// We should be able to iterate over all of the data and end up with list
		// the same as used by the stub
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// Did we get what we expected?
		assertEquals(originals, results);
		
	}

	@Test(expected=IllegalStateException.class)
	public void testNoCallToHasNext1() {
		BasicProgress progress = new BasicProgress();
		RangeMetadataIterator iterator = new RangeMetadataIterator(type, stubSynapse, 5, 60 , 64, progress);
		RowMetadata row = iterator.next();
	}
	
}
