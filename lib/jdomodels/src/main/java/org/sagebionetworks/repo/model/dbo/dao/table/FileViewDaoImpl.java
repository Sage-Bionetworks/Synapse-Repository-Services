package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.spring.transaction.StreamingJdbcTemplate;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class FileViewDaoImpl implements FileViewDao {

	/*
	 * The default CRC32 to use for no results.
	 */
	public static long DEFAULT_EMPTY_CRC = 0;
	private static final String IDS_PARAM_NAME = "ids_param";
	private static final String SQL_COUNT_FILES_IN_CONTAINERS = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE+" WHERE "+COL_NODE_TYPE+" = '"+EntityType.file.name()+"' AND "+COL_NODE_PARENT_ID+" IN (:"+IDS_PARAM_NAME+")";
	private static final String SQL_SELECT_FILE_CRC32 = "SELECT SUM(CRC32(CONCAT("+ COL_NODE_ID+", '-',"+ COL_NODE_ETAG+ "))) FROM "+ TABLE_NODE+ " WHERE "+ COL_NODE_TYPE+ " = '"+ EntityType.file+ "' AND "+ COL_NODE_PARENT_ID + " IN (:" + IDS_PARAM_NAME + ")";
	
	@Autowired
	private StreamingJdbcTemplate streamingJdbcTemplate;

	@Override
	public long calculateCRCForAllFilesWithinContainers(Set<Long> viewContainers) {
		ValidateArgument.required(viewContainers, "viewContainers");
		if (viewContainers.isEmpty()) {
			// default
			return DEFAULT_EMPTY_CRC;
		}
		Map<String, Set<Long>> parameters = new HashMap<String, Set<Long>>(1);
		parameters.put(IDS_PARAM_NAME, viewContainers);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(streamingJdbcTemplate);
		Long result = namedParameterJdbcTemplate.queryForObject(
				SQL_SELECT_FILE_CRC32, parameters, Long.class);
		if (result == null) {
			// default
			return DEFAULT_EMPTY_CRC;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.dao.table.FileViewDao#
	 * streamOverFileEntities(java.util.Set, java.util.List,
	 * org.sagebionetworks.repo.model.dao.table.RowHandler)
	 */
	@Override
	public void streamOverFileEntities(Set<Long> containers,
			final List<ColumnModel> schema, final RowHandler rowHandler) {
		// Determine which columns are primary fields an which are annotations
		final List<ColumnModel> annotationColumns = FileViewUtils
				.getNonFileEntityFieldColumns(schema);
		Map<String, Set<Long>> parameters = new HashMap<String, Set<Long>>(1);
		parameters.put(IDS_PARAM_NAME, containers);
		String query = FileViewUtils.createSQLForSchema(schema);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(streamingJdbcTemplate);
		namedParameterJdbcTemplate.query(query, parameters,
				new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						// Map to a Row
						Row row = FileViewUtils.extractRow(schema,
								annotationColumns, rs);
						rowHandler.nextRow(row);
					}
				});
	}

	@Override
	public long countAllFilesInView(Set<Long> allContainersInScope) {
		Map<String, Set<Long>> parameters = new HashMap<String, Set<Long>>(1);
		parameters.put(IDS_PARAM_NAME, allContainersInScope);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(streamingJdbcTemplate);
		return namedParameterJdbcTemplate.queryForObject(SQL_COUNT_FILES_IN_CONTAINERS, parameters, Long.class);
	}

}
