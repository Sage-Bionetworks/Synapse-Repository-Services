package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.SQLElement;

@FunctionalInterface
public interface StatGenerator<T extends SQLElement> {
	Optional<ElementsStats> generate(T element, TableAndColumnMapper tableAndColumnMapper);
}
