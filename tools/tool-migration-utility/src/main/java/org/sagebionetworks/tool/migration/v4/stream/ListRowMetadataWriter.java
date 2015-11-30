package org.sagebionetworks.tool.migration.v4.stream;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.migration.RowMetadata;

/**
 * List backed  Writer<RowMetadata>
 * 
 * @author jmhill
 *
 */
public class ListRowMetadataWriter implements RowWriter<RowMetadata> {

	List<RowMetadata> list = new LinkedList<RowMetadata>();
	
	@Override
	public void write(RowMetadata toWrite) {
		list.add(toWrite);
	}

	/**
	 * Get the list of data produced by this writer.
	 * @return
	 */
	public List<RowMetadata> getList() {
		return list;
	}

}
