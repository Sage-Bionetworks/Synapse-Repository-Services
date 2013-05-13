package org.sagebionetworks.tool.migration.v3.stream;

import java.io.BufferedWriter;
import java.io.IOException;

import org.sagebionetworks.repo.model.migration.RowMetadata;

/**
 * BufferedWriter backed implementation of Writer<RowMetadata>
 * @author jmhill
 *
 */
public class BufferedRowMetadataWriter implements Writer<RowMetadata> {
	
	public static String DELIMITER = " ";
	public static String NULL = "null"; 
	
	BufferedWriter writer;

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

}
