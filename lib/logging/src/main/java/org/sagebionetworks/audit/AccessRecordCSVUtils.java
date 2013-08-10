package org.sagebionetworks.audit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
public class AccessRecordCSVUtils {

	/**
	 * Capture the schema once for use in this class.
	 */
	private final static ObjectSchema SCHEMA;
	static {
		try {
			SCHEMA = new ObjectSchema(new JSONObjectAdapterImpl(
					AccessRecord.EFFECTIVE_SCHEMA));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * The default headers include all fields ordered alpha-numerically
	 */
	public static final String[] DEFAULT_HEADERS;
	static {
		Set<String> keySet = SCHEMA.getProperties().keySet();
		DEFAULT_HEADERS = keySet.toArray(new String[keySet.size()]);
	}
	
	/**
	 * Write all of the data from the given iterator to the given .
	 * @param batch
	 * @return The GZip bytes
	 */
	public static void writeCSVGZip(Iterator<AccessRecord> batch, OutputStream out){
		try{
			BufferedOutputStream buff = new BufferedOutputStream(out);
			GZIPOutputStream zipOut = new GZIPOutputStream(buff);
			OutputStreamWriter writer = new OutputStreamWriter(zipOut);
			writeBatchToCSV(batch, writer);
			writer.close();
			zipOut.finish();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param in
	 * @return
	 */
	public static Iterator<AccessRecord> readFromCSVGZip(InputStream in){
		try{
			// Wrap the stream in
			BufferedInputStream buffIn = new BufferedInputStream(in);
			GZIPInputStream zins = new GZIPInputStream(buffIn);
			InputStreamReader reader = new InputStreamReader(zins);
			return readFromCSV(reader);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	

	/**
	 * Write a batch of AccessRecords to the passed Writer as a CSV.
	 * 
	 * @param batch
	 *            A row will be written for each record. The order will be
	 *            maintained.
	 * @param writer
	 * @throws Exception
	 */
	public static void writeBatchToCSV(Iterator<AccessRecord> batch, Writer writer) {
		if (batch == null)
			throw new IllegalArgumentException("Batch cannot be null");
		if (writer == null)
			throw new IllegalArgumentException("Writer cannot be null");
		try{
			// Use the full key set for the headers
			CSVWriter csv = new CSVWriter(writer);
			// the first row is always the header
			csv.writeNext(DEFAULT_HEADERS);
			while(batch.hasNext()){
				AccessRecord record = batch.next();
				appendToCSV(record, csv, DEFAULT_HEADERS);
			}
			csv.flush();
			csv.close();
		}catch(Exception e){
			// convert to runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * Write a single AccessRecord to a CSV file.
	 * 
	 * @param record
	 * @param writer
	 * @param headers
	 * @throws Exception
	 */
	public static void appendToCSV(AccessRecord record, CSVWriter writer,
			String[] headers) {
		try {
			// Write the data to a JSON object
			JSONObject json = new JSONObject();
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
			record.writeToJSONObject(adapter);
			// Now write out each value
			String[] cells = new String[headers.length];
			for (int i = 0; i < headers.length; i++) {
				String header = headers[i];
				// Start with null
				if (json.has(header)) {
					Object ob = json.get(header);
					if (ob != null) {
						cells[i] = ob.toString();
					}
				}
			}
			writer.writeNext(cells);
		} catch (Exception e) {
			// convert to runtime
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * Read all of the data from the given CSV file into a list.
	 * @param reader
	 * @return
	 */
	public static List<AccessRecord> readAllFromCSV(Reader reader){
		List<AccessRecord> results = new LinkedList<AccessRecord>();
		Iterator<AccessRecord> it = readFromCSV(reader);
		while(it.hasNext()){
			results.add(it.next());
		}
		return results;
	}

	/**
	 * Iterate over all of the records in the given CSV reader. This method will
	 * stream over the file one record at a time.
	 * 
	 * @param reader
	 * @return
	 * @throws Exception
	 */
	public static Iterator<AccessRecord> readFromCSV(Reader reader) {
		try {
			final CSVReader csvReader = new CSVReader(reader);
			// The first row is the header
			final String[] header = csvReader.readNext();
			Iterator<AccessRecord> iterator = new Iterator<AccessRecord>() {

				AccessRecord last = null;

				@Override
				public boolean hasNext() {
					last = readFromCSV(csvReader, header);
					if (last == null) {
						try {
							csvReader.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					return last != null;
				}

				@Override
				public AccessRecord next() {
					return last;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException("Not supported");

				}
			};
			return iterator;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Read a single AccessRecord from a CSV file.
	 * 
	 * @param reader
	 * @param headers
	 * @return
	 */
	public static AccessRecord readFromCSV(CSVReader reader, String[] headers) {
		if (reader == null)
			throw new IllegalArgumentException("CSVReader cannot be null");
		if (headers == null)
			throw new IllegalArgumentException("Headers cannot be null");
		try {
			String[] cells = reader.readNext();
			if (cells == null)
				return null;
			if (headers.length != cells.length)
				throw new IllegalArgumentException("Expected: "
						+ headers.length + " columns but was: " + cells.length);
			// Split on the delimiter
			// Put the data back
			JSONObject json = new JSONObject();
			for (int i = 0; i < headers.length; i++) {
				String header = headers[i];
				String value = cells[i];
				if ("".equals(value)) {
					value = null;
				}
				if (value != null) {
					ObjectSchema cellScheam = SCHEMA.getProperties()
							.get(header);
					if (cellScheam == null)
						throw new IllegalArgumentException("Cannot find: "
								+ header + " in the schema");
					if (TYPE.STRING == cellScheam.getType()) {
						json.put(header, value);
					} else if (TYPE.INTEGER == cellScheam.getType()) {
						try {
							Long lvalue = Long.parseLong(value);
							json.put(header, lvalue);
						} catch (NumberFormatException e) {
							throw new RuntimeException("Expected: " + header
									+ " to be an integer but was: " + value);
						}
					} else if (TYPE.BOOLEAN == cellScheam.getType()) {
						try {
							Boolean bvalue = Boolean.parseBoolean(value);
							json.put(header, bvalue);
						} catch (NumberFormatException e) {
							throw new RuntimeException("Expected: " + header
									+ " to be an integer but was: " + value);
						}
					} else {
						throw new IllegalArgumentException("Unknown type: "
								+ cellScheam.getType());
					}
				}
			}
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
			AccessRecord record = new AccessRecord();
			record.initializeFromJSONObject(adapter);
			return record;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}


}
