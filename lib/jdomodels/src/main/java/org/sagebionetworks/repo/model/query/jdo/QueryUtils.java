package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sagebionetworks.repo.model.jdo.BasicIdentifierFactory;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.jdo.JDONodeQueryDaoImpl.AttributeDoesNotExist;

@SuppressWarnings("rawtypes")
public class QueryUtils {
	
	private static final HashSet<String> primaryFields;
	private static final Map<Class, String> classTableMap;
	
	// Import the primary fields from the dao
	static {
		primaryFields = new HashSet<String>();
		// First add all of the fields by name from the JDONOde object
		Field[] fields = JDONode.class.getDeclaredFields();
		for(Field field: fields){
			primaryFields.add(field.getName());
		}
		
		// This is a bit of a hack to filter on layers.
		primaryFields.add(SqlConstants.INPUT_DATA_LAYER_DATASET_ID);
		
		// Build up the map of classes to table names.
		classTableMap = new HashMap<Class, String>();
		BasicIdentifierFactory nameFactory = new BasicIdentifierFactory();
		nameFactory.setWordSeparator("");
		classTableMap.put(JDONode.class, nameFactory.generateIdentifierNameForJavaName(JDONode.class.getSimpleName()));
		classTableMap.put(JDOStringAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDOStringAnnotation.class.getSimpleName()));
		classTableMap.put(JDOLongAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDOLongAnnotation.class.getSimpleName()));
		classTableMap.put(JDODoubleAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDODoubleAnnotation.class.getSimpleName()));
		classTableMap.put(JDODateAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDODateAnnotation.class.getSimpleName()));
	}
	
	/**
	 * Get the table name for a given class.
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getTableNameForClass(Class clazz) {
		String tableName = classTableMap.get(clazz);
		if (tableName == null)
			throw new IllegalArgumentException(
					"A table name does not exist for class: " + clazz.getName());
		return tableName;
	}

	/**
	 * What is the table name for a given field type.
	 * 
	 * @param type
	 * @return
	 * @throws AttributeDoesNotExist
	 */
	public static String getTableNameForFieldType(FieldType type) {
		// Get the class and use it to lookup the table
		Class clazz = SqlConstants.getJdoClassForFieldType(type);
		// Get the table for this class
		return getTableNameForClass(clazz);
	}

}
