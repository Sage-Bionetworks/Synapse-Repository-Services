package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Static utilities for FileViews.
 *
 */
public class FileViewUtils {
	
	private static final String IDS_PARAM_NAME = "ids_param";

	/**
	 * Create the SQL need to build a view using the given column models.
	 * 
	 * @param schema
	 * @return
	 */
	public static String createSQLForSchema(List<ColumnModel> schema) {
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
		// add the rest of the columns
		for(ColumnModel cm: schema){
			try {
				FileEntityFields field = FileEntityFields.valueOf(cm.getName());
				if(!added.contains(field)){
					isFirst = false;
					appendColumn(builder, field, isFirst);
					added.add(field);
					if(TABLE_REVISION.equals(field.getDatabaseTableName())){
						joinRevision = true;
					}
				}
			} catch (IllegalArgumentException e) {
				// This is an annotation column
				// annotation blob is selected form revision
				joinRevision = true;
				builder.append(", ").append(revAlias).append(".").append(COL_REVISION_ANNOS_BLOB);
			}
		}
	
		builder.append(" FROM ").append(id.getDatabaseTableName()).append(" ").append(id.getTableAlias());
		// extend from if joining with JDOREVISION
		if(joinRevision){
			builder.append(", ").append(dataFileHandleId.getDatabaseTableName()).append(" ").append(revAlias);	
		}
		// Filter files within the container scope.
		builder.append(" WHERE ");
		builder.append(nodeAlias).append(".").append(COL_NODE_TYPE).append(" = '").append(EntityType.file).append("'");
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
	


}
