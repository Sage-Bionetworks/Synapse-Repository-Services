package org.sagebionetworks.util.csv;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.sagebionetworks.util.ValidateArgument;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Iterator over a CSVReader.
 *
 */
public class CSVReaderIterator implements Iterator<String[]>, Closeable {

	CSVReader reader;
	String[] lastRow;
	
	public CSVReaderIterator(CSVReader reader) {
		ValidateArgument.required(reader, "reader");
		this.reader = reader;
	}
	
	@Override
	public boolean hasNext() {
		try {
			 lastRow = reader.readNext();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return lastRow != null;
	}

	@Override
	public String[] next() {
		return lastRow;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

}
