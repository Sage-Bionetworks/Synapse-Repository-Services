package org.sagebionetworks.table.cluster.search;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class SimpleRowSearchProcessor implements RowSearchProcessor {
	
	private static final String SEPARATOR = " ";

	@Override
	public Optional<String> process(List<ColumnModel> columns, List<String> rowValues) {
		ValidateArgument.required(columns, "columns");
		ValidateArgument.required(rowValues, "rowValues");
		ValidateArgument.requirement(columns.size() == rowValues.size(), "The number of columns and row values must match.");
		
		StringBuilder output = new StringBuilder();
		
		for (int i=0; i < rowValues.size(); i++) {
			String value = rowValues.get(i);
			
			if (StringUtils.isBlank(value)) {
				continue;
			}
			
			ColumnModel model = columns.get(i);
			
			if (ColumnTypeListMappings.isList(model.getColumnType())) {
				List<String> multiValues = new JSONArray(value).toList().stream()
						.map(obj -> obj == null ? null : obj.toString().trim())
						.filter(str -> !StringUtils.isBlank(str))
						.collect(Collectors.toList());
				output.append(String.join(SEPARATOR, multiValues)).append(SEPARATOR);
			} else {
				output.append(value.trim()).append(SEPARATOR);
			}
			
		}
		
		return Optional.ofNullable(StringUtils.trimToNull(output.toString()));
	}

}
