package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.audit.AccessRecord;

public class AccessRecordCSVUtilsTest {

	
	@Test
	public void testCSVRoundTrip() throws Exception{
		List<AccessRecord> input = AuditTestUtils.createList(10, 100);
		// Write the input to a CSV
		StringWriter writer = new StringWriter();
		AccessRecordCSVUtils.writeBatchToCSV(input.iterator(), writer);
		String csv = writer.toString();
		System.out.println(csv);
		// Now back to a list
		List<AccessRecord> results = new LinkedList<AccessRecord>();
		StringReader reader = new StringReader(csv);
		Iterator<AccessRecord> it =  AccessRecordCSVUtils.readFromCSV(reader);
		assertNotNull(it);
		while(it.hasNext()){
			results.add(it.next());
		}
		// The results should be the same as the input
		assertEquals(input, results);
	
	}
	
	@Test
	public void testGZipRoundTrip() throws IOException{
		List<AccessRecord> input = AuditTestUtils.createList(1000, 100);
		// Write to a GZip
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AccessRecordCSVUtils.writeCSVGZip(input.iterator(), out);
		// Now convert the 
		byte[] zippedBytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(zippedBytes);
//		File temp = File.createTempFile("sample", ".gz");
//		System.out.println(temp.getAbsolutePath());
//		FileOutputStream fos = new FileOutputStream(temp);
//		fos.write(zippedBytes);
//		fos.close();
		// Now read it in
		Iterator<AccessRecord> it = AccessRecordCSVUtils.readFromCSVGZip(in);
		List<AccessRecord> results  = new LinkedList<AccessRecord>();
		while(it.hasNext()){
			results.add(it.next());
		}
		// It should be the same as the input
		assertEquals(input, results);
	}
	

}
