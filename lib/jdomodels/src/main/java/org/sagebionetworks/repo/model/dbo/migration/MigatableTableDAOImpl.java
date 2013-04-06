package org.sagebionetworks.repo.model.dbo.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This DAO can be used for any 
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigatableTableDAOImpl implements MigatableTableDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * Injected via Spring
	 */
	private List<MigratableDatabaseObject> databaseObjectRegister;
	
	
	/**
	 * Injected via Spring
	 * @param databaseObjectRegister
	 */
	public void setDatabaseObjectRegister(List<MigratableDatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}
	
	private Map<MigratableTableType, String> deleteSqlMap = new HashMap<MigratableTableType, String>();
	private Map<MigratableTableType, String> countSqlMap = new HashMap<MigratableTableType, String>();
	/**
	 * Called when this bean is ready.
	 */
	public void initialize(){
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		for(MigratableDatabaseObject dbo: databaseObjectRegister){
			TableMapping mapping = dbo.getTableMapping();
			MigratableTableType type = dbo.getMigratableTableType();
			// Build up the SQL cache.
			String delete = DMLUtils.createBatchDelete(mapping);
			deleteSqlMap.put(type, delete);
			String count = DMLUtils.createGetCountStatement(mapping);
			countSqlMap.put(type, count);
			String listRowMetadataSQL = DMLUtils.listRowMetadata(mapping);
		}
	}

	@Override
	public long getCount(MigratableTableType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String countSql = this.countSqlMap.get(type);
		if(countSql == null) throw new IllegalArgumentException("Cannot find count SQL for "+type);
		return simpleJdbcTemplate.queryForLong(countSql);
	}

	@Override
	public QueryResults<RowMetadata> listRowMetadata(MigratableTableType type, long limit, long offest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RowMetadata> listDeltaRowMetadata(MigratableTableType type,	List<String> idList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void backupToStream(MigratableTableType type, List<String> rowIds, OutputStream out) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restoreFromStream(MigratableTableType type, InputStream in) {
		// TODO Auto-generated method stub
		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public int deleteObjectsById(MigratableTableType type, List<String> idList) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		if(idList == null) throw new IllegalArgumentException("idList cannot be null");
		String deleteSQL = this.deleteSqlMap.get(type);
		if(deleteSQL == null) throw new IllegalArgumentException("Cannot find batch delete SQL for "+type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, idList);
		return simpleJdbcTemplate.update(deleteSQL, params);
	}


}
