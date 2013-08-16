package org.sagebionetworks.audit.utils;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for both ObjectCSVReader and the ObjectCSVWriter.
 *
 */
public class ObjectCSVTest {
	
	List<ExampleObject> data;
	
	@Before
	public void before(){
		// Build up some sample data
		int count = 12;
		data = new LinkedList<ExampleObject>();
		for(int i=0; i<count; i++){
			ExampleObject ob = new ExampleObject();
			ob.setaBoolean(i%2 == 0);
			ob.setaString("Value,"+i);
			ob.setaLong(new Long(11*i));
			ob.setaDouble(12312312.34234/i);
			ob.setAnInteger(new Integer(i));
			ob.setaFloat(new Float(123.456*i));
			// Add some nulls
			if(i%3 == 0){
				ob.setaBoolean(null);
			}
			if(i%4 == 0){
				ob.setaString(null);
			}
			if(i%5 == 0){
				ob.setaLong(null);
			}
			data.add(ob);
		}
	}
	
	@Test
	public void testGetNonStaticFieldNames(){
		String[] expected = new String[]{"aString","aLong","aBoolean","aDouble","anInteger","aFloat"};
		String[] restuls = ObjectCSVWriter.getNonStaticFieldNames(ExampleObject.class);
		assertTrue(Arrays.equals(expected, restuls));
	}
	
	@Test
	public void testWriteStaticFields() throws IOException{
		StringWriter writer = new StringWriter();
		ObjectCSVWriter<ExampleObject> csv = new ObjectCSVWriter<ExampleObject>(writer, ExampleObject.class);
		// Write each row
		for(ExampleObject ob: data){
			csv.append(ob);
		}
		csv.close();
		String stringCSV = writer.toString();
		assertFalse("Static fields should not be included in the CSV",stringCSV.indexOf(ExampleObject.A_STATIC_FIEDS) > 0);
	}

	@Test
	public void testRoundTrip() throws IOException{
		// First write to a CSV string
		StringWriter writer = new StringWriter();
		ObjectCSVWriter<ExampleObject> csv = new ObjectCSVWriter<ExampleObject>(writer, ExampleObject.class);
		// Write each row
		for(ExampleObject ob: data){
			csv.append(ob);
		}
		csv.close();
		String stringCSV = writer.toString();
//		System.out.println(stringCSV);
		// Now make sure we can read the data
		List<ExampleObject> results = new LinkedList<ExampleObject>();
		StringReader reader = new StringReader(stringCSV);
		ObjectCSVReader<ExampleObject> csvReader = new ObjectCSVReader<ExampleObject>(reader, ExampleObject.class);
		ExampleObject row = null;
		while((row = csvReader.next()) != null){
			results.add(row);
		}
		csvReader.close();
		// The results should match the original
		assertEquals(data, results);
	}
	
	@Test
	public void testBackwardsCompatibility() throws IOException{
		// First write to a CSV string
		StringWriter writer = new StringWriter();
		ObjectCSVWriter<ExampleObject> csv = new ObjectCSVWriter<ExampleObject>(writer, ExampleObject.class);
		// Write each row
		for(ExampleObject ob: data){
			csv.append(ob);
		}
		csv.close();
		String stringCSV = writer.toString();
//		System.out.println(stringCSV);
		// Now make sure we can read the data into our new object
		List<ExampleObjectV2> results = new LinkedList<ExampleObjectV2>();
		StringReader reader = new StringReader(stringCSV);
		ObjectCSVReader<ExampleObjectV2> csvReader = new ObjectCSVReader<ExampleObjectV2>(reader, ExampleObjectV2.class);
		ExampleObjectV2 row = null;
		while((row = csvReader.next()) != null){
			results.add(row);
		}
		csvReader.close();
		// The results should match the original
		assertEquals(data.size(), results.size());
		// Check the columns that still map
		ExampleObject oldTwo = data.get(1); 
		ExampleObjectV2 newTwo = results.get(1); 
		// This field was renamed so does not map and should be null
		assertEquals(null, newTwo.getBetterFloat());
		assertEquals(oldTwo.getaDouble(), newTwo.getaDouble());
		assertEquals(oldTwo.getaString(), newTwo.getaString());
		assertEquals(oldTwo.getAnInteger(), newTwo.getAnInteger());
	}
	
	@Test
	public void testRoundTripWithHeaders() throws IOException{
		// For this case we are providing the headers.
		String[] headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat"};
		// Use the headers to build up the CSV.
		StringWriter writer = new StringWriter();
		ObjectCSVWriter<ExampleObject> csv = new ObjectCSVWriter<ExampleObject>(writer, ExampleObject.class, headers);
		// Write each row
		for(ExampleObject ob: data){
			csv.append(ob);
		}
		csv.close();
		String stringCSV = writer.toString();
//		System.out.println(stringCSV);
		// Now make sure we can read the data into our new object
		List<ExampleObject> results = new LinkedList<ExampleObject>();
		StringReader reader = new StringReader(stringCSV);
		ObjectCSVReader<ExampleObject> csvReader = new ObjectCSVReader<ExampleObject>(reader, ExampleObject.class, headers);
		ExampleObject row = null;
		while((row = csvReader.next()) != null){
//			System.out.println(row);
			results.add(row);
		}
		csvReader.close();
		// The results should match the original
		assertEquals(data, results);
	}
	
	@Test
	public void testRoundTripWithHeadersBackwardsCompatibility() throws IOException{
		// For this case we are providing the headers.
		String[] headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat"};
		// Use the headers to build up the CSV.
		StringWriter writer = new StringWriter();
		ObjectCSVWriter<ExampleObject> csv = new ObjectCSVWriter<ExampleObject>(writer, ExampleObject.class, headers);
		// Write each row
		for(ExampleObject ob: data){
			csv.append(ob);
		}
		csv.close();
		String stringCSV = writer.toString();
//		System.out.println(stringCSV);
		// Now make sure we can read the data into our new object
		List<ExampleObjectV2> results = new LinkedList<ExampleObjectV2>();
		StringReader reader = new StringReader(stringCSV);
		ObjectCSVReader<ExampleObjectV2> csvReader = new ObjectCSVReader<ExampleObjectV2>(reader, ExampleObjectV2.class, headers);
		ExampleObjectV2 row = null;
		while((row = csvReader.next()) != null){
			results.add(row);
		}
		csvReader.close();
		// The results should match the original
		assertEquals(data.size(), results.size());
		// Check the columns that still map
		ExampleObject oldTwo = data.get(1); 
		ExampleObjectV2 newTwo = results.get(1); 
		// This field was renamed so does not map and should be null
		assertEquals(null, newTwo.getBetterFloat());
		assertEquals(oldTwo.getaDouble(), newTwo.getaDouble());
		assertEquals(oldTwo.getaString(), newTwo.getaString());
		assertEquals(oldTwo.getAnInteger(), newTwo.getAnInteger());
	}
}
