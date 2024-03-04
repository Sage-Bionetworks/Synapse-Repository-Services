package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.TermPrime;

public class NumericValueExpressionGenerator implements StatGeneratorInteface<NumericValueExpression> {

	@Override
	public Optional<ElementStats> generate(NumericValueExpression element, TableAndColumnMapper tableAndColumnMapper) {
		NumericValueExpression numericValueExpression = (NumericValueExpression) element;
		StatGenerator generator = new StatGenerator();
		
		Optional<ElementStats> termStats = generator.generate(numericValueExpression.getTerm(), tableAndColumnMapper);
		
		ElementStats termPrimeStats = ElementStats.builder().build();
		for (TermPrime termPrime : numericValueExpression.getPrimeList()) {
			Optional<ElementStats> child = generator.generate(termPrime, tableAndColumnMapper);
			if (!child.isEmpty()) {
				termPrimeStats = ElementStats.generateSumStats(termPrimeStats, child.get());
			}
		}
		
		if (termStats.isEmpty()) {
			return Optional.of(termPrimeStats);
		}
		
		return Optional.of(ElementStats.generateSumStats(termStats.get(), termPrimeStats));
	}

}
