package org.sagebionetworks.repo.model.query.jdo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.entity.NodeToEntity;

public class QueryUtils {
	
	public static final int MAX_LIMIT = 50000000; // MySQL's upper bound on
	// LIMIT

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

	public static String buildPaging(long offset, long limit,
			Map<String, Object> parameters) {
		// We need to convert from offset and limit to "fromIncl" and "toExcl"
		if (offset < 0) {
			offset = 0;
		}
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT - 1;
		}
		String paging = "limit :limitVal offset :offsetVal";
		parameters.put("limitVal", limit);
		parameters.put("offsetVal", offset);
		return paging;
	}
	
	/**
	 * Create an empty result.
	 * 
	 * @return
	 */
	public static NodeQueryResults createEmptyResults(){
		NodeQueryResults results = new NodeQueryResults();
		results.setAllSelectedData(new LinkedList<Map<String,Object>>());
		results.setResultIds(new LinkedList<String>());
		results.setTotalNumberOfResults(0);
		return results;
	}

	/**
	 * Assemble query results into a ResourceQueryResults object.
	 * 
	 * @param fromDB
	 * @param select
	 * @return
	 * @throws DatastoreException 
	 */
	public static NodeQueryResults translateResults(List<Map<String, Object>> fromDB, long totalCount, List<String> select) throws DatastoreException{
		// First build up the list of ID
		List<String> idList = new ArrayList<String>();
		for(Map<String, Object> row: fromDB){
			// Remove the annotations from the map if there
			byte[] zippedAnnos = (byte[]) row.remove(SqlConstants.COL_REVISION_ANNOS_BLOB);
			// If select is null then add all
			if(zippedAnnos != null){
				try {
					NamedAnnotations named = AnnotationUtils.decompressedAnnotations(zippedAnnos);
					// Add the primary
					addNewToMap(row, named.getPrimaryAnnotations(), select);
					// Now add the secondary.
					addNewToMap(row, named.getAdditionalAnnotations(), select);
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			}else{
				// Convert convert annotation values
				convertAnnotationValuesToLists(row);
			}
			// Replace the ID with a string if needed
			Long idLong = (Long) row.remove(NodeField.ID.getFieldName());
			if(idLong != null){
				String id = KeyFactory.keyToString(idLong);
				row.put(NodeField.ID.getFieldName(), id);
				idList.add(id);
			}
			// Replace the parentID with a string if needed
			Long parentIdLong = (Long) row.get(NodeField.PARENT_ID.getFieldName());
			if(parentIdLong != null){
				String parentId = KeyFactory.keyToString(parentIdLong);
				row.put(NodeField.PARENT_ID.getFieldName(), parentId);
			}
			//			System.out.println(row);
		}
		// Return the results.
		return new NodeQueryResults(idList, fromDB, totalCount);
	}
	
	/**
	 * Convert any annotation values in the row to a list of the type.
	 * @param row
	 */
	public static void convertAnnotationValuesToLists(Map<String, Object> row){
		for(String key: row.keySet()){
			Object value = row.get(key);
			if(!NodeToEntity.isNodeField(key)){
				List list = new LinkedList<>();
				list.add(convertAnnotationString(value));
				row.put(key, list);
			}
		}
	}
	
	/**
	 * Convert a string annotation value to the original annotation type.
	 */
	public static Object convertAnnotationString(Object value){
		if(value == null){
			return null;
		}
		if(!(value instanceof String)){
			throw new IllegalArgumentException("Unknown value type: "+value.getClass().getName());
		}
		String stringValue = (String) value;
		try{
			return Long.parseLong(stringValue);
		}catch(IllegalArgumentException e){
			try{
				return Double.parseDouble(stringValue);
			}catch(IllegalArgumentException e1){
				return stringValue;
			}
		}
	}
	
	
	/**
	 * Try to determine the type from the value
	 * @param value
	 * @return
	 */
	public static FieldType determineTypeFromValue(Object value){
		if(value == null) return null;
		if(value instanceof Long){
			return FieldType.LONG_ATTRIBUTE;
		}
		if(value instanceof Double){
			return FieldType.DOUBLE_ATTRIBUTE;
		}
		if(value instanceof String){
			String string = (String) value;
			// Try to parse long
			try{
				Long.parseLong(string);
				return FieldType.LONG_ATTRIBUTE;
			}catch (NumberFormatException e) {
				// Not a long
				try{
					Double.parseDouble(string);
					return FieldType.DOUBLE_ATTRIBUTE;
				}catch(NumberFormatException e2){
					// Not a long or a double so it is a string
					return FieldType.STRING_ATTRIBUTE;
				}
			}
	
		}
		// All others are treated as strings
		return FieldType.STRING_ATTRIBUTE;
	}
	
	private static void addNewToMap(Map<String, Object> row, Annotations annotations, List<String> select) {
		if(annotations != null){
			addNewOnly(row, annotations.getStringAnnotations(), select);
			addNewOnly(row, annotations.getDateAnnotations(), select);
			addNewOnly(row, annotations.getLongAnnotations(), select);
			addNewOnly(row, annotations.getDoubleAnnotations(), select);
		}
	}
	
	/**
	 * Only add values that are not already in the map
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param toAdd
	 */
	protected static <K> void addNewOnly(Map<String, Object> map, Map<String, ? extends K> toAdd, List<String> select){
		if(toAdd != null){
			// If the select list is null then add all keys
			Iterator<String> keyIt = null;
			if(select == null){
				// Add all keys
				keyIt = toAdd.keySet().iterator();
			}else{
				// Only add keys from the list
				keyIt = select.iterator();
			}
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Object value = map.get(key);
				if(value == null){
					map.put(key, toAdd.get(key));
				}
			}
		}
	}

}
