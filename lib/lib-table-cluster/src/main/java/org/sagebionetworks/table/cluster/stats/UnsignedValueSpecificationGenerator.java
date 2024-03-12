package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;

public class UnsignedValueSpecificationGenerator implements StatGeneratorInteface<UnsignedValueSpecification> {

	@Override
	public Optional<ElementStats> generate(UnsignedValueSpecification element, TableAndColumnMapper tableAndColumnMapper) {
		return Optional.of(ElementStats.builder()
					.setMaximumSize(Long.valueOf(element.toSqlWithoutQuotes().length()))
					.build());
	}

}
