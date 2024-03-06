package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.ValueExpression;

public class SetFunctionSpecificationGenerator implements StatGeneratorInteface<SetFunctionSpecification> {

	@Override
	public Optional<ElementStats> generate(SetFunctionSpecification element, TableAndColumnMapper tableAndColumnMapper) {
		switch (element.getSetFunctionType().getFunctionReturnType()) {
			case LONG:
				return buildStats(ColumnConstants.MAX_INTEGER_BYTES_AS_STRING);
		
			case DOUBLE:
				return buildStats(ColumnConstants.MAX_DOUBLE_BYTES_AS_STRING);
				
			default:
				return Optional.empty();
		}
	}
	
	static Optional<ElementStats> buildStats(int maxIntegerBytesAsString) {
		return Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(maxIntegerBytesAsString))
				.build()); 
	}

}
