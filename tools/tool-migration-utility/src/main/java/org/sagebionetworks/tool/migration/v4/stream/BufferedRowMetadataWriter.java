package org.sagebionetworks.tool.migration.v4.stream;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

import org.sagebionetworks.repo.model.migration.RowMetadata;

/**
 * BufferedWriter backed implementation of RowWriter<RowMetadata>
 * @author jmhill
 *
 */
public class BufferedRowMetadataWriter implements RowWriter<RowMetadata>, Closeable, Flushable {
	
	public static String DELIMITER = " ";
	public static String NULL = "null"; 
	
	BufferedWriter writer;

	public BufferedRowMetadataWriter(Writer out) {
		super();
		this.writer = new BufferedWriter(out);
	}

	@Override
	public void write(RowMetadata toWrite) {
		if(toWrite == null) throw new IllegalArgumentException("toWrite Cannot be null");
		try {
			// Write the id, parentId
			writeLong(toWrite.getId());
			writer.write(DELIMITER);
			writeLong(toWrite.getParentId());
			writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeLong(Long value) throws IOException{
		// Write the id, parentId
		if(value != null){
			writer.write(value.toString());
		}else{
			writer.write(NULL);
		}
	}

	@Override
	public void flush() throws IOException {
		this.writer.flush();
	}

	@Override
	public void close() throws IOException {
		this.writer.close();
	}

}
