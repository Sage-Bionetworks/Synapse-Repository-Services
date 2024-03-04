package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;

public class SetFunctionSpecificationGenerator implements StatGeneratorInteface<SetFunctionSpecification> {

	@Override
	public Optional<ElementStats> generate(SetFunctionSpecification element, TableAndColumnMapper tableAndColumnMapper) {
		switch (element.getSetFunctionType()) {
			case COUNT:
				
			default:
				return Optional.empty();
		}
	}

}
