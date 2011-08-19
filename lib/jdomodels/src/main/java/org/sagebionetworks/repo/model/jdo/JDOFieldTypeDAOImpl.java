package org.sagebionetworks.repo.model.jdo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
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
	
	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern.compile("^[a-z,A-Z,0-9,_,.]+");

	/**
	 * Since the types never change once they are set, we can safely cache the results.
	 */
	private Map<String, FieldType> localCache = Collections.synchronizedMap(new HashMap<String, FieldType>());
	
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
	public boolean addNewType(String name, FieldType type) throws DatastoreException, InvalidModelException {
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		if(type == null) throw new IllegalArgumentException("FieldType cannot be null");
		// check the name
		name = checkKeyName(name);
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
	 * @param nodeType
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
	
	/**
	 * Validate the name
	 * @param key
	 * @throws InvalidModelException
	 */
	static String checkKeyName(String key) throws InvalidModelException {
		if(key == null) throw new InvalidModelException("Annotation names cannot be null");
		key = key.trim();
		if("".equals(key)) throw new InvalidModelException("Annotation names cannot be empty strings");
		Matcher matcher = ALLOWABLE_CHARS.matcher(key);
		if (!matcher.matches()) {
			throw new InvalidModelException("Invalid annotation name: '"+key+"'. Annotation names may only contain; letters, numbers, '_' and '.'");
		}
		return key;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void validateAnnotations(Annotations updated)
			throws DatastoreException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		// Validate the annotation names
		
		// Validate the strings
		if(updated.getStringAnnotations() != null){
			Iterator<String> it = updated.getStringAnnotations().keySet().iterator();
			while(it.hasNext()){
				addNewType(it.next(), FieldType.STRING_ATTRIBUTE);
			}
		}
		// Validate the longs
		if(updated.getLongAnnotations() != null){
			Iterator<String> it = updated.getLongAnnotations().keySet().iterator();
			while(it.hasNext()){
				addNewType(it.next(), FieldType.LONG_ATTRIBUTE);
			}
		}
		// Validate the dates
		if(updated.getDateAnnotations() != null){
			Iterator<String> it = updated.getDateAnnotations().keySet().iterator();
			while(it.hasNext()){
				addNewType(it.next(), FieldType.DATE_ATTRIBUTE);
			}
		}
		// Validate the Doubles
		if(updated.getDoubleAnnotations() != null){
			Iterator<String> it = updated.getDoubleAnnotations().keySet().iterator();
			while(it.hasNext()){
				addNewType(it.next(), FieldType.DOUBLE_ATTRIBUTE);
			}
		}
		// Validate the Doubles
		if(updated.getBlobAnnotations() != null){
			Iterator<String> it = updated.getBlobAnnotations().keySet().iterator();
			while(it.hasNext()){
				addNewType(it.next(), FieldType.BLOB_ATTRIBUTE);
			}
		}
		
	}

}
