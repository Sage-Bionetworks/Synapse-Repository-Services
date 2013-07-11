package org.sagebionetworks.repo.model.query;

import static org.sagebionetworks.repo.model.query.SQLConstants.*;

import java.util.ArrayList;
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
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class QueryTools {
	
	public static final int MAX_LIMIT = 50000000; // MySQL's upper bound on LIMIT
	
	/**
	 * Construct a limit-offset SQL clause.
	 */
	protected static String buildPaging(long offset, long limit,
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
		for (Expression exp : basicQuery.getFilters()) {
			if (exp.getId().getFieldName().equalsIgnoreCase(DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID)) {
				return exp.getValue().toString();
			}
		}
		throw new IllegalArgumentException("Queries must specify an owner Object ID");
	}

	/**
	 * Determine what type of Object we should query for.
	 */
	protected static QueryObjectType getQueryObjectType(BasicQuery basicQuery) {
		String from = basicQuery.getFrom();
		try {
			return QueryObjectType.valueOf(from.toUpperCase());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Unknown object type: " + from);
		}
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
			long count,	List<String> select, boolean includePrivate) throws JSONObjectAdapterException {
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		Set<String> headers = new LinkedHashSet<String>();
		List<Row> rows = new ArrayList<Row>();
		
		// Prepare headers
		headers.add(DBOConstants.PARAM_ANNOTATION_OWNER_ID);
		if (select != null) {
			headers.addAll(select);
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
				annoMap.put(DBOConstants.PARAM_ANNOTATION_OWNER_ID, annos.getOwnerId());
				
				// If select is null (i.e. 'SELECT *'), add all attributes
				if (select == null) {
					headers.addAll(annoMap.keySet());
				}				

				// Collect values for all attributes
				List<String> values = new ArrayList<String>();
				for (String key : headers) {
					Object value = annoMap.get(key);
					values.add(value == null ? null : value.toString());
				}				
				
				// Create a new row for this object
				Row row = new Row();
				row.setValues(values);
				rows.add(row);
			}
		}
		
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

	private static Map<String, Object> annosToMap(boolean includePrivate, Annotations annos) {
		Map<String, Object> annoMap = new HashMap<String, Object>();
		List<StringAnnotation> stringAnnos = annos.getStringAnnos();
		for (StringAnnotation sa : stringAnnos) {
			if (!sa.getIsPrivate() || includePrivate) {
				annoMap.put(sa.getKey(), sa.getValue());
			}
		}
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			if (!la.getIsPrivate() || includePrivate) {
				annoMap.put(la.getKey(), la.getValue());
			}
		}
		List<DoubleAnnotation> doubleAnnos = annos.getDoubleAnnos();
		for (DoubleAnnotation da : doubleAnnos) {
			if (!da.getIsPrivate() || includePrivate) {
				annoMap.put(da.getKey(), da.getValue());
			}
		}
		return annoMap;
	}
	
}