package org.sagebionetworks.repo.model.dbo;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A basic DAO for database objects.
 * 
 * @author jmhill
 *
 */
public interface DBOBasicDao {
	

	/**
	 * Create a new Database object.
	 * @param <T>
	 * @param toCreate
	 * @return
	 * @throws DatastoreException 
	 */
	public <T extends DatabaseObject<T>> T createNew(T toCreate) throws DatastoreException;
	
	/**
	 * Do a batch create.
	 * @param <T>
	 * @param batch
	 * @return
	 * @throws DatastoreException
	 */
	public <T extends DatabaseObject<T>> List<T> createBatch(List<T> batch) throws DatastoreException;
	
	/**
	 * Update an existing object.
	 * @param <T>
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 */
	public <T extends DatabaseObject<T>> boolean update(T toUpdate) throws DatastoreException;
	
	/**
	 * Get an object using its ID.
	 * @param <T>
	 * @param clazz
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public <T extends DatabaseObject<T>> T getObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param clazz
	 * @param id
	 * @return
	 * @throws DatastoreException
	 */
	public <T extends DatabaseObject<T>> boolean deleteObjectByPrimaryKey(Class<? extends T> clazz, SqlParameterSource namedParameters) throws DatastoreException;

	public <T extends DatabaseObject<T>> long getCount(Class<? extends T> clazz) throws DatastoreException;
}
