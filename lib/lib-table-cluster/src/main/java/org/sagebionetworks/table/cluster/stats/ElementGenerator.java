package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.SQLElement;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;

public class ElementGenerator implements StatGenerator<SQLElement> {

	@Override
	public Optional<ElementsStats> generate(SQLElement element, TableAndColumnMapper tableAndColumnMapper) {		
		if (element == null) {
			return Optional.empty();
		}
		
		if (element instanceof ColumnReference) {
			return new ColumnReferenceGenerator().generate((ColumnReference) element, tableAndColumnMapper);
		}
		
		if (element instanceof UnsignedValueSpecification) {
			return new UnsignedValueSpecificationGenerator().generate((UnsignedValueSpecification) element, tableAndColumnMapper);
		}
		
		if (element instanceof MySqlFunction) {
			return new MySqlFunctionGenerator().generate((MySqlFunction) element, tableAndColumnMapper);
		}
		
		ElementsStats elementsStats = ElementsStats.builder().build();
		for (Element child : element.getChildren()) {
			Optional<ElementsStats> childStats = generate((SQLElement) child, tableAndColumnMapper);
			if (!childStats.isEmpty()) {
				elementsStats = ElementsStats.generateSumStats(elementsStats, childStats.get());
			}	
		}
		
		return Optional.of(elementsStats);
	}

}
