package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.ColumnReference;

public class ColumnReferenceGenerator implements StatGenerator<ColumnReference> {

	@Override
	public Optional<ElementsStats> generate(ColumnReference element, TableAndColumnMapper tableAndColumnMapper) {
		return tableAndColumnMapper.lookupColumnReference(element)
				.map(columnTranslationReference -> ElementsStats.builder()
					.setMaximumSize(columnTranslationReference.getMaximumSize())
					.setMaxListLength(columnTranslationReference.getMaximumListLength())
					.setDefaultValue(columnTranslationReference.getDefaultValues())
					.setFacetType(columnTranslationReference.getFacetType())
					.setJsonSubColumns(columnTranslationReference.getJsonSubColumns())
					.build());
	}
}
