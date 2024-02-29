package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.ValueExpression;

public class MySqlFunctionGenerator implements StatGenerator<MySqlFunction> {

	@Override
	public Optional<ElementsStats> generate(MySqlFunction element, TableAndColumnMapper tableAndColumnMapper) {
		ElementsStats elementsStats = ElementsStats.builder().build();
		
		switch (element.getFunctionName()) {
			case CONCAT:
				for (ValueExpression valueExpression : element.getParameterValues()) {
					Optional<ElementsStats> childStats = new ElementGenerator().generate(valueExpression, tableAndColumnMapper);
					if (childStats.isEmpty()) {
						return Optional.empty();
					}
					
					elementsStats = ElementsStats.generateSumStats(elementsStats, childStats.get());
				}
				
				return Optional.ofNullable(elementsStats);
				
			default:
				return Optional.empty();
		}
	}

}
