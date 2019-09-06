package org.sagebionetworks.repo.model.athena;

import com.amazonaws.services.athena.model.Row;

public interface RowMapper<T> {
	
	T mapRow(Row row);

}
