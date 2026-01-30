package org.sagebionetworks.table.cluster.avro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.avro.file.SeekableByteArrayInput;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.avro.pfb.model.Node;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.avro.RowPFBReader.PFBRow;

public class RowPFBWriteReadTest {

	@Test
	public void testAllTypesWriteAndRead() throws IOException {
		boolean hasDefault = false;
		String tableName = "foo";
		List<ColumnModel> columns = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(columns, 10,
				new TableModelTestUtils.ValueOptions().includeSpace(false));
		
		List<String> idColumnNames = null;
		// call under test
		List<PFBRow> result = writeAndRead(tableName, columns, idColumnNames, createMetadata(), rows);
		
		assertEquals(rows.stream().map(r -> new PFBRow(null, r.getValues())).collect(Collectors.toList()), result);
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

		List<String> idColumnNames = null;
		
		// call under test
		List<PFBRow> result = writeAndRead(tableName, columns, idColumnNames, createMetadata(), rows);
		
		assertEquals(rows.stream().map(r -> new PFBRow(RowPFBUtils.createEntityIdFromRowId(r), r.getValues())).collect(Collectors.toList()), result);
	}
	
	@Test
	public void testAllTypesWriteAndReadWithIdColumnName() throws IOException {
		boolean hasDefault = false;
		String tableName = "foo";
		List<ColumnModel> columns = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(columns, 10, new TableModelTestUtils.ValueOptions().includeSpace(false));
		
		// Uses the "INTEGER" and "STRING" columns to compose the id column
		List<String> idColumnNames = List.of(columns.get(2).getName(), columns.get(0).getName());
		// Call under test
		List<PFBRow> result = writeAndRead(tableName, columns, idColumnNames, createMetadata(), rows);
		
		assertEquals(rows.stream().map(r -> new PFBRow(RowPFBUtils.createEntityIdFromColumns(r.getValues(), new int[] {2,0}), r.getValues())).collect(Collectors.toList()), result);
	}
	
	@Test
	public void testAllTypesWriteAndReadWithMismatchingIdColumnName() throws IOException {
		boolean hasDefault = false;
		String tableName = "foo";
		List<ColumnModel> columns = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(columns, 10, new TableModelTestUtils.ValueOptions().includeSpace(false));
		
		List<String> idColumnNames = List.of("invalid");
		
		assertEquals("Could not find column `invalid` in the select list.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			writeAndRead(tableName, columns, idColumnNames, createMetadata(), rows);
		}).getMessage());
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
	static List<PFBRow> writeAndRead(String tableName, List<ColumnModel> columns, List<String> idColumnNames, Metadata metadata, List<Row> rows)
			throws IOException {
		// write
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (RowPFBWriter writer = new RowPFBWriter(tableName, columns, idColumnNames, metadata, out)) {
			rows.forEach(r -> {
				writer.nextRow(r);
			});
		}

		// Read
		List<PFBRow> result = new ArrayList<>();
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
