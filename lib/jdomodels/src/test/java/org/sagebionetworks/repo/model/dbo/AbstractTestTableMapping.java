package org.sagebionetworks.repo.model.dbo;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Helper for making test TableMapping classes
 * 
 * @author jmhill
 *
 */
public abstract class AbstractTestTableMapping<T>  implements TableMapping<T>{

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return null;
	}

	@Override
	public String getDDLFileName() {
		return null;
	}

	@Override
	public Class<? extends T> getDBOClass() {
		return null;
	}

}
