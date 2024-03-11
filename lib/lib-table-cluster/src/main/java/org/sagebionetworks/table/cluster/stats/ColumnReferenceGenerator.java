package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.model.ColumnReference;

public class ColumnReferenceGenerator implements StatGeneratorInteface<ColumnReference> {

	@Override
	public Optional<ElementStats> generate(ColumnReference element, TableAndColumnMapper tableAndColumnMapper) {
		Optional<ColumnTranslationReference> ctrOptional = tableAndColumnMapper.lookupColumnReference(element);
		
		if (ctrOptional.isEmpty()) {
			return Optional.empty();
		}
		
		ColumnTranslationReference ctr = ctrOptional.get();
		return Optional.of(ElementStats.builder()
                .setMaximumSize(ctr.getMaximumSize())
                .setMaxListLength(ctr.getMaximumListLength())
                .build()); 
	}
}
