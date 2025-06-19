package org.sagebionetworks.table.cluster.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.sagebionetworks.avro.pfb.model.Entity;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;

public class RowPFBWriter implements RowHandler {

	private final String tableName;
	private final DataFileWriter<Entity> writer;
	private final List<ColumnModel> columns;
	private final Schema objectSchema;
	private final Schema entitySchema;

	public RowPFBWriter(String tableName, List<ColumnModel> columns, Metadata metadata, OutputStream out) throws IOException {
		this.tableName = tableName;
		this.columns = columns;
		this.objectSchema = ColumnTypeAvro.toAvro(tableName, columns);
		// Expand the Entity schema to include an Object that matches the columns.
		entitySchema = Entity.createEntitySchema(List.of(objectSchema));

		writer = new DataFileWriter<>(new SpecificDatumWriter<>(entitySchema));
		writer.create(entitySchema, out);
		// the first row must be metadata
		writer.append(new Entity(entitySchema).setId(null).setName("Metadata")
				.setObject(metadata));
	}

	@Override
	public void nextRow(Row row) {
		try {
			writer.append(new Entity(entitySchema).setId(RowPFBUtils.createEntiyId(row)).setName(tableName)
					.setObject(RowPFBUtils.createObject(objectSchema, columns, row)));
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
