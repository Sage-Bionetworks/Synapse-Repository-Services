package org.sagebionetworks.tool.migration.v3.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

import org.sagebionetworks.repo.model.migration.RowMetadata;
import static org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter.DELIMITER;
import static org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter.NULL;

public class BufferedRowMetadataReader implements Iterator<RowMetadata>{
	
	BufferedReader reader;
	private boolean done = false;

	@Override
	public boolean hasNext() {
		return !done;
	}

	@Override
	public RowMetadata next() {
		if(done) return null;
		// read a line
		try {
			String line = reader.readLine();
			if(line == null){
				done = true;
				return null;
			}
			// Split the line by the delimiter
			String[] split = line.split(DELIMITER);
			if(split.length != 2) throw new IllegalStateException("Expected 2 parts to a line: "+line);
			// first should be the id, then the parent id.
			RowMetadata results = new RowMetadata();
			results.setId(getLong(split[0]));
			results.setParentId(getLong(split[1]));
			return results;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Long getLong(String toTry){
		if(NULL.equals(toTry)) return null;
		return Long.parseLong(toTry);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported");
	}

}
