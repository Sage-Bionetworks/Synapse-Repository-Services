package org.sagebionetworks.table.cluster.avro;

import java.util.List;
import java.util.StringJoiner;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.sagebionetworks.avro.pfb.model.Entity;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.ValidateArgument;

public class RowPFBUtils {
	
	private static String DELMITER= "_";
	private static String ID_VERSION_TEMPLATE = "%d"+DELMITER+"%d";

	/**
	 * Create an {@link Entity} id from the row's id and version.
	 * 
	 * @param row
	 * @return
	 */
	public static String createEntityIdFromRowId(Row row) {
		ValidateArgument.required(row, "row");
		if (row.getRowId() == null) {
			return null;
		}
		return row.getVersionNumber() != null ? String.format(ID_VERSION_TEMPLATE, row.getRowId(), row.getVersionNumber())
				: String.format("%d", row.getRowId());
	}
	
	/**
	 * Create an {@link Entity} id concatenating the values at the given index positions.
	 * 
	 * @param rowValues The row values
	 * @param idColumnIndexRef The index positions of the columns to use for the id
	 * @return
	 */
	public static String createEntityIdFromColumns(List<String> rowValues, int[] idColumnIndexRef) {
		ValidateArgument.required(rowValues, "rowValues");
		ValidateArgument.required(idColumnIndexRef, "idColumnIndexRef");		
		StringJoiner joiner = new StringJoiner(DELMITER);		
		for (int index : idColumnIndexRef) {
			joiner.add(rowValues.get(index));
		}		
		return joiner.toString();
	}
	
	/**
	 * Create a {@link GenericRecord} object to represent the provided row.
	 * 
	 * @param objectSchema The schema that defines the row.
	 * @param columns      The ColumnModel schema that defines the row.
	 * @param values       The row values.
	 * @return
	 */
	public static GenericRecord createObject(Schema objectSchema, List<ColumnModel> columns, List<String> values) {
		GenericRecordBuilder builder = new GenericRecordBuilder(objectSchema);
		objectSchema.getFields().forEach(f -> {
			Object value = ColumnTypeAvro.matchType(columns.get(f.pos()).getColumnType()).rowToAvro(values.get(f.pos()));
			builder.set(f,value);
		});
		return builder.build();
	}

}
