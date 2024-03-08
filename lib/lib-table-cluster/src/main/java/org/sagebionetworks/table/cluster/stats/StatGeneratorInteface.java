package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.Element;

/**
 * Implementations of the StatGeneratorInterface will be used in combination to recursively calculate stats 
 * to be used as estimations for new columns.  
 * 
 * @param <T>
 */

@FunctionalInterface
public interface StatGeneratorInteface<T extends Element> {
	Optional<ElementStats> generate(T element, TableAndColumnMapper tableAndColumnMapper);
}
