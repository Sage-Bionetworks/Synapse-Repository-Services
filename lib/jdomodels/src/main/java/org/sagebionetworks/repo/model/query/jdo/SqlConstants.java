package org.sagebionetworks.repo.model.query.jdo;

import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.ObjectType;

@SuppressWarnings("rawtypes")
public class SqlConstants {
	// The alias used for the dataset table.
	public static final String PRIMARY_ALIAS	= "prm";
	// This seems to be the name of the id column for all tables.
	public static final String COLUMN_ID		= "id";
	// This is the column on a primary that that annotations can be joined with.
	public static final String PRIMARY_ANNOTATION_ID = "ANNOTATIONS_ID_OID";
	// The annotation table's foreign keys
	public static final String FOREIGN_KEY_LONG_ANNOTATION		= "LONG_ANNOTATIONS_ID_OWN";
	public static final String FOREIGN_KEY_STRING_ANNOTATION	= "STRING_ANNOTATIONS_ID_OWN";
	public static final String FOREIGN_KEY_DATE_ANNOTATION		= "DATE_ANNOTATIONS_ID_OWN";
	public static final String FOREIGN_KEY_DOUBLE_ANNOTATION	= "DOUBLE_ANNOTATIONS_ID_OWN";
	
	public static final String ANNOTATION_ATTRIBUTE_COLUMN = "attribute";
	public static final String ANNOTATION_VALUE_COLUMN = "value";
	
	// This is the alias of the sub-query used for sorting on annotations.
	public static final String ANNOTATION_SORT_SUB_ALIAS 	= "assa";
	
	public static final String OPERATOR_SQL_EQUALS			= "=";
	public static final String OPERATOR_SQL_DOES_NOT_EQUAL	= "!=";
	public static final String OPERATOR_SQL_GREATER_THAN	= ">";
	public static final String OPERATOR_SQL_LESS_THAN		= "<";
	
	public static final String INPUT_DATA_LAYER_DATASET_ID = "INPUT_LAYERS_ID_OWN";
	
	/**
	 * Get the JDO class for each field type.
	 * @param type
	 * @return
	 * @throws AttributeDoesNotExist 
	 */
	public static Class getJdoClassForFieldType(FieldType type) {
		if(FieldType.STRING_ATTRIBUTE == type){
			return JDOStringAnnotation.class;
		}else if(FieldType.DATE_ATTRIBUTE == type){
			return JDODateAnnotation.class;
		}else if(FieldType.LONG_ATTRIBUTE == type){
			return JDOLongAnnotation.class;
		}else if(FieldType.DOUBLE_ATTRIBUTE == type){
			return JDODoubleAnnotation.class;
		}else{
			throw new IllegalArgumentException("No class for : "+type);
		}
	}
	
	/**
	 * Get the JDO class for a given object type.
	 * @param type
	 * @return
	 */
	public static Class getJdoClassForObjectType(ObjectType type){
		if(ObjectType.dataset == type){
			return JDODataset.class;
		}else if(ObjectType.layer == type){
			return JDOInputDataLayer.class;
		}else{
			throw new IllegalArgumentException("There is class for type: "+type);
		}
		
	}
	
	/**
	 * For a given type, what is the foreign key column name of the associated 
	 * annotation table.
	 * @param type
	 * @return
	 */
	public static String getForeignKeyColumnNameForType(FieldType type){
		if(FieldType.STRING_ATTRIBUTE == type){
			return FOREIGN_KEY_STRING_ANNOTATION;
		}else if(FieldType.DATE_ATTRIBUTE == type){
			return FOREIGN_KEY_DATE_ANNOTATION;
		}else if(FieldType.LONG_ATTRIBUTE == type){
			return FOREIGN_KEY_LONG_ANNOTATION;
		}else if(FieldType.DOUBLE_ATTRIBUTE == type){
			return FOREIGN_KEY_DOUBLE_ANNOTATION;
		}else{
			throw new IllegalArgumentException("There is no table for type: "+type);
		}
	}
	
	/**
	 * Translate an Comparator to SQL
	 * @param comp
	 * @return
	 */
	public static String getSqlForComparator(Compartor comp){
		if(Compartor.EQUALS == comp){
			return OPERATOR_SQL_EQUALS;
		}else if(Compartor.NOT_EQUALS == comp){
			return OPERATOR_SQL_DOES_NOT_EQUAL;
		}else if(Compartor.GREATER_THAN == comp){
			return OPERATOR_SQL_GREATER_THAN;
		}else if(Compartor.LESS_THAN == comp){
			return OPERATOR_SQL_LESS_THAN;
		}else{
			throw new IllegalArgumentException("Unsupported Compartor: "+comp);
		}
	}

}
