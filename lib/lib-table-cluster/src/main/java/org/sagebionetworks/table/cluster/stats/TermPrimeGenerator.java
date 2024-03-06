package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.TermPrime;

public class TermPrimeGenerator implements StatGeneratorInteface<TermPrime> {

	@Override
	public Optional<ElementStats> generate(TermPrime element, TableAndColumnMapper tableAndColumnMapper) {
		switch (element.getOperator()) {
			case PLUS_SIGN:
				return new StatGenerator().generate(element.getTerm(), tableAndColumnMapper);
				
			default:
				return Optional.empty();
		}
	}
	
}
