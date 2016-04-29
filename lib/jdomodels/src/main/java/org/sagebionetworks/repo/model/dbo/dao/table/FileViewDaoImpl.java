package org.sagebionetworks.repo.model.dbo.dao.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class FileViewDaoImpl implements FileViewDao {
	
	private static final String IDS_PARAM_NAME = "ids_param";

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	public void stream(Set<Long> containers, List<ColumnModel> schema, final RowHandler rowHandler){
		Map<String, Set<Long>> parameters = new HashMap<String, Set<Long>>(1);
		parameters.put(IDS_PARAM_NAME, containers);
		String query = FileViewUtils.createSQLForSchema(schema);
		namedParameterJdbcTemplate.query(query, parameters, new RowCallbackHandler() {
			
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// TODO Auto-generated method stub
				
			}
		});
	}
}
