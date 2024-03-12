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
	
	/**
	 * For the given element, attempt to estimate the stats for that element, if we are able to estimate the stats return them as an optional.
	 * If the element is of an unhandled case, return Optional.empty()
	 * 
	 * @param element
	 * @param tableAndColumnMapper
	 * @return
	 */
	Optional<ElementStats> generate(T element, TableAndColumnMapper tableAndColumnMapper);
}
