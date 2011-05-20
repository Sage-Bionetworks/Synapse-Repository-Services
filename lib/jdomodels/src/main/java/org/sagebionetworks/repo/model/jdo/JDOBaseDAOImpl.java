package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	@Transactional(readOnly = true)	
	public Collection<S> getAll() throws DatastoreException {
		try {
			Collection<T> all = jdoTemplate.find(getJdoClass());
			Collection<S> ans = new ArrayList<S>();
			for (T jdo: all) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
			}
			return ans;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(S dto) throws DatastoreException, InvalidModelException,
		NotFoundException {
		try {
			if (dto.getId() == null)
				throw new InvalidModelException("id is null");
			Long id = KeyFactory.stringToKey(dto.getId());
			T jdo = (T) jdoTemplate.getObjectById(getJdoClass(), id);
			copyFromDto(dto, jdo);
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
