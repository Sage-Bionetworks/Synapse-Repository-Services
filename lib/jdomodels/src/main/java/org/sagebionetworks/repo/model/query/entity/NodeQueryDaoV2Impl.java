package org.sagebionetworks.repo.model.query.entity;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_ENTITY_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_TABLE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class NodeQueryDaoV2Impl implements NodeQueryDaoV2 {
	
	public static final String BIND_ENTITY_IDS = "bEntityids";
	
	private static final String SQL_SELECT_ANNOTATIONS_FOR_ENTITY_IDS = 
			"SELECT "
			+ANNOTATION_REPLICATION_COL_ENTITY_ID
			+", "+ANNOTATION_REPLICATION_COL_KEY
			+", "+ANNOTATION_REPLICATION_COL_STRING_VALUE
			+" FROM "+ANNOTATION_REPLICATION_TABLE
			+" WHERE "
			+ANNOTATION_REPLICATION_COL_ENTITY_ID+" IN (:"+BIND_ENTITY_IDS+")";
			
	
	NamedParameterJdbcTemplate namedTemplate;
	
	/**
	 * Create a new DAO for the given connection.
	 * 
	 * @param connection
	 */
	public NodeQueryDaoV2Impl(DataSource connection){
		namedTemplate = new NamedParameterJdbcTemplate(connection);
	}

	@Override
	public List<Map<String, Object>> executeQuery(QueryModel model) {
		Parameters params = new Parameters();
		model.bindParameters(params);
		String sql = model.toSql();
		return namedTemplate.queryForList(sql, params.getParameters());
	}

	@Override
	public long executeCountQuery(QueryModel model) {
		Parameters params = new Parameters();
		model.bindParameters(params);
		String countSql = model.toCountSql();
		return namedTemplate.queryForObject(countSql, params.getParameters(), Long.class);
	}
	
	@Override
	public Set<Long> getDistinctBenefactors(QueryModel model, long limit) {
		Parameters params = new Parameters();
		model.bindParameters(params);
		String sql = model.toDistinctBenefactorSql(limit);
		final HashSet<Long> benefactorIds = new HashSet<>();
		namedTemplate.query(sql, params.getParameters(), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				benefactorIds.add(rs.getLong(TableConstants.ENTITY_REPLICATION_COL_BENEFACTOR_ID));
			}
		});
		return benefactorIds;
	}

	@Override
	public void addAnnotationsToResults(List<Map<String, Object>> results) {
		if(results.isEmpty()){
			// nothing to do if there are no results.
			return;
		}
		// Map each row it its ID
		final Map<Long, Map<String,Object>> idToRowMap = new HashMap<>(results.size());
		Set<Long> idSet = new HashSet<>(results.size());
		for(Map<String, Object> row: results){
			Object idObject = row.get(NodeField.ID.getFieldName());
			if(idObject == null){
				throw new IllegalStateException("Entity ID is missing from the results");
			}
			if(!(idObject instanceof Long)){
				throw new IllegalStateException("Unknown ID type in results: "+idObject.getClass());
			}
			Long id = (Long) idObject;
			idSet.add(id);
			idToRowMap.put(id, row);
		}
		// Fetch the annotations for the rows
		HashMap<String, Object> parameters = new HashMap<>(1);
		parameters.put(BIND_ENTITY_IDS, idSet);
		namedTemplate.query(SQL_SELECT_ANNOTATIONS_FOR_ENTITY_IDS, parameters, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Long entityId = rs.getLong(ANNOTATION_REPLICATION_COL_ENTITY_ID);
				String key = rs.getString(ANNOTATION_REPLICATION_COL_KEY);
				String value = rs.getString(ANNOTATION_REPLICATION_COL_STRING_VALUE);
				// add it to this row
				Map<String, Object> row = idToRowMap.get(entityId);
				row.put(key, value);
			}
		});
		
	}

}
