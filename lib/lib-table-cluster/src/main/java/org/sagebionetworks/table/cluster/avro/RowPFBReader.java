package org.sagebionetworks.table.cluster.avro;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.sagebionetworks.avro.pfb.model.Entity;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.repo.model.table.ColumnType;

public class RowPFBReader implements Iterator<RowPFBReader.PFBRow>, Closeable {

	private final Schema entitySchema;
	private final GenericDatumReader<GenericRecord> entityReader;
	private final DataFileReader<GenericRecord> dataFileReader;
	private final Metadata metadata;

	public RowPFBReader(SeekableInput in) throws IOException {
		this.entityReader = new GenericDatumReader<GenericRecord>();
		this.dataFileReader = new DataFileReader<>(in, this.entityReader);
		this.entitySchema = this.dataFileReader.getSchema();
		this.metadata = readMetadata();
	}
	
	private Metadata readMetadata() {
		if(dataFileReader.hasNext()) {
			Entity entity = new Entity(this.entitySchema, dataFileReader.next());
			if(entity.getObject() instanceof Metadata) {
				return (Metadata) entity.getObject();
			}
		}
		throw new IllegalArgumentException("The first row of a PFB must be 'Metadata'");
	}
	
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public boolean hasNext() {
		return dataFileReader.hasNext();
	}

	@Override
	public PFBRow next() {
		Entity entity = new Entity(this.entitySchema, dataFileReader.next());
		
		IndexedRecord object = entity.getObject();
		
		List<Field> fields = object.getSchema().getFields();
		List<String> values = new ArrayList<>(fields.size());
		
		fields.forEach(f -> {
			Schema typeSchema = f.schema().getTypes().get(1);
			ColumnType columnType = ColumnTypeAvro.getColumnType(typeSchema);
			String value = ColumnTypeAvro.matchType(columnType).avroToRow(object.get(f.pos()));
			values.add(value);
		});

		return new PFBRow(entity.getId(), values);
	}

	@Override
	public void close() throws IOException {
		if (dataFileReader != null) {
			dataFileReader.close();
		}
	}
	
	public static final class PFBRow {
		
		private final String entityId;
		private final List<String> values;
		
		public PFBRow(String entityId, List<String> values) {
			this.entityId = entityId;
			this.values = values;
		}
		
		public String getEntityId() {
			return entityId;
		}
		
		public List<String> getValues() {
			return values;
		}

		@Override
		public int hashCode() {
			return Objects.hash(entityId, values);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			PFBRow other = (PFBRow) obj;
			return Objects.equals(entityId, other.entityId) && Objects.equals(values, other.values);
		}
		
	}

}
