package org.sagebionetworks.audit;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.Method;

public class AccessRecordCollateUtilsTest {

	@Before
	public void before(){
		
	}
	
	@Test
	public void testCollateHappyCase(){
		// Create three lists to collate together.
		List<AccessRecord> one = createList(10, 100);
		List<AccessRecord> two = createList(5, 102);
		List<AccessRecord> three = createList(11, 101);
		
		// Create the three CSV files

	}
	
	/**
	 * Create a list for use in testing that is sorted on timestamp
	 * @param count
	 * @param startTimestamp
	 * @return
	 */
	public static List<AccessRecord> createList(int count, long startTimestamp){
		List<AccessRecord> list = new LinkedList<AccessRecord>();
		for(int i=0; i<count; i++){
			AccessRecord ar = new AccessRecord();
			ar.setUserId((long) i);
			ar.setElapseMS((long) (10*i));
			ar.setTimestamp(startTimestamp+i);
			ar.setMethod(Method.values()[i%4]);
			ar.setSuccess(i%2 > 0);
			ar.setRequestURL("/url/"+i);
			list.add(ar);
		}
		return list;
	}
	
	/**
	 * Create the CSV string for a batch
	 * @param list
	 * @return
	 */
	public static String createCSVString(List<AccessRecord> list){
		StringWriter writer = new StringWriter();
		AccessRecordCSVUtils.writeBatchToCSV(list, writer);
		return writer.toString();
	}
}
