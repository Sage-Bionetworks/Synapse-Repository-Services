package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.SQLElement;

@FunctionalInterface
public interface StatGeneratorInteface<T extends Element> {
	Optional<ElementStats> generate(T element, TableAndColumnMapper tableAndColumnMapper);
}
