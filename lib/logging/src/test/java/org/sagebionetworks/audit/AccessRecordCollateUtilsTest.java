package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.audit.AccessRecord;

public class AccessRecordCollateUtilsTest {

	@Before
	public void before(){
		
	}
	
	@Test
	public void testCollateHappyCase() throws UnsupportedEncodingException{
		// Create three lists to collate together.
		List<AccessRecord> one = AuditTestUtils.createList(10, 100);
		List<AccessRecord> two = AuditTestUtils.createList(5, 102);
		List<AccessRecord> three = AuditTestUtils.createList(11, 101);
		Set<String> inputSessionIds = getSessionIdsSet(one, two, three);
		
		// Create the three CSV files
		List<Reader> readers = new LinkedList<Reader>();
		readers.add(new StringReader(createCSVString(one)));
		readers.add(new StringReader(createCSVString(two)));
		readers.add(new StringReader(createCSVString(three)));
		// Now collate
		StringWriter out = new StringWriter();
		AccessRecordCollateUtils.collateSortedCSVReaders(readers, out);
		String outCSV = out.toString();
		System.out.println(outCSV);
		// Get the results
		List<AccessRecord> results = AccessRecordCSVUtils.readAllFromCSV(new StringReader(outCSV));
		assertNotNull(results);
		int expectedSize = one.size()+two.size()+three.size();
		assertEquals(expectedSize, results.size());
		Set<String> resultSessionIds = getSessionIdsSet(results);
		assertTrue("SessionId were lost!",resultSessionIds.containsAll(inputSessionIds));
		assertTrue("SessionId were introduced!", inputSessionIds.containsAll(resultSessionIds));
		// Validate they are collated
		validateCollated(results);
	}
	
	/**
	 * Validate that each record has a timestamp that is greater than or equal to the previous timestamp.
	 * @param results
	 */
	public static void validateCollated(List<AccessRecord> results){
		for(int i=1; i<results.size(); i++){
			AccessRecord previous = results.get(i-1);
			AccessRecord thisRecord = results.get(i);
			assertTrue("The results are not collated. timestamp: "+previous.getTimestamp()+" appears before "+thisRecord.getTimestamp(),previous.getTimestamp().compareTo(thisRecord.getTimestamp()) < 1) ;
		}
		
	}
	
	/**
	 * Create the CSV string for a batch
	 * @param list
	 * @return
	 */
	public static String createCSVString(List<AccessRecord> list){
		StringWriter writer = new StringWriter();
		AccessRecordCSVUtils.writeBatchToCSV(list.iterator(), writer);
		return writer.toString();
	}
	
	/**
	 * Extract the set of session IDs.
	 * @param array
	 * @return
	 */
	private static Set<String> getSessionIdsSet(List<AccessRecord>...array){
		HashSet<String> set = new HashSet<String>();
		for(List<AccessRecord> list: array){
			for(AccessRecord record: list){
				if(record.getSessionId() == null) throw new IllegalArgumentException("SessionID cannot be null");
				if(!set.add(record.getSessionId())){
					throw new IllegalArgumentException("Duplicate sessionId found: "+record.getSessionId());
				}
			}
		}
		return set;
	}
}
