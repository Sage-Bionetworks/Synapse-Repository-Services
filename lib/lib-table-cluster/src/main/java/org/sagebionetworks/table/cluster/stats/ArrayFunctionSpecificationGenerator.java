package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;

public class ArrayFunctionSpecificationGenerator implements StatGeneratorInteface<ArrayFunctionSpecification> {

	@Override
	public Optional<ElementStats> generate(ArrayFunctionSpecification element, TableAndColumnMapper tableAndColumnMapper) {
		switch (element.getListFunctionType()) {
			case UNNEST: 
				Optional<ColumnTranslationReference> ctrOptional = 
						tableAndColumnMapper.lookupColumnReference(element.getColumnReference());
				
				if (ctrOptional.isPresent()) {
					return Optional.of(ElementStats.builder()
			                .setMaximumSize(ctrOptional.get().getMaximumSize())
			                .build());
				} else {
					return Optional.empty();
				}
				
			default:
				return Optional.empty();
		}
	}

}
