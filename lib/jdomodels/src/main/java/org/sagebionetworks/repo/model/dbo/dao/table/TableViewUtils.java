package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields.currentVersion;
import static org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields.dataFileHandleId;
import static org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields.id;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Static utilities for FileViews.
 *
 */
public class TableViewUtils {
	
	private static final String IDS_PARAM_NAME = "ids_param";

	/**
	 * Create the SQL need to build a view using the given column models.
	 * 
	 * @param schema
	 * @return
	 */
	public static String createSQLForSchema(List<ColumnModel> schema, ViewType type) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.requirement(!schema.isEmpty(), "schema cannot be empty");
		StringBuilder builder = new StringBuilder();
		Set<FileEntityFields> added = new HashSet<FileEntityFields>(2);
		// Always select id and current version
		builder.append("SELECT ");
		// add id
		boolean isFirst = true;
		appendColumn(builder, id, isFirst);
		added.add(id);
		// add current version
		isFirst = false;
		appendColumn(builder, currentVersion, isFirst);
		added.add(currentVersion);
		// Used to determine if a join to JDOREVISION is need.
		boolean joinRevision = false;
		String nodeAlias = id.getTableAlias();
		String revAlias = dataFileHandleId.getTableAlias();
		// Add all primary columns
		List<FileEntityFields> primaryColumns = TableViewUtils.getFileEntityFields(schema);
		for(FileEntityFields field: primaryColumns){
			if(!added.contains(field)){
				isFirst = false;
				appendColumn(builder, field, isFirst);
				added.add(field);
				if(TABLE_REVISION.equals(field.getDatabaseTableName())){
					joinRevision = true;
				}
			}
		}
		// Add annotations if needed
		List<ColumnModel> annotations = TableViewUtils.getNonFileEntityFieldColumns(schema);
		if(!annotations.isEmpty()){
			joinRevision = true;
			builder.append(", ").append(revAlias).append(".").append(COL_REVISION_ANNOS_BLOB);
		}
	
		builder.append(" FROM ").append(id.getDatabaseTableName()).append(" ").append(id.getTableAlias());
		// extend from if joining with JDOREVISION
		if(joinRevision){
			builder.append(", ").append(dataFileHandleId.getDatabaseTableName()).append(" ").append(revAlias);	
		}
		// Filter files within the container scope.
		builder.append(" WHERE ");
		builder.append(nodeAlias).append(".").append(COL_NODE_TYPE).append(" = '").append(type.name()).append("'");
		builder.append(" AND ");
		builder.append(nodeAlias).append(".").append(COL_NODE_PARENT_ID).append(" IN (:").append(IDS_PARAM_NAME).append(")");
		// complete join with JDOREVISION if needed.
		if(joinRevision){
			builder.append(" AND ");
			builder.append(nodeAlias).append(".").append(COL_NODE_ID);
			builder.append(" = ");
			builder.append(revAlias).append(".").append(COL_REVISION_OWNER_NODE);
			builder.append(" AND ");
			builder.append(nodeAlias).append(".").append(COL_CURRENT_REV);
			builder.append(" = ");
			builder.append(revAlias).append(".").append(COL_REVISION_NUMBER);
		}
		return builder.toString();
	}
	
	/**
	 * Append a column to the current select statement.
	 * 
	 * @param builder
	 * @param field
	 * @param isFirst
	 */
	private static void appendColumn(StringBuilder builder, FileEntityFields field, boolean isFirst){
		if(!isFirst){
			builder.append(", ");
		}
		builder.append(field.getTableAlias()).append(".").append(field.getDatabaseColumnName()).append(" as '").append(field.name()).append("'");
	}
	
	/**
	 * Get the sub-set of FileEntityFields that match the given ColumnModels.
	 * If a given ColumnModel does not match a FileEntityField, then a result will 
	 * not be returned for that ColumnModel.
	 * 
	 * This is used to identity "primary" columns.
	 * 
	 * @param models
	 * @return
	 */
	public static List<FileEntityFields> getFileEntityFields(List<ColumnModel> models){
		ValidateArgument.required(models, "models");
		List<FileEntityFields> results = new LinkedList<FileEntityFields>();
		for(ColumnModel cm: models){
			try{
				results.add(FileEntityFields.valueOf(cm.getName()));
			}catch (IllegalArgumentException e){
				// thrown when match cannot be found.
			}
		}
		return results;
	}
	
	/**
	 * Get the sub-set of ColumnModes that do not match FileEntityFields.
	 * If a given ColumnModel matches a FileEntityField, then a result
	 * will not be returned for that ColumnModel.
	 * 
	 * This is used to identity "annotation" Columns.
	 * @param models
	 * @return
	 */
	public static List<ColumnModel> getNonFileEntityFieldColumns(List<ColumnModel> models){
		ValidateArgument.required(models, "models");
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(ColumnModel cm: models){
			try{
				FileEntityFields.valueOf(cm.getName());
			}catch (IllegalArgumentException e){
				results.add(cm);
			}
		}
		return results;
	}
	
	/**
	 * Extract the given annotations from the raw byes.
	 * 
	 * @param annotationKeys
	 * @param rawBytes
	 * @return
	 */
	public static Map<String, String> extractAnnotations(List<ColumnModel> annotationColumns, byte[] rawBytes){
		try {
			NamedAnnotations annos = JDOSecondaryPropertyUtils.decompressedAnnotations(rawBytes);
			return extractAnnotations(annotationColumns, annos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extract the given annotations from the NameedAnnotations.
	 * 
	 * @param annotationKeys
	 * @param annos
	 * @return
	 */
	public static Map<String, String> extractAnnotations(
			List<ColumnModel> annotationColumns, NamedAnnotations annos) {
		Map<String, String> results = new HashMap<String, String>();
		if (annos.getAdditionalAnnotations() != null) {
			// Seek each value
			for (ColumnModel column : annotationColumns) {
				// Lookup the value
				String valueString = null;
				switch (column.getColumnType()) {
				case STRING:
					List<String> stringList = annos.getAdditionalAnnotations()
							.getStringAnnotations().get(column.getName());
					if (stringList != null && !stringList.isEmpty()) {
						valueString = stringList.get(0);
					}
					break;
				case DATE:
					List<Date> dateList = annos.getAdditionalAnnotations()
							.getDateAnnotations().get(column.getName());
					if (dateList != null && !dateList.isEmpty()) {
						Date date = dateList.get(0);
						if (date != null) {
							valueString = Long.toString(date.getTime());
						}
					}
					break;
				case DOUBLE:
					List<Double> doubleList = annos.getAdditionalAnnotations()
							.getDoubleAnnotations().get(column.getName());
					if (doubleList != null && !doubleList.isEmpty()) {
						Double dValue = doubleList.get(0);
						if (dValue != null) {
							valueString = dValue.toString();
						}
					}
					break;
				case INTEGER:
					List<Long> longList = annos.getAdditionalAnnotations()
							.getLongAnnotations().get(column.getName());
					if (longList != null && !longList.isEmpty()) {
						Long lValue = longList.get(0);
						if (lValue != null) {
							valueString = lValue.toString();
						}
					}
					break;
				default:
					valueString = null;
					break;
				}
				results.put(column.getName(), valueString);
			}
		}
		return results;
	}
	
	/**
	 * Extract a Row from a RowSet given the schema and annotation columns.
	 * @param schema
	 * @param annotationNames
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static Row extractRow(List<ColumnModel> schema, List<ColumnModel> annotationColumns, ResultSet rs) throws SQLException{
		// Map to a Row
		Row row = new Row();
		// rowId and version are part of every query.
		row.setRowId(rs.getLong(FileEntityFields.id.name()));
		row.setVersionNumber(rs.getLong(FileEntityFields.currentVersion.name()));
		Map<String, String> annotationsMap = null;
		
		// Read the annotations blob if there are annotations
		if(!annotationColumns.isEmpty()){
			Blob annosBlob = rs.getBlob(COL_REVISION_ANNOS_BLOB);
			if(annosBlob != null){
				byte[] bytes = annosBlob.getBytes(1, (int) annosBlob.length());
				annotationsMap = TableViewUtils.extractAnnotations(annotationColumns, bytes);
			}
		}
		// Create the results
		List<String> values = new LinkedList<String>();
		for(ColumnModel column: schema){
			String value = null;
			if(annotationsMap != null && annotationsMap.containsKey(column.getName())){
				// annotation
				value = annotationsMap.get(column.getName());
			}else{
				// primary
				value = rs.getString(column.getName());
			}
			values.add(value);
		}
		row.setValues(values);
		return row;
	}

	/**
	 * Does the given schema contain the benefactor column?
	 * 
	 * @param currentSchema
	 * @return
	 */
	public static boolean containsBenefactor(List<ColumnModel> schema) {
		ValidateArgument.required(schema, "schema");
		for(ColumnModel cm: schema){
			if(FileEntityFields.benefactorId.name().equals(cm.getName())){
				return true;
			}
		}
		return false;
	}
}
