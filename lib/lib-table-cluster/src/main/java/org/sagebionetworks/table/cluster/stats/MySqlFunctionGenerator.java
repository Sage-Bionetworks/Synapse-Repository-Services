package org.sagebionetworks.table.cluster.stats;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.ValueExpression;

public class MySqlFunctionGenerator implements StatGeneratorInteface<MySqlFunction> {

	@Override
	public Optional<ElementStats> generate(MySqlFunction element, TableAndColumnMapper tableAndColumnMapper) {
		StatGenerator generator = new StatGenerator(); 
		
		switch (element.getFunctionName()) {
			case CONCAT:
				List<ValueExpression> children = element.getParameterValues();
				
				if (children.size() == 0) {
					return Optional.empty();
				}
				
				ElementStats elementStats = ElementStats.builder().build();
				
				for (ValueExpression valueExpression : children) {
					Optional<ElementStats> childStats = generator.generate(valueExpression, tableAndColumnMapper);
					
					if (childStats.isEmpty()) {
						return Optional.empty();
					}
					
					elementStats = elementStats.cloneBuilder()
							.setMaximumSize(ElementStats.addLongsWithNull(elementStats.getMaximumSize(), childStats.get().getMaximumSize()))
							.build();
				}
				
				return Optional.of(elementStats);
				
			default:
				return Optional.empty();
		}
	}

}
