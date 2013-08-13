package org.sagebionetworks.audit;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Write an objects to a CSV stream where each Object represents a row. The
 * first row of the resulting CSV will be a header that maps field names to each
 * column. This header row is required for the {@link ObjectCSVReader} to parse
 * the data correctly.
 * 
 * Note: This only works for very simple objects with fields of type: String,
 * Long, and Boolean.
 * 
 * @author jmhill
 * 
 * @param <T>
 */
public class ObjectCSVWriter<T> {

	Class<T> clazz;
	String[] buffer;
	CSVWriter csv;
	Field[] fields;

	/**
	 * Create a new
	 * 
	 * @param out
	 *            The Writer that will receive the data.
	 * @param clazz
	 *            The class of the objects that will be written to thei stream.
	 */
	public ObjectCSVWriter(Writer out, Class<T> clazz) {
		if (out == null)
			throw new IllegalArgumentException("Writer cannot be null");
		if (clazz == null)
			throw new IllegalArgumentException("Class cannot be null");
		this.clazz = clazz;
		// Use all fields as the header
		Field[] allFields = clazz.getDeclaredFields();
		List<Field> filteredFields = new LinkedList<Field>();
		for(Field field: allFields){
			if((field.getModifiers() & Modifier.STATIC) == 0){
				filteredFields.add(field);
			}
		}
		fields = filteredFields.toArray(new Field[filteredFields.size()]);
		buffer = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
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
	 * Append a row to this writer.
	 * 
	 * @param row
	 */
	public void append(T row) {
		// Extract the row
		try {
			for (int i = 0; i < fields.length; i++) {
				Object ob = fields[i].get(row);
				if (ob != null) {
					buffer[i] = ob.toString();
				} else {
					buffer[i] = null;
				}
			}
			// Write the row
			csv.writeNext(buffer);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Close should be called when done.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		csv.close();
	}
}
