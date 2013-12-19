package org.sagebionetworks.repo.model.dbo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides basic CRUD operations for objects that implement DatabaseObject
 * 
 * @author jmhill
 *
 */
@SuppressWarnings("rawtypes")
public class DBOBasicDaoImpl implements DBOBasicDao, InitializingBean {
	
	public static final String GET_LAST_ID_SQL = "SELECT LAST_INSERT_ID()";
	
	@Autowired
	private DDLUtils ddlUtils;	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * Injected via Spring
	 */
	private List<DatabaseObject> databaseObjectRegister;
	
	
	/**
	 * Injected via spring
	 * @param databaseObjectRegister
	 */
	public void setDatabaseObjectRegister(List<DatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}

	/**
	 * We cache the SQL for each object type.
	 */
	private Map<Class<? extends DatabaseObject>, String> insertMap = new HashMap<Class<? extends DatabaseObject>, String>();
	private Map<Class<? extends DatabaseObject>, String> insertOnDuplicateUpdateMap = new HashMap<Class<? extends DatabaseObject>, String>();
	private Map<Class<? extends DatabaseObject>, String> fetchMap = new HashMap<Class<? extends DatabaseObject>, String>();
	private Map<Class<? extends DatabaseObject>, String> countMap = new HashMap<Class<? extends DatabaseObject>, String>();
	private Map<Class<? extends DatabaseObject>, String> deleteMap = new HashMap<Class<? extends DatabaseObject>, String>();
	private Map<Class<? extends DatabaseObject>, String> updateMap = new HashMap<Class<? extends DatabaseObject>, String>();
	
	/**
	 * We cache the mapping for each object type.
	 */
	private Map<Class<? extends DatabaseObject>, TableMapping> classToMapping = new HashMap<Class<? extends DatabaseObject>, TableMapping>();

	@Override
	public void afterPropertiesSet() throws Exception {
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		for(DatabaseObject dbo: databaseObjectRegister){
			TableMapping mapping = dbo.getTableMapping();
			ddlUtils.validateTableExists(mapping);
			// Create the Insert SQL
			String insertSQL = DMLUtils.createInsertStatement(mapping);
			this.insertMap.put(mapping.getDBOClass(), insertSQL);
			// INSERT ON DUPLICATE KEY UPDATE
			String insertOnDuplicateKeyUpdate = DMLUtils.getBatchInsertOrUdpate(mapping);
			this.insertOnDuplicateUpdateMap.put(mapping.getDBOClass(), insertOnDuplicateKeyUpdate);
			// The get SQL
			String getSQL = DMLUtils.createGetByIDStatement(mapping);
			this.fetchMap.put(mapping.getDBOClass(), getSQL);
			// The COUNT SQL
			String countSQL = DMLUtils.createGetCountStatement(mapping);
			this.countMap.put(mapping.getDBOClass(), countSQL);
			// The delete SQL
			String deleteSql = DMLUtils.createDeleteStatement(mapping);
			deleteMap.put(mapping.getDBOClass(), deleteSql);
			// The UPDATE sql
			String update = DMLUtils.createUpdateStatment(mapping);
			updateMap.put(mapping.getDBOClass(), update);
			this.classToMapping.put(mapping.getDBOClass(), dbo.getTableMapping());
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> T createNew(T toCreate) throws DatastoreException {
		if(toCreate == null) throw new IllegalArgumentException("The object to create cannot be null");
		// Lookup the insert SQL
		String insertSQl = getInsertSQL(toCreate.getClass());
		return insert(toCreate, insertSQl);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)	
	@Override
	public <T extends DatabaseObject<T>> T createOrUpdate(T toCreate) throws DatastoreException {
		// Lookup the insert SQL
		String insertOrUpdateSQl = getInsertOnDuplicateUpdateSQL(toCreate.getClass());
		return insert(toCreate, insertOrUpdateSQl);
	}

	private <T> T insert(T toCreate, String insertSQl) {
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(toCreate);
		try{
			simpleJdbcTemplate.update(insertSQl, namedParameters);
			// If this is an auto-increment class we need to fetch the new ID.
			if(toCreate instanceof AutoIncrementDatabaseObject){
				AutoIncrementDatabaseObject autoDBO = (AutoIncrementDatabaseObject) toCreate;
				Long id = simpleJdbcTemplate.queryForLong(GET_LAST_ID_SQL);
				autoDBO.setId(id);
			}
			return toCreate;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> List<T> createBatch(List<T> batch)	throws DatastoreException {
		if(batch == null) throw new IllegalArgumentException("The batch cannot be null");
		if(batch.size() < 1) throw new IllegalArgumentException("There must be at least one item in the batch");
		// Lookup the insert SQL
		String insertSQl = getInsertSQL(batch.get(0).getClass());
		return batchUpdate(batch, insertSQl, true);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> List<T> createOrUpdateBatch(
			List<T> batch) throws DatastoreException {
		if(batch == null) throw new IllegalArgumentException("The batch cannot be null");
		if(batch.size() < 1) throw new IllegalArgumentException("There must be at least one item in the batch");
		// Lookup the insert SQL
		String insertSQl = getInsertOnDuplicateUpdateSQL(batch.get(0).getClass());

		return batchUpdate(batch, insertSQl, false);
	}
	
	private <T> List<T> batchUpdate(List<T> batch, String sql, boolean enforceUpdate) {
		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
		for(int i=0; i<batch.size(); i++){
			namedParameters[i] = new BeanPropertySqlParameterSource(batch.get(i));
		}
		try{
			int[] updatedCountArray = simpleJdbcTemplate.batchUpdate(sql, namedParameters);
			if(enforceUpdate){
				for(int count: updatedCountArray){
					if(count != 1) throw new DatastoreException("Failed to insert without error");
				}
			}
			// If this is an auto-increment class we need to fetch the new ID.
			if(batch.get(0) instanceof AutoIncrementDatabaseObject){
				Long id = simpleJdbcTemplate.queryForLong(GET_LAST_ID_SQL);
				// Now get each ID
				int delta = batch.size()-1;
				for(int i=0; i<batch.size(); i++){
					AutoIncrementDatabaseObject aido = (AutoIncrementDatabaseObject) batch.get(i);
					aido.setId(new Long(id.longValue()-(delta-i)));
				}
			}
			return batch;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> boolean update(T toUpdate)	throws DatastoreException {
		String sql = getUpdateSQL(toUpdate.getClass());
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(toUpdate);
		try{
			int updatedCount = simpleJdbcTemplate.update(sql, namedParameters);
			return updatedCount > 0;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public <T extends DatabaseObject<T>> T getObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters) throws DatastoreException, NotFoundException{
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		if(namedParameters == null) throw new IllegalArgumentException("namedParameters cannot be null");
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(clazz);
		if(mapping == null) throw new IllegalArgumentException("Cannot find the mapping for Class: "+clazz+" The class must be added to the 'databaseObjectRegister'");
		String fetchSql = getFetchSQL(clazz);
		try{
			return simpleJdbcTemplate.queryForObject(fetchSql, mapping, namedParameters);
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}
	
	@Override
	public <T extends DatabaseObject<T>> long getCount(Class<? extends T> clazz) throws DatastoreException {
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(clazz);
		if(mapping == null) throw new IllegalArgumentException("Cannot find the mapping for Class: "+clazz+" The class must be added to the 'databaseObjectRegister'");
		String countSql = getCountSQL(clazz);
		return simpleJdbcTemplate.queryForLong(countSql);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> boolean deleteObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters) throws DatastoreException {
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		if(namedParameters == null) throw new IllegalArgumentException("namedParameters cannot be null");
		String sql = getDeleteSQL(clazz);
		int count = simpleJdbcTemplate.update(sql, namedParameters);
		return count == 1;
	}
	
	/**
	 * Get the insert sql for a given class.;
	 * @param clazz
	 * @return
	 */
	private String getInsertSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.insertMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the insert SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}
	
	/**
	 * Get the insert sql for a given class.;
	 * @param clazz
	 * @return
	 */
	private String getInsertOnDuplicateUpdateSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.insertOnDuplicateUpdateMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the 'INSERT...ON DUPLICATE KEY UPDATE' SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}
	
	/**
	 * The get sql for a given class.
	 * @param clazz
	 * @return
	 */
	private String getFetchSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.fetchMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the get SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}
	
	/**
	 * The count sql for a given class.
	 * @param clazz
	 * @return
	 */
	private String getCountSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.countMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the COUNT SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}
	
	/**
	 * The delete SQL for a given class
	 * @param clazz
	 * @return
	 */
	private String getDeleteSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.deleteMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the delete SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}

	/**
	 * The delete SQL for a given class
	 * @param clazz
	 * @return
	 */
	private String getUpdateSQL(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("The clazz cannot be null");
		String sql = this.updateMap.get(clazz);
		if(sql == null) throw new IllegalArgumentException("Cannot find the update SQL for class: "+clazz+".  Please register this class by adding it to the 'databaseObjectRegister' bean");
		return sql;
	}
}
