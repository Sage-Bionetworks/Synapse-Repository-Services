package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;

public class SetFunctionSpecificationGenerator implements StatGeneratorInteface<SetFunctionSpecification> {

	@Override
	public Optional<ElementStats> generate(SetFunctionSpecification element, TableAndColumnMapper tableAndColumnMapper) {
		switch (element.getSetFunctionType().getFunctionReturnType()) {
			case LONG:
				return Optional.of(ElementStats.builder()
						.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING))
						.build()); 
		
			case DOUBLE:
				return Optional.of(ElementStats.builder()
						.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING))
						.build()); 
				
			default:
				return Optional.empty();
		}
	}
	
}
