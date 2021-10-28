package org.sagebionetworks.table.cluster.search;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class SimpleRowSearchProcessor implements RowSearchProcessor {
	
	private static final String SEPARATOR = " ";

	@Override
	public String process(List<TypedCellValue> data) {
		ValidateArgument.required(data, "data");
		
		StringBuilder output = new StringBuilder();
		
		for (TypedCellValue rawValue : data) {
			String value = rawValue.getRawValue();
			
			if (StringUtils.isBlank(value)) {
				continue;
			}
			
			if (ColumnTypeListMappings.isList(rawValue.getColumnType())) {
				List<String> multiValues = new JSONArray(value).toList().stream()
						.map(obj -> obj == null ? null : obj.toString().trim())
						.filter(str -> !StringUtils.isBlank(str))
						.collect(Collectors.toList());
				output.append(String.join(SEPARATOR, multiValues)).append(SEPARATOR);
			} else {
				output.append(value.trim()).append(SEPARATOR);
			}
			
		}
		
		return StringUtils.trimToNull(output.toString());
	}

}
