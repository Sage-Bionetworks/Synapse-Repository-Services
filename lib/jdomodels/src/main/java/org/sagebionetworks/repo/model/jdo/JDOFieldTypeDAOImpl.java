package org.sagebionetworks.repo.model.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.query.FieldType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents the annotation types associated with each annotation name.
 * 
 * @author jmhill
 * 
 */
@Transactional(readOnly = true)
public class JDOFieldTypeDAOImpl implements FieldTypeDAO, InitializingBean {

	@Autowired
	JdoTemplate jdoTemplate;

	/**
	 * Since the types never change once they are set, we can safely cache the results.
	 */
	private Map<String, FieldType> localCache = new HashMap<String, FieldType>();
	
	public JDOFieldTypeDAOImpl(){
	}

	/**
	 * Used for a mocking unit test.
	 * @param mockTemplate
	 */
	public JDOFieldTypeDAOImpl(JdoTemplate mockTemplate) {
		jdoTemplate = mockTemplate;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean addNewType(String name, FieldType type) throws DatastoreException {
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		if(type == null) throw new IllegalArgumentException("FieldType cannot be null");
		// First check the local cache
		FieldType currentType = localCache.get(type.name());
		if(currentType != null){
			validateType(name, type, currentType);
			return true;
		}
		// First determine if this type already exists
		try {
			JDOAnnotationType exists = jdoTemplate.getObjectById(JDOAnnotationType.class, name);
			currentType = FieldType.valueOf(exists.getTypeClass());
			validateType(name, type, currentType);
			return true;
		} catch (Exception e) {
			// this means the type does not exist so create it
			JDOAnnotationType jdoType = new JDOAnnotationType();
			jdoType.setAttributeName(name);
			jdoType.setTypeClass(type.toString());
			jdoTemplate.makePersistent(jdoType);
			// Add this to the local map
			localCache.put(name, type);
			return false;
		}
	}

	/**
	 * Validate that the passed type matches the current type.
	 * @param name
	 * @param type
	 * @param currentType
	 * @throws DatastoreException
	 */
	private static void validateType(String name, FieldType newType, FieldType currentType)
			throws DatastoreException {
		if (newType != currentType) {
			throw new DatastoreException("The annotation name: " + name
					+ " cannot be used for a type of: " + newType.name()
					+ " because it has already been used for a type of: "
					+ currentType.name());
		}
	}

	@Transactional(readOnly = true)
	@Override
	public FieldType getTypeForName(String name) {
		// Since the values never change we can first look it up in the local
		// cache
		FieldType type = localCache.get(name);
		if (type != null)
			return type;
		// Look it up in the db
		try {
			JDOAnnotationType exists = jdoTemplate.getObjectById(JDOAnnotationType.class, name);
			type = FieldType.valueOf(exists.getTypeClass());
			// Add this to the local cache
			localCache.put(name, type);
			return type;
		} catch (Exception e) {
			// this means the type does not exist
			return FieldType.DOES_NOT_EXIST;
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String name) {
		JDOAnnotationType mapping = jdoTemplate.getObjectById(JDOAnnotationType.class, name);
		jdoTemplate.deletePersistent(mapping);
		localCache.remove(name);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void afterPropertiesSet() throws Exception {
		// Make sure the primary Node fields are in place
		Field[] fields = Node.class.getDeclaredFields();
		for(Field field: fields){
			this.addNewType(field.getName(), FieldType.PRIMARY_FIELD);
		}
	}

}
