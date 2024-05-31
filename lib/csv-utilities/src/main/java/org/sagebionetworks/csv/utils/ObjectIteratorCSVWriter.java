package org.sagebionetworks.csv.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

public class ObjectIteratorCSVWriter {

	/**
	 * Read each record from the iterator and write it to an output stream
	 * 
	 * @param it - the iterator to get records
	 * @param out - the output stream to write records to
	 * @param headers - the format of a record
	 * @param clazz - the type of the record
	 * @throws IOException 
	 */
	public static <T> void write(Iterator<T> it, OutputStream out, String[] headers, Class<T> clazz) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(out);
		ObjectCSVWriter<T> writer = new ObjectCSVWriter<T>(osw, clazz, headers);
		while (it.hasNext()) {
			T record = it.next();
			writer.append(record);
		}
		writer.close();
		osw.close();
		out.flush();
	}
}
