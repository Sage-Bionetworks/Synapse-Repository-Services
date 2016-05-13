package org.sagebionetworks.util.csv;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * A basic implementation CSVWriterStream that is a proxy for an actual CSVWriter;
 * @author John
 *
 */
public class CSVWriterStreamProxy implements CSVWriterStream {

	CSVWriter writer;
	
	public CSVWriterStreamProxy(CSVWriter writer) {
		if(writer == null){
			throw new IllegalArgumentException("Writer cannot be null");
		}
		this.writer = writer;
	}

	@Override
	public void writeNext(String[] nextLine) {
		writer.writeNext(nextLine);
	}

}
