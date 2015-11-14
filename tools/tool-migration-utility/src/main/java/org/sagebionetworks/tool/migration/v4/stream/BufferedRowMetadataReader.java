package org.sagebionetworks.tool.migration.v4.stream;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.sagebionetworks.repo.model.migration.RowMetadata;
import static org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter.DELIMITER;
import static org.sagebionetworks.tool.migration.v3.stream.BufferedRowMetadataWriter.NULL;

public class BufferedRowMetadataReader implements Iterator<RowMetadata>, Closeable{
	
	BufferedReader reader;
	private boolean done = false;
	String lastLine;

	/**
	 * Buffered Reader
	 * @param reader
	 */
	public BufferedRowMetadataReader(Reader reader){
		this.reader = new BufferedReader(reader);
		try {
			lastLine = this.reader.readLine();
		} catch (IOException e) {
			lastLine = null;
		}
	}

	@Override
	public boolean hasNext() {
		return lastLine != null;
	}

	@Override
	public RowMetadata next() {
		if(lastLine == null) return null;
		// read a line
		try {
			if(lastLine == null){
				done = true;
				return null;
			}
			// Split the line by the delimiter
			String[] split = lastLine.split(DELIMITER);
			if(split.length != 2) throw new IllegalStateException("Expected 2 parts to a line: "+lastLine);
			// first should be the id, then the parent id.
			RowMetadata results = new RowMetadata();
			results.setId(getLong(split[0]));
			results.setParentId(getLong(split[1]));
			// Get the next line
			lastLine = reader.readLine();
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

	@Override
	public void close() throws IOException {
		this.reader.close();
	}

}
