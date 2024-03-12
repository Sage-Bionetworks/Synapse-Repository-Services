package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.NumericValueExpression;

public class NumericValueExpressionGenerator implements StatGeneratorInteface<NumericValueExpression> {

	@Override
	public Optional<ElementStats> generate(NumericValueExpression element, TableAndColumnMapper tableAndColumnMapper) {
		if (element.getPrimeList().size() == 0) {
			return new StatGenerator().generate(element.getTerm(), tableAndColumnMapper);
		}
		
		return Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING))
				.build());
	}

}
