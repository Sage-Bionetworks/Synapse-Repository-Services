package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class TableIndexDAOImpl implements TableIndexDAO{

	@Override
	public boolean createOrUpdateTable(SimpleJdbcTemplate connection, List<ColumnModel> schema, String tableId) {
		if(connection == null) throw new IllegalArgumentException("Connection cannot be null");
		if(schema == null) throw new IllegalArgumentException("New schema cannot be null");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteTable(SimpleJdbcTemplate connection, String tableId) {
		// TODO Auto-generated method stub
		return false;
	}

}
