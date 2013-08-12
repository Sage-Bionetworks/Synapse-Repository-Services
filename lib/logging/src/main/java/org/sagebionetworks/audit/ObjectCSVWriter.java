package org.sagebionetworks.audit;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;

import au.com.bytecode.opencsv.CSVWriter;


public class ObjectCSVWriter<T> {

	Class<T> clazz;
	String[] buffer;
	CSVWriter csv;
	Field[] fields;
	
	public ObjectCSVWriter(Writer out, Class<T> clazz){
		this.clazz = clazz;
		// Use all fields as the header
		fields = clazz.getDeclaredFields();
		buffer = new String[fields.length];
		for(int i=0; i<fields.length; i++){
			Field field = fields[i];
			// This will allow us to read/write private fields.
			field.setAccessible(true);
			buffer[i] = field.getName();
		}
		// Create the writer
		csv = new CSVWriter(out);
		// the first row is always the header
		csv.writeNext(buffer);
	}
	
	/**
	 * Append a row.
	 * @param row
	 */
	public void append(T row){
		// Extract the row
		try{
			for(int i=0; i<fields.length; i++){
				Object ob = fields[i].get(row);
				if(ob != null){
					buffer[i] = ob.toString();
				}else{
					buffer[i] = null;
				}
			}
			// Write the row
			csv.writeNext(buffer);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Close when done.
	 * @throws IOException
	 */
	public void close() throws IOException{
		csv.close();
	}
}
