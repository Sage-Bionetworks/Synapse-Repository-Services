package org.sagebionetworks.table.cluster.avro;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.repo.model.table.ColumnModel;

@FunctionalInterface
public interface RowPFBWriterProvider {

	/**
	 * Create a new {@link RowPFBWriter}.
	 * @param tableName
	 * @param columns
	 * @param file
	 * @return
	 * @throws IOException
	 */
	RowPFBWriter createWriter(String tableName, List<ColumnModel> columns, Metadata metadata, File file) throws IOException;
}
