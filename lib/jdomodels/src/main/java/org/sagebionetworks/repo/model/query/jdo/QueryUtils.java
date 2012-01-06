package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashSet;

import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.jdo.JDONodeQueryDaoImpl.AttributeDoesNotExist;

@SuppressWarnings("rawtypes")
public class QueryUtils {
	
	private static final HashSet<String> primaryFields;
	
	// Import the primary fields from the dao
	static {
		primaryFields = new HashSet<String>();
		// First add all of the fields by name from the JDONOde object
		Field[] fields = DBONode.class.getDeclaredFields();
		for(Field field: fields){
			primaryFields.add(field.getName());
		}
		
		// This is a bit of a hack to filter on layers.
		primaryFields.add(SqlConstants.INPUT_DATA_LAYER_DATASET_ID);
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
		if(FieldType.BLOB_ATTRIBUTE == type){
			throw new UnsupportedOperationException("Blob annotaions are no longer supported");
		}else if(FieldType.DATE_ATTRIBUTE == type){
			return SqlConstants.TABLE_DATE_ANNOTATIONS;
		}else if(FieldType.DOUBLE_ATTRIBUTE == type){
			return SqlConstants.TABLE_DOUBLE_ANNOTATIONS;
		}else if(FieldType.STRING_ATTRIBUTE == type){
			return SqlConstants.TABLE_STRING_ANNOTATIONS;
		}else if(FieldType.LONG_ATTRIBUTE == type){
			return SqlConstants.TABLE_LONG_ANNOTATIONS;
		}else{
			throw new IllegalArgumentException("Unknown type: "+type);
		}
	}

}
