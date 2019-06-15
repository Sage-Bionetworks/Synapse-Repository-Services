package org.sagebionetworks.repo.model.dbo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Provides basic CRUD operations for objects that implement DatabaseObject
 * 
 * @author jmhill
 *
 */
@SuppressWarnings("rawtypes")
public class DBOBasicDaoImpl implements DBOBasicDao, InitializingBean {
	
	public static final String GET_LAST_ID_SQL = "SELECT LAST_INSERT_ID()";

	public static final String GET_DATABASE_UNIX_TIMESTAMP_MILLIS = "SELECT CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED)";
	
	@Autowired
	private DDLUtils ddlUtils;	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	/**
	 * Injected via Spring
	 */
	private List<DatabaseObject> databaseObjectRegister;
	
	/**
	 * Map of MySQL function names to function definition file names used
	 * to create/update MySQL functions.
	 * 
	 * Injected via Spring
	 * 
	 */
	private Map<String, String> functionMap;

	/**
	 * Map of MySQL function names to function definition file names used
	 * to create/update MySQL functions.
	 * 
	 * Injected via Spring
	 * 
	 */
	public void setFunctionMap(Map<String, String> functionMap) {
		this.functionMap = functionMap;
	}

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
			String countSQL = DMLUtils.createGetCountByPrimaryKeyStatement(mapping);
			this.countMap.put(mapping.getDBOClass(), countSQL);
			// The delete SQL
			String deleteSql = DMLUtils.createDeleteStatement(mapping);
			deleteMap.put(mapping.getDBOClass(), deleteSql);
			// The UPDATE sql
			String update = DMLUtils.createUpdateStatment(mapping);
			updateMap.put(mapping.getDBOClass(), update);
			this.classToMapping.put(mapping.getDBOClass(), dbo.getTableMapping());
		}
		
		/**
		 * Create all functions that should be created.
		 */
		if(functionMap != null){
			for(String functionName: functionMap.keySet()){
				ddlUtils.createFunction(functionName, functionMap.get(functionName));
			}
		}
	}

	@WriteTransaction
	@Override
	public <T extends DatabaseObject<T>> T createNew(T toCreate) throws DatastoreException {
		if(toCreate == null) throw new IllegalArgumentException("The object to create cannot be null");
		// Lookup the insert SQL
		String insertSQl = getInsertSQL(toCreate.getClass());
		return insert(toCreate, insertSQl);
	}
	
	@WriteTransaction	
	@Override
	public <T extends DatabaseObject<T>> T createOrUpdate(T toCreate) throws DatastoreException {
		// Lookup the insert SQL
		String insertOrUpdateSQl = getInsertOnDuplicateUpdateSQL(toCreate.getClass());
		return insert(toCreate, insertOrUpdateSQl);
	}
	
	private <T> T insert(T toCreate, String insertSQl) {
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(toCreate.getClass());
		if (mapping == null) {
			throw new IllegalArgumentException("Cannot find the mapping for Class: " + toCreate.getClass()
					+ " The class must be added to the 'databaseObjectRegister'");
		}
		SqlParameterSource namedParameters = getSqlParameterSource(toCreate,mapping);
		try{
			namedJdbcTemplate.update(insertSQl, namedParameters);
			// If this is an auto-increment class we need to fetch the new ID.
			if(toCreate instanceof AutoIncrementDatabaseObject){
				AutoIncrementDatabaseObject autoDBO = (AutoIncrementDatabaseObject) toCreate;
				Long id = jdbcTemplate.queryForObject(GET_LAST_ID_SQL, Long.class);
				autoDBO.setId(id);
			}
			return toCreate;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}

	@WriteTransaction
	@Override
	public <T extends DatabaseObject<T>> List<T> createBatch(List<T> batch)	throws DatastoreException {
		if(batch == null) throw new IllegalArgumentException("The batch cannot be null");
		if(batch.size() < 1) throw new IllegalArgumentException("There must be at least one item in the batch");
		// Lookup the insert SQL
		String insertSQl = getInsertSQL(batch.get(0).getClass());
		return batchUpdate(batch, insertSQl, true);
	}

	@WriteTransaction
	@Override
	public <T extends DatabaseObject<T>> List<T> createOrUpdateBatch(
			List<T> batch) throws DatastoreException {
		if(batch == null) throw new IllegalArgumentException("The batch cannot be null");
		if(batch.size() < 1) throw new IllegalArgumentException("There must be at least one item in the batch");
		// Lookup the insert SQL
		String insertSQl = getInsertOnDuplicateUpdateSQL(batch.get(0).getClass());

		return batchUpdate(batch, insertSQl, false);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> batchUpdate(List<T> batch, String sql, boolean enforceUpdate) {
		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
		TableMapping<T> mapping = null;
		for(int i=0; i<batch.size(); i++){
			if (mapping == null) {
				mapping = classToMapping.get(batch.get(i).getClass());
				if (mapping == null) {
					throw new IllegalArgumentException("Cannot find the mapping for Class: " + batch.get(i).getClass()
							+ " The class must be added to the 'databaseObjectRegister'");
				}
			}
			namedParameters[i] = getSqlParameterSource(batch.get(i), mapping);
		}
		try{
			int[] updatedCountArray = namedJdbcTemplate.batchUpdate(sql, namedParameters);
			if(enforceUpdate){
				for(int count: updatedCountArray){
					if(count != 1) throw new DatastoreException("Failed to insert without error");
				}
			}
			// If this is an auto-increment class we need to fetch the new ID.
			if(batch.get(0) instanceof AutoIncrementDatabaseObject){
				Long id = jdbcTemplate.queryForObject(GET_LAST_ID_SQL, Long.class);
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
	
	
	@WriteTransaction
	@Override
	public <T extends DatabaseObject<T>> boolean update(T toUpdate)	throws DatastoreException {
		String sql = getUpdateSQL(toUpdate.getClass());
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(toUpdate.getClass());
		if (mapping == null) {
			throw new IllegalArgumentException("Cannot find the mapping for Class: " + toUpdate.getClass()
					+ " The class must be added to the 'databaseObjectRegister'");
		}
		SqlParameterSource namedParameters = getSqlParameterSource(toUpdate, mapping);
		try{
			int updatedCount = namedJdbcTemplate.update(sql, namedParameters);
			return updatedCount > 0;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public <T extends DatabaseObject<T>> T getObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters)
			throws DatastoreException, NotFoundException {
		try {
			return doGetObjectByPrimaryKey(clazz, namedParameters, false);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	@Override
	public <T extends DatabaseObject<T>> T getObjectByPrimaryKeyIfExists(Class<? extends T> clazz, SqlParameterSource namedParameters)
			throws DatastoreException {
		try {
			return doGetObjectByPrimaryKey(clazz, namedParameters, false);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public <T extends DatabaseObject<T>> T getObjectByPrimaryKeyWithUpdateLock(Class<? extends T> clazz, SqlParameterSource namedParameters)
			throws DatastoreException, NotFoundException {
		try {
			return doGetObjectByPrimaryKey(clazz, namedParameters, true);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	private <T extends DatabaseObject<T>> T doGetObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters,
			boolean updateLock) throws DatastoreException {
		if (clazz == null)
			throw new IllegalArgumentException("Clazz cannot be null");
		if (namedParameters == null)
			throw new IllegalArgumentException("namedParameters cannot be null");
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(clazz);
		if (mapping == null)
			throw new IllegalArgumentException("Cannot find the mapping for Class: " + clazz
					+ " The class must be added to the 'databaseObjectRegister'");
		String fetchSql = getFetchSQL(clazz);
		if (updateLock) {
			fetchSql += " FOR UPDATE";
		}
		return namedJdbcTemplate.queryForObject(fetchSql, namedParameters, mapping);
	}

	@Override
	public <T extends DatabaseObject<T>> long getCount(Class<? extends T> clazz) throws DatastoreException {
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		@SuppressWarnings("unchecked")
		TableMapping<T> mapping = classToMapping.get(clazz);
		if(mapping == null) throw new IllegalArgumentException("Cannot find the mapping for Class: "+clazz+" The class must be added to the 'databaseObjectRegister'");
		String countSql = getCountSQL(clazz);
		return jdbcTemplate.queryForObject(countSql, Long.class);
	}

	@Override
	public long getDatabaseTimestampMillis() {
		return jdbcTemplate.queryForObject(GET_DATABASE_UNIX_TIMESTAMP_MILLIS, Long.class);
	}

	@WriteTransaction
	@Override
	public <T extends DatabaseObject<T>> boolean deleteObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters) throws DatastoreException {
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		if(namedParameters == null) throw new IllegalArgumentException("namedParameters cannot be null");
		String sql = getDeleteSQL(clazz);
		int count = namedJdbcTemplate.update(sql, namedParameters);
		return count == 1;
	}
	
	private <T> SqlParameterSource getSqlParameterSource(T toCreate, TableMapping<T> mapping) {
		if (mapping instanceof AutoTableMapping) {
			return ((AutoTableMapping) mapping).getSqlParameterSource(toCreate);
		}
		return new BeanPropertySqlParameterSource(toCreate);
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
	 * The update SQL for a given class
	 * 
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
