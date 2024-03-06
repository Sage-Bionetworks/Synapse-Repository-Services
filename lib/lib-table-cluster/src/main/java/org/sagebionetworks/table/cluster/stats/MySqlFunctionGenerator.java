package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.ValueExpression;

public class MySqlFunctionGenerator implements StatGeneratorInteface<MySqlFunction> {

	@Override
	public Optional<ElementStats> generate(MySqlFunction element, TableAndColumnMapper tableAndColumnMapper) {
		ElementStats elementStats = ElementStats.builder().build();
		
		switch (element.getFunctionName()) {
			case CONCAT:
				for (ValueExpression valueExpression : element.getParameterValues()) {
					Optional<ElementStats> childStats = new StatGenerator().generate(valueExpression, tableAndColumnMapper);
					
					if (childStats.isEmpty()) {
						return Optional.empty();
					}
					
					elementStats = ElementStats.generateSumStats(elementStats, childStats.get());
				}
				
				return elementStats.getMaximumSize() > 0 ? Optional.of(elementStats) : Optional.empty();
				
			default:
				return Optional.empty();
		}
	}

}
