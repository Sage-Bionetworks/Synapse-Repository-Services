package org.sagebionetworks.repo.manager.athena;

import com.amazonaws.services.athena.model.Row;

public interface RowMapper<T> {
	
	T mapRow(Row row);

}
