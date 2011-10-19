package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.jdo.BasicIdentifierFactory;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDONodeType;
import org.sagebionetworks.repo.model.jdo.persistence.JDOReference;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.jdo.JDONodeQueryDaoImpl.AttributeDoesNotExist;
 
@SuppressWarnings("rawtypes")
public class SqlConstants {
	
	// Node table constants
	public static final String TABLE_NODE 				= "JDONODE";
	public static final String COL_NODE_ID				= "ID";
	public static final String COL_NODE_PARENT_ID		= "PARENT_ID";
	public static final String COL_NODE_BENEFACTOR_ID	= "BENEFACTOR_ID";
	public static final String COL_NODE_NAME			= "NAME";
	public static final String COL_NODE_ANNOATIONS		= "ANNOTATIONS_ID_OID";
	public static final String COL_NODE_DESCRIPTION 	= "DESCRIPTION";
	public static final String COL_NODE_ETAG 			= "ETAG";
	public static final String COL_NODE_CREATED_BY 		= "CREATED_BY";
	public static final String COL_NODE_CREATED_ON 		= "CREATED_ON";
	public static final String COL_NODE_MODIFIED_BY 	= "MODIFIED_BY";
	public static final String COL_NODE_MODIFIED_ON 	= "MODIFIED_ON";
	public static final String COL_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACL				= "NODE_ACL";
	public static final String COL_CURRENT_REV			= "CURRENT_REV_NUM";
	
	// The Revision table
	public static final String TABLE_REVISION 			= "JDOREVISION";
	public static final String COL_REVISION_OWNER_NODE	= "OWNER_NODE_ID";
	public static final String COL_REVISION_NUMBER		= "NUMBER";
	public static final String COL_REVISION_LABEL		= "LABEL";
	public static final String COL_REVISION_COMMENT		= "COMMENT";
	public static final String COL_REVISION_ANNOS_BLOB	= "ANNOTATIONS";
	public static final String COL_REVISION_REFS_BLOB	= "REFERENCES";

	// The Reference table
	public static final String TABLE_REFERENCE						= "JDOREFERENCE";
	public static final String COL_REFERENCE_OWNER_NODE				= "REF_OWNER_NODE_ID";
	public static final String COL_REFERENCE_TARGET_NODE			= "REF_TARGET_NODE_ID";
	public static final String COL_REFERENCE_TARGET_REVISION_NUMBER	= "REF_TARGET_REV_NUM";
	public static final String COL_REFERENCE_GROUP_NAME				= "REF_GROUP_NAME";
	
	// Annotations tables
	public static final String TABLE_STRING_ANNOTATIONS	= "JDOSTRINGANNOTATION";
	public static final String TABLE_DOUBLE_ANNOTATIONS	= "JDODOUBLEANNOTATION";
	public static final String TABLE_LONG_ANNOTATIONS	= "JDOLONGANNOTATION";
	public static final String TABLE_BLOB_ANNOTATIONS	= "JDOBLOBANNOTATION";
	public static final String TABLE_DATE_ANNOTATIONS	= "JDODATEANNOTATION";
	// There are the column names that all annotation tables have.
	public static final String ANNOTATION_ATTRIBUTE_COLUMN 		= "ATTRIBUTE";
	public static final String ANNOTATION_VALUE_COLUMN			= "VALUE";
	public static final String ANNOTATION_OWNER_ID_COLUMN		= "OWNER_ID";
	
	// The name of the node type table.
	public static final String TABLE_NODE_TYPE				= "NODE_TYPE";
	public static final String TABLE_ANNOTATION_TYPE		= "ANNOTATION_TYPE";

	public static final String TABLE_USER					= "JDOUSER";
	public static final String TABLE_USER_GROUP				= "JDOUSERGROUP";
	public static final String TABLE_USER_GROUP_USERS		= "JDOUSERGROUPUSERS";
	public static final String COL_USER_GROUP_ID			= "ID";
	public static final String COL_USER_GROUP_NAME 			= "NAME";
	public static final String COL_USER_GROUP_IS_INDIVIDUAL = "ISINDIVIDUAL";
	
	public static final String TABLE_ACCESS_CONTROL_LIST = "ACL";
	public static final String COL_ACL_ID				= "ID";
	public static final String COL_ACL_OWNER_ID			= "NODE_OWNER";
	public static final String ACL_OWNER_ID_COLUMN		= "OWNER_ID_COLUMN";
	
	// The resource access table
	public static final String TABLE_RESOURCE_ACCESS			= "JDORESOURCEACCESS";
	public static final String COL_RESOURCE_ACCESS_OWNER		= "OWNER_ID";
	public static final String COL_RESOURCE_ACCESS_GROUP_ID		= "GROUP_ID";
	public static final String COL_RESOURCE_ACCESS_TYPE			= "RESOURCE_TYPE";
	public static final String COL_RESOURCE_ACCESS_RESOURCE_ID	= "RESOURCE_ID";
	public static final String COL_RESOURCE_ACCESS_ID			= "ID";
	
	// The backup/restore status table
	public static final String TABLE_BACKUP_STATUS 				= "JDO_BACKUP_RESTORE_STATUS";
	public static final String COL_BACKUP_ID					= "ID";
	public static final String COL_BACKUP_STATUS				= "STATUS";
	public static final String COL_BACKUP_TYPE					= "TYPE";
	public static final String COL_BACKUP_STARTED_BY 			= "STARTED_BY";
	public static final String COL_BACKUP_STARTED_ON 			= "STARTED_ON";
	public static final String COL_BACKUP_PROGRESS_MESSAGE		= "PROGRESS_MESSAGE";
	public static final String COL_BAKUP_PROGRESS_CURRENT		= "PROGRESS_CURRENT";
	public static final String COL_BACKUP_PROGRESS_TOTAL		= "PROGRESS_TOTAL";
	public static final String COL_BACKUP_ERORR_MESSAGE			= "ERROR_MESSAGE";
	public static final String COL_BACKUP_ERROR_DETAILS			= "ERROR_DETAILS";
	public static final String COL_BACKUP_URL					= "BACKUP_URL";
	public static final String COL_BACKUP_RUNTIME				= "RUN_TIME_MS";
	
	public static final String TABLE_BACKUP_TERMINATE 			= "JDO_BACKUP_TERMINATE";
	public static final String COL_BACKUP_TERM_OWNER			= "BACKUP_OWNER";
	public static final String COL_BACKUP_FORCE_TERMINATION		= "FORCE_TERMINATION";
		
	// The resource access join table
	// datanucleus doesn't seem to be respecting the join table name when creating the schema
	// so I've modified the string to match the generated name
	public static final String TABLE_RESOURCE_ACCESS_TYPE		= "JDORESOURCEACCESS_ACCESSTYPE"; 
	public static final String COL_RESOURCE_ACCESS_TYPE_ID		= "ID_OID";
	public static final String COL_RESOURCE_ACCESS_TYPE_ELEMENT	= "STRING_ELE";
	
	// This constraint ensure that children names are unique within their parent.
	public static final String CONSTRAINT_UNIQUE_CHILD_NAME = "NODE_UNIQUE_CHILD_NAME";
	
	
	// The alias used for the dataset table.
	public static final String PRIMARY_ALIAS	= "prm";
	// This seems to be the name of the id column for all tables.
	public static final String COLUMN_ID		= "id";
	
	public static final String TYPE_COLUMN_NAME = "nodeType";
	
	public static final String AUTH_FILTER_ALIAS = "auth";
	

	
	// This is the alias of the sub-query used for sorting on annotations.
	public static final String ANNOTATION_SORT_SUB_ALIAS 	= "assa";
	
	public static final String OPERATOR_SQL_EQUALS					= "=";
	public static final String OPERATOR_SQL_DOES_NOT_EQUAL			= "!=";
	public static final String OPERATOR_SQL_GREATER_THAN			= ">";
	public static final String OPERATOR_SQL_LESS_THAN				= "<";
	public static final String OPERATOR_SQL_GREATER_THAN_OR_EQUALS	= ">=";
	public static final String OPERATOR_SQL_LESS_THAN_OR_EQUALS		= "<=";
	
	
	public static final String INPUT_DATA_LAYER_DATASET_ID = "INPUT_LAYERS_ID_OWN";
	
	private static final Map<String, String> primaryFieldColumns;
	private static final Map<String, String> mapClassToTable;




	static{
		// Map column names to the field names
		// RELEASE_DATE,STATUS,PLATFORM,PROCESSING_FACILITY,QC_BY,QC_DATE,TISSUE_TYPE,TYPE,CREATION_DATE,DESCRIPTION,PREVIEW,PUBLICATION_DATE,RELEASE_NOTES
		primaryFieldColumns = new HashMap<String, String>();
		
		SqlConstants.addAllFields(Node.class, primaryFieldColumns);
		// This is a special case for nodes.
		primaryFieldColumns.put(NodeConstants.COL_PARENT_ID, "PARENT_ID_OID");
		
		// These will be deleted once we move to NodeDao
		SqlConstants.addAllFields(Dataset.class, primaryFieldColumns);
		SqlConstants.addAllFields(Layer.class, primaryFieldColumns);
		primaryFieldColumns.put(NodeConstants.COL_PARENT_ID, "PARENT_ID");
		primaryFieldColumns.put("INPUT_LAYERS_ID_OWN", "INPUT_LAYERS_ID_OWN");
		
		// This is the map of varrious classes to their table names
		mapClassToTable = new HashMap<String, String>();
		mapClassToTable.put(JDONode.class.getName(),				TABLE_NODE);
		mapClassToTable.put(JDONodeType.class.getName(),			TABLE_NODE_TYPE);
		mapClassToTable.put(JDOReference.class.getName(),			TABLE_REFERENCE);
		mapClassToTable.put(JDOAnnotationType.class.getName(),		TABLE_ANNOTATION_TYPE);
		mapClassToTable.put(JDOLongAnnotation.class.getName(),		TABLE_LONG_ANNOTATIONS);
		mapClassToTable.put(JDODoubleAnnotation.class.getName(),	TABLE_DOUBLE_ANNOTATIONS);
		mapClassToTable.put(JDODateAnnotation.class.getName(), 		TABLE_DATE_ANNOTATIONS);
		mapClassToTable.put(JDOStringAnnotation.class.getName(),	TABLE_STRING_ANNOTATIONS);
		// security
		mapClassToTable.put(JDOResourceAccess.class.getName(),		TABLE_RESOURCE_ACCESS);
		mapClassToTable.put(JDOUserGroup.class.getName(), 			TABLE_USER_GROUP);
		// Join tables
//		mapClassToTable.put(JDOResourceAccess.class.getName()+".accessType",	TABLE_RESOURCE_ACCESS_TYPE);
		mapClassToTable.put(JDOUserGroup.class.getName()+".users",				TABLE_USER_GROUP_USERS);
	}
	
	/**
	 * Get the table name for a class.
	 * @param clazz
	 * @return
	 */
	public static String getTableForClass(Class clazz){
		if(clazz == null) throw new IllegalArgumentException("Class cannot be null");
		return getTableForClassName(clazz.getName());
	}
	
	/**
	 * Get the table for a class name.
	 * @param className
	 * @return
	 */
	public static String getTableForClassName(String className){
		if(className == null) throw new IllegalArgumentException("Class cannot be null");
		String table = mapClassToTable.get(className);
		if(table == null) throw new IllegalArgumentException("Cannot find table for Class: "+className);
		return table;
	}
	/**
	 * Add all of the fields for a given object.
	 * @param clazz
	 * @param map
	 */
	private static void addAllFields(Class clazz, Map<String, String> map){
		// This class generates the names the same way as datanucleus.
		BasicIdentifierFactory factory = new BasicIdentifierFactory();
		Field[] fields = clazz.getDeclaredFields();
		for(int i=0; i<fields.length; i++){
			if(!fields[i].isAccessible()){
				fields[i].setAccessible(true);
			}
			String fieldName = fields[i].getName();
			map.put(fieldName, factory.generateIdentifierNameForJavaName(fieldName));
		}
	}
	
	/**
	 * Get the database column name for a given primary field name.
	 * @param field
	 * @return
	 */
	public static String getColumnNameForPrimaryField(String field){
		if(field == null) return null;
		String column = primaryFieldColumns.get(field);
		if(column == null) throw new IllegalArgumentException("Unknown field: "+field);
		return column;
	}
	
	
	
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
		}else if(Compartor.GREATER_THAN_OR_EQUALS == comp){
			return OPERATOR_SQL_GREATER_THAN_OR_EQUALS;
		}else if(Compartor.LESS_THAN_OR_EQUALS == comp){
			return OPERATOR_SQL_LESS_THAN_OR_EQUALS;
		}else{
			throw new IllegalArgumentException("Unsupported Compartor: "+comp);
		}
	}

}
