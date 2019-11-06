package org.sagebionetworks.repo.model.query;

import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_DOUBLE;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_LONG;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_STRING;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_ANNO_BLOB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class QueryTools {
	
	public static final String FROM_TYPE_ID_DELIMTER = "_";
	private static final int MAX_LIMIT = 50000000; // MySQL's upper bound on LIMIT
	
	/**
	 * Construct a limit-offset SQL clause.
	 */
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
	 * Get the Synapse ID of the owner parent object on which we the query should be run.
	 */
	protected static String getQueryObjectId(BasicQuery basicQuery) {
		String[] from = parseFrom(basicQuery);
		return from[1];
	}

	/**
	 * Determine what type of Object we should query for.
	 */
	protected static QueryObjectType getQueryObjectType(BasicQuery basicQuery) {
		String[] from = parseFrom(basicQuery);
		String type = from[0];
		try {
			return QueryObjectType.valueOf(type.toUpperCase());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Unknown object type: " + type);
		}
	}

	/**
	 * Parse a 'FROM' String. Expected format is 'project_syn123'.
	 */
	private static String[] parseFrom(BasicQuery basicQuery) {
		if (basicQuery == null || basicQuery.getFrom() == null) {
			throw new IllegalArgumentException("The query and its 'from' clause cannot be null");
		}
		String[] from = basicQuery.getFrom().split(FROM_TYPE_ID_DELIMTER);
		if (from.length < 2) {
			throw new IllegalArgumentException("Failed to parse 'from' clause: [" + 
					basicQuery.getFrom() + "] Expected format is {objectType}" + 
					FROM_TYPE_ID_DELIMTER + "{objectId}");
		}
		return from;
	}

	/**
	 * Determine the type of a passed value.
	 */
	protected static FieldType getFieldType(Object value) {
		if (value instanceof Long || value instanceof Integer) {
			return FieldType.LONG_ATTRIBUTE;
		} else if (value instanceof Double) {
			return FieldType.DOUBLE_ATTRIBUTE;
		} else if (value instanceof String) {
			return FieldType.STRING_ATTRIBUTE;
		} else if (value == null) {
			return FieldType.DOES_NOT_EXIST;
		}
		throw new IllegalArgumentException("Unknown type for value: " + value.toString());
	}

	/**
	 * Determine the typed Annotation table for a given FieldType.
	 */
	protected static String getTableNameForFieldType(FieldType type) {
		switch (type) {
		case DOUBLE_ATTRIBUTE:
			return ANNO_DOUBLE;
		case LONG_ATTRIBUTE:
			return ANNO_LONG;
		default:
			return ANNO_STRING;
		}
	}

	/**
	 * Convert DB results to API-level results. Also perform authorization filtering on the fly. 
	 * @throws JSONObjectAdapterException 
	 */
	protected static QueryTableResults translateResults(List<Map<String, Object>> results,
			long count,	List<String> userSelect, boolean includePrivate) throws JSONObjectAdapterException {
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		List<Row> rows = new ArrayList<Row>();
		
		// Prepare headers. If the user query has projection, use the provided SELECT statement.
		// If not, we must collect all attributes for all returnd Annotations objects.
		Set<String> selectSet = null;
		if (userSelect == null) {
			selectSet = new LinkedHashSet<String>();
		}
		
		// Assemble results
		for (Map<String, Object> res : results) {
			// Fetch the Annotations blob
			byte[] annoBlob = (byte[]) res.remove(COL_ANNO_BLOB);
			
			if (annoBlob != null) {
				// Deserialize
				String json = new String(annoBlob);
				Annotations annos = new Annotations(joa.createNew(json));
				
				// Flatten
				Map<String, Object> annoMap = annosToMap(includePrivate, annos);

				// Insert OwnerId
				annoMap.put(DBOConstants.PARAM_ANNOTATION_OBJECT_ID, annos.getObjectId());
				
				// For 'SELECT *' add all attributes
				if (selectSet != null) {
					selectSet.addAll(annoMap.keySet());
				}				

				// Collect values for all attributes
				List<String> values = new ArrayList<String>();
				Collection<String> currentHeaders = userSelect != null ? userSelect : selectSet;
				for (String key : currentHeaders) {
					Object value = annoMap.get(key);
					values.add(value == null ? null : value.toString());
				}				
				
				// Create a new row for this object
				Row row = new Row();
				row.setValues(values);
				rows.add(row);
			}
		}

		List<String> headers = userSelect != null ? userSelect : new ArrayList<String>(selectSet);
		
		// Normalize row length
		for (Row row : rows) {
			List<String> values = row.getValues();
			while (values.size() < headers.size()) {
				values.add(null);
			}
		}
		
		// Return the results.
		QueryTableResults table = new QueryTableResults();
		table.setHeaders(headers);
		table.setRows(rows);
		table.setTotalNumberOfResults(count);
		return table;
	}

	public static Map<String, Object> annosToMap(boolean includePrivate, Annotations annos) {
		Map<String, Object> annoMap = new HashMap<String, Object>();
		List<StringAnnotation> stringAnnos = annos.getStringAnnos();
		for (StringAnnotation sa : stringAnnos) {
			if ((sa.getIsPrivate()!=null && !sa.getIsPrivate()) || includePrivate) {
				annoMap.put(sa.getKey(), sa.getValue());
			}
		}
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			if ((la.getIsPrivate()!=null && !la.getIsPrivate()) || includePrivate) {
				annoMap.put(la.getKey(), la.getValue());
			}
		}
		List<DoubleAnnotation> doubleAnnos = annos.getDoubleAnnos();
		for (DoubleAnnotation da : doubleAnnos) {
			if ((da.getIsPrivate()!=null && !da.getIsPrivate()) || includePrivate) {
				annoMap.put(da.getKey(), da.getValue());
			}
		}
		return annoMap;
	}
	
}