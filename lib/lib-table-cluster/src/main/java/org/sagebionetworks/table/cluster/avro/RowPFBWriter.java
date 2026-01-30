package org.sagebionetworks.table.cluster.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.sagebionetworks.avro.pfb.model.Entity;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;

public class RowPFBWriter implements RowHandler {
	
	static int[] getColumnIdIndexRef(List<ColumnModel> columns, List<String> columnNames) {
		if (columnNames == null || columnNames.isEmpty()) {
			return null;
		}
		
		Map<String, Integer> nameToIndex = IntStream.range(0, columns.size()).boxed()
			.collect(Collectors.toMap(i -> columns.get(i).getName(), i -> i));
		
		int[] indexRef = new int[columnNames.size()];
		
		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			
			Integer index = nameToIndex.get(columnName);
			
			if (index == null) {
				throw new IllegalArgumentException("Could not find column `" + columnName + "` in the select list.");
			}
			
			indexRef[i] = index;
		}

		return indexRef;
		
	}
	
	private final String tableName;
	private final DataFileWriter<Entity> writer;
	private final List<ColumnModel> columns;
	private final Schema objectSchema;
	private final Schema entitySchema;
	private final int[] idColumnIndexRef;

	public RowPFBWriter(String tableName, List<ColumnModel> columns, List<String> idColumnNames, Metadata metadata, OutputStream out) throws IOException {
		this.tableName = tableName;
		this.idColumnIndexRef = getColumnIdIndexRef(columns, idColumnNames);
		this.columns = columns;
		this.objectSchema = ColumnTypeAvro.toAvro(tableName, columns);
		// Expand the Entity schema to include an Object that matches the columns.
		this.entitySchema = Entity.createEntitySchema(List.of(objectSchema));

		this.writer = new DataFileWriter<>(new SpecificDatumWriter<>(entitySchema));
		
		this.writer.create(entitySchema, out);
		// the first row must be metadata
		this.writer.append(new Entity(entitySchema)
			.setId(null)
			.setName("Metadata")
			.setObject(metadata)
		);
	}

	@Override
	public void nextRow(Row row) {
		try {
			
			String entityId;
			List<String> rowValues = row.getValues();
			
			if (idColumnIndexRef != null) {
				entityId = RowPFBUtils.createEntityIdFromColumns(rowValues, idColumnIndexRef);
			} else {
				entityId = RowPFBUtils.createEntityIdFromRowId(row);
			}
			
			writer.append(new Entity(entitySchema)
				.setId(entityId)
				.setName(tableName)
				.setObject(RowPFBUtils.createObject(objectSchema, columns, rowValues))
			);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
	}

}
