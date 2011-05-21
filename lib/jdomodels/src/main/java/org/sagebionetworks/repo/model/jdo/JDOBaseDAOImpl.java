package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * Base class for User and UserGroup DAOs
 */
@Transactional
abstract public class JDOBaseDAOImpl<S extends Base, T extends JDOBase> {
	
	@Autowired
	JdoTemplate jdoTemplate;
	
	
//	private static final Logger log = Logger
//	.getLogger(JDOBaseDAOImpl.class.getName());
	/**
	 * Create a new instance of the data transfer object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract S newDTO();
	
	/**
	 * Create a new instance of the persistable object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract T newJDO();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException
	 */
	abstract void copyToDto(T jdo, S dto) throws DatastoreException;

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 * @throws DatastoreException 
	 */
	abstract void copyFromDto(S dto, T jdo)
			throws InvalidModelException, DatastoreException;
	
	/**
	 * When retrieving objects by range, there has to be some
	 * nominal ordering strategy.  The DAO has to choose a default
	 * sort column, e.g. a 'name' field.
	 */
	abstract String defaultSortField();

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	abstract Class<T> getJdoClass();

	public String getType() {return getJdoClass().getName();}

	/**
	* Create a new object, using the information in the passed DTO
	* 
	* @param dto
	* @return the ID of the created object
	* @throws InvalidModelException
	*/
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(S dto) throws InvalidModelException, DatastoreException {
		try {
			dto.setCreationDate(new Date());
			
			T jdo = newJDO();
			copyFromDto(dto, jdo);
			jdo.setId(null); // system will generate the ID
			jdo.setEtag(0L);
			jdoTemplate.makePersistent(jdo);
			copyToDto(jdo, dto);
			return KeyFactory.keyToString(jdo.getId());
		} catch (InvalidModelException ime) {
			throw ime;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	/**
	* 
	* @param id
	*            id of the object to be retrieved
	* @return the DTO version of the retrieved object
	* @throws DatastoreException
	* @throws NotFoundException
	*/
	@Transactional(readOnly = true)
	public S get(String id) throws DatastoreException, NotFoundException {
		Long key = KeyFactory.stringToKey(id);
		try {
			T jdo = (T) jdoTemplate.getObjectById(getJdoClass(), key);
			S dto = newDTO();
			copyToDto(jdo, dto);
			return dto;
		} catch (JdoObjectRetrievalFailureException e) {
			throw new NotFoundException(e);
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	List<S> copyToDtoCollection(Collection<T> jdos) throws DatastoreException {
		List<S> ans = new ArrayList<S>();
		for (T jdo: jdos) {
			S dto = newDTO();
			copyToDto(jdo, dto);
			ans.add(dto);
		}
		return ans;
	}
	
	@Transactional(readOnly = true)	
	public Collection<S> getAll() throws DatastoreException {
		try {
			Collection<T> all = jdoTemplate.find(getJdoClass());
			return copyToDtoCollection(all);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	@Transactional(readOnly = true)	
	public Collection<S> getInRange(long fromIncl, long toExcl) throws DatastoreException {
		try {
			JDOExecutor exec = new JDOExecutor(jdoTemplate);
			List<T> all = exec.execute(getJdoClass(), 
					null,
					null,
					null,
					fromIncl,
					toExcl,
					defaultSortField());
			return copyToDtoCollection(all);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	/**
	* Delete the specified object
	* 
	* @param id
	*            the id of the object to be deleted
	* @throws DatastoreException
	* @throws NotFoundException
	*/
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		Long key = KeyFactory.stringToKey(id);
		try {
			T jdo = (T) jdoTemplate.getObjectById(getJdoClass(), key);
			jdoTemplate.deletePersistent(jdo);
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	// TODO: Need to add a locking mechanism like that used for JDONode
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(S dto) throws DatastoreException, InvalidModelException,
		NotFoundException {
		try {
			if (dto.getId() == null)
				throw new InvalidModelException("id is null");
			Long id = KeyFactory.stringToKey(dto.getId());
			T jdo = (T) jdoTemplate.getObjectById(getJdoClass(), id);
			copyFromDto(dto, jdo);
			jdo.setEtag(jdo.getEtag()+1L);
			jdoTemplate.makePersistent(jdo);
		} catch (InvalidModelException ime) {
			throw ime;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

}
