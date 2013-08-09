package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.Method;

public class AccessRecordUtilsTest {
	
	List<AccessRecord> input;
	String errorStringOne;
	
	@Before
	public void before(){
		// Build up a small stack trace
		Exception error = new Exception(new RuntimeException(new IllegalArgumentException("Small error")));
		errorStringOne = AccessRecordUtils.createStackTraceString(error);
		
		input  = new LinkedList<AccessRecord>();
		// First one has a null elapse and error
		AccessRecord ar = new AccessRecord();
		ar.setTimestamp(100L);
		ar.setUserId(456L);
		ar.setMethod(Method.GET);
		ar.setRequestURL("/entity/987");
		ar.setSuccess(false);
		input.add(ar);
		// Next has an error
		ar = new AccessRecord();
		ar.setTimestamp(101L);
		ar.setUserId(1L);
		ar.setMethod(Method.POST);
		ar.setRequestURL("/foot/cow/ball");
		ar.setElapseMS(10L);
		ar.setSuccess(true);
		input.add(ar);
		// Add a Third
		ar = new AccessRecord();
		ar.setTimestamp(102L);
		ar.setUserId(null);
		ar.setMethod(Method.PUT);
		ar.setRequestURL("/foot/where/mouth/is");
		ar.setElapseMS(1000001L);
		ar.setSuccess(null);
		input.add(ar);

	}

	
	@Test
	public void testCSVRoundTrip() throws Exception{
		// Write the input to a CSV
		StringWriter writer = new StringWriter();
		AccessRecordUtils.writeBatchToCSV(input, writer);
		String csv = writer.toString();
		System.out.println(csv);
		// Now back to a list
		List<AccessRecord> results = new LinkedList<AccessRecord>();
		StringReader reader = new StringReader(csv);
		Iterator<AccessRecord> it =  AccessRecordUtils.readFromCSV(reader);
		assertNotNull(it);
		while(it.hasNext()){
			results.add(it.next());
		}
		// The results should be the same as the input
		assertEquals(input, results);
	
	}
	

}
