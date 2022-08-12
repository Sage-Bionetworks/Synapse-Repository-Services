package org.sagebionetworks.table.cluster.search;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class TableRowSearchProcessorImpl implements TableRowSearchProcessor {
	
	private static final String SEPARATOR = " ";
	
	@Override
	public String process(TableRowData rowData, boolean includeRowId) {
		ValidateArgument.required(rowData, "rowData");
		
		StringBuilder output = new StringBuilder();
		
		if (includeRowId) {
			// We treat the id as an entity id so that the syn prefix variant is also included
			appendValue(output, ColumnType.ENTITYID, rowData.getRowId().toString());
		}
		
		rowData.getRowValues().forEach( rawValue -> appendValue(output, rawValue.getColumnType(), rawValue.getRawValue()));
		
		return StringUtils.trimToNull(output.toString());
	}
	
	private static void appendValue(StringBuilder builder, ColumnType type, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		
		if (ColumnTypeListMappings.isList(type)) {
			ColumnType nonListType = ColumnTypeListMappings.nonListType(type);
			new JSONArray(value).toList().stream()
				.map(obj -> obj == null ? null : obj.toString())
				.filter(str -> !StringUtils.isBlank(str))
				.forEach((singleValue) -> appendValue(builder, nonListType, singleValue));
			return;
		}
		
		if (ColumnType.ENTITYID == type) {
			Long id = KeyFactory.stringToKey(value.trim());
			builder.append(id).append(SEPARATOR);
			value = KeyFactory.keyToString(id);
		}
		
		builder.append(value.trim()).append(SEPARATOR);
	}
	
}
