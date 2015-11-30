package org.sagebionetworks.tool.migration.v4.stream;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.migration.RowMetadata;

/**
 * Test for both BufferedRowMetadataWriter and BufferedRowMetadataReader
 * @author jmhill
 *
 */
public class BufferedRowMetadataWriterReaderTest {
	
	
	@Before
	public void before(){

	}

	@Test
	public void testRoundTrip() throws IOException{
		List<RowMetadata> input = buildList(new Long[][]{
				new Long[]{4l,null},
				new Long[]{5l,null},
				new Long[]{25l,30l},
				new Long[]{null,4l},
		});
		StringWriter stringWriter = new StringWriter();
		BufferedRowMetadataWriter writer = new BufferedRowMetadataWriter(stringWriter);
		// Create a writer, then read it from a reader
		for(RowMetadata row: input){
			writer.write(row);
		}
		writer.close();
		String output = stringWriter.toString();
		System.out.println(output);
		// Now create a reader
		StringReader stringReader = new StringReader(output);
		BufferedRowMetadataReader reader = new BufferedRowMetadataReader(stringReader);
		// Now read all data into a new list
		List<RowMetadata> results = new LinkedList<RowMetadata>();
		while(reader.hasNext()){
			RowMetadata row = reader.next();
			results.add(row);
		}
		// The result should match the original input list
		assertEquals(input, results);
	}
	
	/**
	 * Buildup a list from an simple array. The first long is the id the second long
	 * is the parent id.
	 * @param data
	 * @return
	 */
	List<RowMetadata> buildList(Long[][] data){
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0; i<data.length; i++){
			RowMetadata row = new RowMetadata();
			row.setId(data[i][0]);
			row.setParentId(data[i][1]);
			list.add(row);
		}
		return list;
	}
}
