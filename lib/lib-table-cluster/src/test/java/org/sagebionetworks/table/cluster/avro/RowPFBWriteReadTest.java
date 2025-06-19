package org.sagebionetworks.table.cluster.avro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.file.SeekableByteArrayInput;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.avro.pfb.model.Node;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;

public class RowPFBWriteReadTest {

	@Test
	public void testAllTypesWriteAndRead() throws IOException {
		boolean hasDefault = false;
		String tableName = "foo";
		List<ColumnModel> columns = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(columns, 10,
				new TableModelTestUtils.ValueOptions().includeSpace(false));
		List<Row> result = writeAndRead(tableName, columns, createMetadata(), rows);
		assertEquals(rows, result);
	}

	@Test
	public void testAllTypesWriteAndReadWithIdsAndVersion() throws IOException {
		boolean hasDefault = false;
		String tableName = "foo";
		List<ColumnModel> columns = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(columns, 10,
				new TableModelTestUtils.ValueOptions().includeSpace(false));
		// set row ids and version
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			row.setRowId(Long.valueOf(i));
			long version = i % 2 > 0 ? 1 : 0;
			row.setVersionNumber(version);
		}

		// call under test
		List<Row> result = writeAndRead(tableName, columns, createMetadata(), rows);
		assertEquals(rows, result);
	}

	public static Metadata createMetadata() {
		return new Metadata().setNodes(List.of(new Node().setName("blank")));
	}

	/**
	 * Write the provided data to PFB, then read back the results.
	 * 
	 * @param tableName
	 * @param columns
	 * @param rows
	 * @return
	 * @throws IOException
	 */
	static List<Row> writeAndRead(String tableName, List<ColumnModel> columns, Metadata metadata, List<Row> rows)
			throws IOException {
		// write
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (RowPFBWriter writer = new RowPFBWriter(tableName, columns, metadata, out)) {
			rows.forEach(r -> {
				writer.nextRow(r);
			});
		}

		// Read
		List<Row> result = new ArrayList<>();
		Metadata readMetadata = null;
		try (RowPFBReader reader = new RowPFBReader(new SeekableByteArrayInput(out.toByteArray()))) {
			readMetadata = reader.getMetadata();
			while (reader.hasNext()) {
				result.add(reader.next());
			}
		}
		assertEquals(readMetadata, metadata);
		return result;
	}
}
