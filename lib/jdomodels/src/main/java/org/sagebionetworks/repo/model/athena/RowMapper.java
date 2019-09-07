package org.sagebionetworks.repo.model.athena;

import com.amazonaws.services.athena.model.Row;

/**
 * Mapper for an Athena {@link Row} to perform translation while querying
 * 
 * @author maras
 *
 * @param <T> The result type
 */
@FunctionalInterface
public interface RowMapper<T> {

	/**
	 * Maps the given row to the type T
	 * 
	 * @param row The input row
	 * @return The mapped row to T
	 */
	T mapRow(Row row);

}
