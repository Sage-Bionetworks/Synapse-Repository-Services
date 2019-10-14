package org.sagebionetworks.table.cluster;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.util.csv.CSVWriterStream;

public class InMemoryCSVWriterStream implements CSVWriterStream {
	
	List<String[]> rows;
	
	public InMemoryCSVWriterStream() {
		rows = new LinkedList<String[]>();
	}
	
	@Override
	public void writeNext(String[] nextLine) {
		rows.add(nextLine);
	}

	public List<String[]> getRows() {
		return rows;
	}
}
