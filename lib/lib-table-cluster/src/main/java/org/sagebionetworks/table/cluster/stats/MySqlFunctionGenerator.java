package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.ValueExpression;

public class MySqlFunctionGenerator implements StatGeneratorInteface<MySqlFunction> {

	@Override
	public Optional<ElementStats> generate(MySqlFunction element, TableAndColumnMapper tableAndColumnMapper) {
		ElementStats elementsStats = ElementStats.builder().build();
		
		switch (element.getFunctionName()) {
			case CONCAT:
				for (ValueExpression valueExpression : element.getParameterValues()) {
					Optional<ElementStats> childStats = new StatGenerator().generate(valueExpression, tableAndColumnMapper);
					if (childStats.isEmpty()) {
						return Optional.empty();
					}
					
					elementsStats = ElementStats.generateSumStats(elementsStats, childStats.get());
				}
				
				return Optional.of(elementsStats);
				
			default:
				return Optional.empty();
		}
	}

}
