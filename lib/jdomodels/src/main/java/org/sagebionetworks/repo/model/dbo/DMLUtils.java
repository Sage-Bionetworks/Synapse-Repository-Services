package org.sagebionetworks.repo.model.dbo;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.springframework.jdbc.core.RowMapper;

/**
 * Utility for generating Data Manipulation Language (DML) statements.
 *  
 * @author John
 *
 */
public class DMLUtils {

	/**
	 * The SQL bind variable for a list of IDs;
	 */
	public static final String BIND_VAR_ID_lIST = "BVIDLIST";
	public static final String BIND_VAR_OFFSET = "BVOFFSET";
	public static final String BIND_VAR_LIMIT = "BCLIMIT";

	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createInsertStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("DBOMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("INSERT INTO ");
		main.append(mapping.getTableName());

		// Build up the columns and values.
		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(i != 0){
				columns.append(", ");
				values.append(", ");
			}
			columns.append("`");
			columns.append(fc.getColumnName());
			columns.append("`");
			values.append(":");
			values.append(fc.getFieldName());
		}
		main.append("(");
		main.append(columns.toString());
		main.append(") VALUES (");
		main.append(values.toString());
		main.append(")");
		return main.toString();
	}
	
	/**
	 * A batch insert or update SQL.
	 * @return
	 */
	public static String getBatchInsertOrUdpate(TableMapping mapping){
		StringBuilder builder = new StringBuilder();
		builder.append(createInsertStatement(mapping));
		if(hasNonPrimaryKeyColumns(mapping)){
			builder.append(" ON DUPLICATE KEY UPDATE ");
			buildUpdateBody(mapping, builder);
		}
		return builder.toString();
	}
	
	/**
	 * Does this table have any columns that are not part of the primary key
	 * @param mapping
	 * @return
	 */
	private static boolean hasNonPrimaryKeyColumns(TableMapping mapping) {
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(!fc.isPrimaryKey()) return true;
		}
		return false;
	}

	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createGetByIDStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("DBOMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT * FROM ");
		main.append(mapping.getTableName());
		// Setup the primary key
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}
	
	/**
	 * Create a COUNT statement for a given mapping
	 * @param mapping
	 * @return the COUNT statement
	 */
	public static String createGetCountStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT COUNT("+getPrimaryFieldColumnName(mapping)+") FROM ");
		main.append(mapping.getTableName());
		return main.toString();		
	}
	
	/**
	 * Create a MAX statement for a given mapping
	 * @param mapping
	 * @return the MAX statement
	 */
	public static String createGetMaxStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT MAX("+getPrimaryFieldColumnName(mapping)+") FROM ");
		main.append(mapping.getTableName());
		return main.toString();		
	}

	public static String getPrimaryFieldColumnName(TableMapping mapping) {
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isPrimaryKey()) return fc.getColumnName();
		}
		throw new IllegalArgumentException("Table "+mapping.getTableName()+" has no primary key.");
	}

	/**
	 * Append the primary key
	 * @param mapping
	 * @param main
	 */
	public static void appendPrimaryKey(TableMapping mapping, StringBuilder main) {
		int keyCount = 0;
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isPrimaryKey()){
				if(keyCount > 0){
					main.append(" AND ");
				}
				main.append("`");
				main.append(fc.getColumnName());
				main.append("`");
				main.append(" = :");
				main.append(fc.getFieldName());
				keyCount++;
			}
		}
	}
	
	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createDeleteStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("TableMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("DELETE FROM ");
		main.append(mapping.getTableName());
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}
	
	/**
	 * Build the UPDATE sql for A given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createUpdateStatment(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("TableMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("UPDATE ");
		main.append(mapping.getTableName());
		main.append(" SET ");
		buildUpdateBody(mapping, main);
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}

	/**
	 * The main body of an update
	 * @param mapping
	 * @param builder
	 */
	private static void buildUpdateBody(TableMapping mapping, StringBuilder builder) {
		int count = 0;
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(!fc.isPrimaryKey()){
				if(count > 0){
					builder.append(", ");
				}
				builder.append("`");
				builder.append(fc.getColumnName());
				builder.append("`");
				builder.append(" = :");
				builder.append(fc.getFieldName());
				count++;
			}
		}
	}
	
	/**
	 * Build a batch Delete SQL statment for the given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createBatchDelete(TableMapping mapping){
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("DELETE FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE ");
		addBackupIdInList(builder, mapping);
		return builder.toString();
	}
	
	/**
	 * 'ID' IN ( :BVIDLIST )
	 * @param builder
	 * @param mapping
	 */
	private static void addBackupIdInList(StringBuilder builder, TableMapping mapping){
		// Find the backup id
		builder.append("`");
		builder.append(getBackupIdColumnName(mapping).getColumnName());
		builder.append("`");
		builder.append(" IN ( :"+BIND_VAR_ID_lIST+" )");
	}
	
	/**
	 * Find the backup column
	 * @param mapping
	 * @return
	 */
	public static FieldColumn getBackupIdColumnName(TableMapping mapping){
		for(FieldColumn column: mapping.getFieldColumns()){
			if(column.isBackupId()){
				return column;
			}
		}
		throw new IllegalArgumentException(mapping.getTableName()+" did not have a TableMapping.fieldColumn with is isBackupId = 'true' ");
	}
	
	/**
	 * Get the Etag column if there is one.
	 * @param mapping
	 * @return
	 */
	public static FieldColumn getEtagColumn(TableMapping mapping){
		for(FieldColumn column: mapping.getFieldColumns()){
			if(column.isEtag()){
				return column;
			}
		}
		return null;
	}
	
	/**
	 * Get the self foreign key column if there is one.
	 * @param mapping
	 * @return
	 */
	public static FieldColumn getSelfForeignKey(TableMapping mapping){
		for(FieldColumn column: mapping.getFieldColumns()){
			if(column.isSelfForeignKey()){
				return column;
			}
		}
		return null;
	}

	/**
	 * List all of the row data.
	 * @param mapping
	 * @return
	 */
	public static String listRowMetadata(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		buildSelectIdAndEtag(mapping, builder);
		builder.append(" FROM ");
		builder.append(mapping.getTableName());
		buildBackupOrderBy(mapping, builder, true);
		builder.append(" LIMIT :");
		builder.append(BIND_VAR_LIMIT);
		builder.append(" OFFSET :");
		builder.append(BIND_VAR_OFFSET);
		return builder.toString();
	}

	/**
	 * When etag is not null: " `ID`, `ETAG`", else: "`ID`"
	 * @param mapping
	 * @param builder
	 */
	private static void buildSelectIdAndEtag(TableMapping mapping,StringBuilder builder) {
		FieldColumn backupId = getBackupIdColumnName(mapping);
		builder.append("`");
		builder.append(backupId.getColumnName());
		builder.append("`");
		FieldColumn etagColumn = getEtagColumn(mapping);
		if(etagColumn != null){
			builder.append(", `");
			builder.append(etagColumn.getColumnName());
			builder.append("`");
		}
		FieldColumn selfKey = getSelfForeignKey(mapping);
		if(selfKey !=null){
			builder.append(", `");
			builder.append(selfKey.getColumnName());
			builder.append("`");
		}
	}
	
	/**
	 * List all of the row data.
	 * @param mapping
	 * @return
	 */
	public static String deltaListRowMetadata(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		buildSelectIdAndEtag(mapping, builder);
		builder.append(" FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE ");
		addBackupIdInList(builder, mapping);
		buildBackupOrderBy(mapping, builder, true);
		return builder.toString();
	}
	
	/**
	 * List all of the row data.
	 * @param mapping
	 * @return
	 */
	public static String getBackupBatch(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT * FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE ");
		addBackupIdInList(builder, mapping);
		buildBackupOrderBy(mapping, builder, true);
		return builder.toString();
	}
	
	/**
	 * build - "ORDER BY `BACKUP_ID` ASC/DESC"
	 * @param mapping
	 * @param builder
	 */
	private static void buildBackupOrderBy(TableMapping mapping, StringBuilder builder, boolean ascending) {
		builder.append(" ORDER BY `");
		FieldColumn backupId = getBackupIdColumnName(mapping);
		builder.append(backupId.getColumnName());
		builder.append("` ");
		if(ascending){
			builder.append("ASC");
		}else{
			builder.append("DESC");
		}
	}
	
	/**
	 * Validate the passed mapping meets the minimum requirements for a backup table mapping.
	 * 
	 * @param mapping
	 */
	public static void validateMigratableTableMapping(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getTableName() == null) throw new IllegalArgumentException("Mapping.tableName cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("TableMapping.fieldColumns() cannot be null");
		FieldColumn backupId = getBackupIdColumnName(mapping);
		if(backupId == null) throw new IllegalArgumentException("One column must be marked as the backupIdColumn");
	}
	
	/**
	 * Get the RowMapper for a given type.
	 * @param type
	 * @return
	 */
	public static RowMapper<RowMetadata> getRowMetadataRowMapper(TableMapping mapping){
		// There are two different row mappers, on with etags and one without.
		final FieldColumn etag = getEtagColumn(mapping);
		final FieldColumn id = getBackupIdColumnName(mapping);
		final FieldColumn selfKey = getSelfForeignKey(mapping);
		if(etag == null){
			if(selfKey == null){
				// Row mapper with a null etag
				return new RowMapper<RowMetadata>() {
					@Override
					public RowMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
						RowMetadata metadata = new RowMetadata();
						metadata.setId(rs.getLong(id.getColumnName()));
						metadata.setEtag(null);
						metadata.setParentId(null);
						return metadata;
					}
				};
			}else{
				// Row mapper with a null etag
				return new RowMapper<RowMetadata>() {
					@Override
					public RowMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
						RowMetadata metadata = new RowMetadata();
						metadata.setId(rs.getLong(id.getColumnName()));
						metadata.setEtag(null);
						metadata.setParentId(rs.getLong(selfKey.getColumnName()));
						if(rs.wasNull()){
							metadata.setParentId(null);
						}
						return metadata;
					}
				};
			}
		}else{
			if(selfKey == null){
				// Row mapper with an etag
				return new RowMapper<RowMetadata>() {
					@Override
					public RowMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
						RowMetadata metadata = new RowMetadata();
						metadata.setId(rs.getLong(id.getColumnName()));
						metadata.setEtag(rs.getString(etag.getColumnName()));
						metadata.setParentId(null);
						return metadata;
					}
				};
			}else{
				// Row mapper with an etag
				return new RowMapper<RowMetadata>() {
					@Override
					public RowMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
						RowMetadata metadata = new RowMetadata();
						metadata.setId(rs.getLong(id.getColumnName()));
						metadata.setEtag(rs.getString(etag.getColumnName()));
						metadata.setParentId(rs.getLong(selfKey.getColumnName()));
						if(rs.wasNull()){
							metadata.setParentId(null);
						}
						return metadata;
					}
				};
			}

		}
	}
	
}
