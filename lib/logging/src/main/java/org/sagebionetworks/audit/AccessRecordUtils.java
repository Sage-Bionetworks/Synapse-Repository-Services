package org.sagebionetworks.audit;

import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Utilities for working with AccessRecord objects.
 * 
 * @author jmhill
 *
 */
public class AccessRecordUtils {
	
	public static final String DELIMITER = ",";
	
	private final static ObjectSchema SCHEMA;
	static{
		try {
			SCHEMA = new ObjectSchema(new JSONObjectAdapterImpl(AccessRecord.EFFECTIVE_SCHEMA));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Write a single AccessRecord to a CSV file.
	 * @param record
	 * @param writer
	 * @param headers
	 * @throws Exception
	 */
	public static void appendToCSV(AccessRecord record, CSVWriter writer, String[] headers) throws Exception{
		// Write the data to a JSON object 
		JSONObject json = new JSONObject();
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
		record.writeToJSONObject(adapter);
		// Now write out each value
		String[] cells = new String[headers.length];
		for(int i=0; i<headers.length;i++){
			String header = headers[i];
			// Start with null
			if(json.has(header)){
				Object ob = json.get(header);
				if(ob != null){
					cells[i] =  ob.toString();
				}
			}
		}
		writer.writeNext(cells);
	}
	
	public static void writeBatchToCSV(List<AccessRecord> batch){
		
	}

	/**
	 * Read a single AccessRecord from a CSV file.
	 * @param reader
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	public AccessRecord readFromCSV(CSVReader reader, String[] headers) throws Exception {
		String[] cells = reader.readNext();
		if(cells == null) return null;
		if(headers.length != cells.length) throw new IllegalArgumentException("Expected: "+headers.length+" columns but was: "+cells.length);
		// Split on the delimiter
		// Put the data back
		JSONObject json = new JSONObject();
		for(int i=0; i<headers.length; i++){
			String header = headers[i];
			String value = cells[i];
			ObjectSchema cellScheam = SCHEMA.getProperties().get(header);
			if(cellScheam == null) throw new IllegalArgumentException("Cannot find: "+header+" in the schema");
			if(TYPE.STRING == cellScheam.getType()){
				json.put(header, value);
			}else if(TYPE.INTEGER == cellScheam.getType()){
				Long lvalue = Long.parseLong(value);
				json.put(header, lvalue);
			}else{
				throw new IllegalArgumentException("Unknown type: "+cellScheam.getType());
			}
		}
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
		AccessRecord record = new AccessRecord();
		record.initializeFromJSONObject(adapter);
		return record;
	}
	
	/**
	 * This Comparator compares AccessRecord based on the time stamp. 
	 * @author jmhill
	 *
	 */
	public static class AccessRecordComparator implements Comparator<AccessRecord>{

		@Override
		public int compare(AccessRecord one, AccessRecord two) {
			if(one == null) throw new IllegalArgumentException("One cannot be null");
			if(one.getTimestamp() == null) throw new IllegalArgumentException("One.timestamp cannot be null");
			if(two == null) throw new IllegalArgumentException("Two cannot be null");
			if(two.getTimestamp() == null) throw new IllegalArgumentException("Two.timestamp cannot be null");
			return one.getTimestamp().compareTo(two.getTimestamp());
		}
		
	}
}
