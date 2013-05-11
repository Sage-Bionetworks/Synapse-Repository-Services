package org.sagebionetworks.tool.migration.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.migration.RowMetadata;

/**
 * Test for the migration delta detection.
 * 
 * @author John
 *
 */
public class DeltaBuilderTest {
	
	
	ByteArrayOutputStream create;
	DataOutputStream createDataOut;
	ByteArrayOutputStream update;
	DataOutputStream updateDataOut;
	ByteArrayOutputStream delete;
	DataOutputStream deleteDataOut;
	
	@Before
	public void before(){
		create = new ByteArrayOutputStream();
		createDataOut = new DataOutputStream(create);
		update = new ByteArrayOutputStream();
		updateDataOut = new DataOutputStream(update);
		delete = new ByteArrayOutputStream();
		deleteDataOut = new DataOutputStream(delete);
	}

	/**
	 * For this case both the source and destination are equal
	 * @throws Exception 
	 */
	@Test
	public void testNoDelta() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e0","e2","e3", null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e0","e2","e3", null});
		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(0, counts.getCreate());
		assertEquals(0, counts.getUpdate());
		assertEquals(0, counts.getDelete());
		// Create the input streams
		// create
		List<Long> results = readFromStream(create);
		assertTrue(results.isEmpty());
		// update
		results = readFromStream(update);
		assertTrue(results.isEmpty());
		// delete
		results = readFromStream(delete);
		assertTrue(results.isEmpty());
	}
	
	/**
	 * For this case both the source and destination are same size but there are etag changes
	 * @throws Exception 
	 */
	@Test
	public void testUpdates() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e0","e2","e3", null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e5","e2","e4", null});
		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(0, counts.getCreate());
		assertEquals(2, counts.getUpdate());
		assertEquals(0, counts.getDelete());
		// Create the input streams
		// create
		List<Long> results = readFromStream(create);
		assertTrue(results.isEmpty());
		// update
		results = readFromStream(update);
		assertEquals(2, results.size());
		assertEquals(new Long(0), results.get(0));
		assertEquals(new Long(2), results.get(1));
		//delete
		results = readFromStream(delete);
		assertTrue(results.isEmpty());
	}
	
	/**
	 * For this there is not data in the destination.
	 * @throws Exception 
	 */
	@Test
	public void testDestEmpty() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e0","e2","e3", null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{null, null, null, null}, new String[]{null, null, null, null});
		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(3, counts.getCreate());
		assertEquals(0, counts.getUpdate());
		assertEquals(0, counts.getDelete());
		// Create the input streams
		// create
		List<Long> results = readFromStream(create);
		assertEquals(3, results.size());
		assertEquals(new Long(0), results.get(0));
		assertEquals(new Long(1), results.get(1));
		assertEquals(new Long(2), results.get(2));
		// update
		results = readFromStream(update);
		assertTrue(results.isEmpty());
		// delte
		results = readFromStream(delete);
		assertTrue(results.isEmpty());
	}
	
	/**
	 * For this there is not data source
	 * @throws Exception 
	 */
	@Test
	public void testSourceEmpty() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{null, null, null, null}, new String[]{null, null, null, null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{"0","1","2", null}, new String[]{"e0","e2","e3", null});

		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(0, counts.getCreate());
		assertEquals(0, counts.getUpdate());
		assertEquals(3, counts.getDelete());
		// Create the input streams
		// create
		List<Long> results = readFromStream(create);
		assertTrue(results.isEmpty());
		// update
		results = readFromStream(update);
		assertTrue(results.isEmpty());
		// delete
		results = readFromStream(delete);
		assertEquals(3, results.size());
		assertEquals(new Long(0), results.get(0));
		assertEquals(new Long(1), results.get(1));
		assertEquals(new Long(2), results.get(2));

	}
	
	/**
	 * For this there is not data source
	 * @throws Exception 
	 */
	@Test
	public void testMixed() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{"0","2","3","4", null, null}, new String[]{"e0","e22","e3","e4", null, null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{"0","1","2","5","6", null}, new String[]{"e0","e1","e2","e5","e6", null});

		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(2, counts.getCreate());
		assertEquals(1, counts.getUpdate());
		assertEquals(3, counts.getDelete());
		// create
		List<Long> results = readFromStream(create);
		assertEquals(2, results.size());
		assertEquals(new Long(3), results.get(0));
		assertEquals(new Long(4), results.get(1));
		// update
		results = readFromStream(update);
		assertEquals(1, results.size());
		assertEquals(new Long(2), results.get(0));
		// delete
		results = readFromStream(delete);
		assertEquals(3, results.size());
		assertEquals(new Long(1), results.get(0));
		assertEquals(new Long(5), results.get(1));
		assertEquals(new Long(6), results.get(2));
	}
	
	/**
	 * For this there is not data source
	 * @throws Exception 
	 */
	@Test
	public void testMixed2() throws Exception{
		Iterator<RowMetadata> srcIt = createIterator(new String[]{"0","4","5","6","7","8", null}, new String[]{"e0","e4","e5","e6","e7","e8", null});
		Iterator<RowMetadata> desIt = createIterator(new String[]{"0","1","2","3","6","8", null}, new String[]{"e01","e1","e2","e3","e6","e81", null});

		DeltaBuilder builder = new DeltaBuilder(srcIt, desIt, createDataOut, updateDataOut, deleteDataOut);
		// Build the deltas
		DeltaCounts counts = builder.call();
		assertEquals(3, counts.getCreate());
		assertEquals(2, counts.getUpdate());
		assertEquals(3, counts.getDelete());
		// create
		List<Long> results = readFromStream(create);
		assertEquals(3, results.size());
		assertEquals(new Long(4), results.get(0));
		assertEquals(new Long(5), results.get(1));
		assertEquals(new Long(7), results.get(2));
		// update
		results = readFromStream(update);
		assertEquals(2, results.size());
		assertEquals(new Long(0), results.get(0));
		assertEquals(new Long(8), results.get(1));
		// delete
		results = readFromStream(delete);
		assertEquals(3, results.size());
		assertEquals(new Long(1), results.get(0));
		assertEquals(new Long(2), results.get(1));
		assertEquals(new Long(3), results.get(2));
	}
	
	/**
	 * Read a list of longs form the stream
	 * @param out
	 * @return
	 * @throws IOException
	 */
	private static List<Long> readFromStream(ByteArrayOutputStream out) throws IOException{
		List<Long> results = new LinkedList<Long>();
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
		while(true){
			try{
				long value = in.readLong();
				results.add(new Long(value));
			} catch (EOFException e){
				break;
			}
		}
		return results;
	}
	
	/**
	 * Setup a mock with the given data
	 * @param ids
	 * @param etags
	 * @param mock
	 */
	public static Iterator<RowMetadata> createIterator(String[] ids, String[] etags){
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0;  i<ids.length; i++){
			if(ids[i] == null){
				list.add(null);
			}else{
				RowMetadata row = new RowMetadata();
				row.setId(ids[i]);
				row.setEtag(etags[i]);
				list.add(row);
			}
		}
		return list.iterator();
	}
}
