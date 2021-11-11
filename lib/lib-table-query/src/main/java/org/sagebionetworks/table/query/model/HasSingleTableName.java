package org.sagebionetworks.table.query.model;

import java.util.Optional;

public interface HasSingleTableName {

	/**
	 * If this SQL element has one and only one table name, then return that table
	 * name. This method should return Optional.emtpy if the SQL includes more than
	 * one table name due to a the inclusion of a join.
	 * 
	 * @return
	 */
	Optional<String> getSingleTableName();

}
