package org.sagebionetworks.repo.model.dbo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.migration.ChecksumTableResult;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.util.ValidateArgument;
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
	public static final String BIND_MIN_ID = "BMINID";
	public static final String BIND_MAX_ID = "BMAXID";
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
		main.append("INSERT ");
		// If a table consists only of primary keys, inserting a duplicate should not result in failure 
		if (!hasNonPrimaryKeyColumns(mapping)) {
			main.append("IGNORE ");
		}
		main.append("INTO ");
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
		if (hasNonPrimaryKeyColumns(mapping)) {
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
	public static String createGetCountByPrimaryKeyStatement(TableMapping mapping) {
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
	public static String createGetMaxByBackupKeyStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT MAX("+getBackupFieldColumnName(mapping)+") FROM ");
		main.append(mapping.getTableName());
		return main.toString();		
	}

	/**
	 * Create a MAX statement for a given mapping
	 * @param mapping
	 * @return the MAX statement
	 */
	public static String createGetMinByBackupKeyStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT MIN("+getBackupFieldColumnName(mapping)+") FROM ");
		main.append(mapping.getTableName());
		return main.toString();		
	}
	
	public static String createGetMinMaxCountByKeyStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		String backupFieldName = getBackupFieldColumnName(mapping);
		String primaryFieldName = getPrimaryFieldColumnName(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT MIN(`" + backupFieldName + "`), MAX(`" + backupFieldName + "`), COUNT(`" + primaryFieldName + "`) FROM ");
		builder.append(mapping.getTableName());
		return builder.toString();
	}

	public static String getPrimaryFieldColumnName(TableMapping mapping) {
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isPrimaryKey()) return fc.getColumnName();
		}
		throw new IllegalArgumentException("Table "+mapping.getTableName()+" has no primary key.");
	}

	public static String getBackupFieldColumnName(TableMapping mapping) {
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isBackupId()) return fc.getColumnName();
		}
		throw new IllegalArgumentException("Table "+mapping.getTableName()+" has no backup key.");
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
	 * Create an DELETE statement for a given mapping.
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
	 * Delete rows with a range of backup IDs.
	 * @param mapping
	 * @return
	 */
	public static String createDeleteByBackupIdRange(TableMapping mapping) {
		ValidateArgument.required(mapping, "Mapping cannot be null");
		ValidateArgument.required(mapping.getFieldColumns(), "TableMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("DELETE FROM ");
		main.append(mapping.getTableName());
		main.append(" WHERE ");
		addBackupRange(main, mapping);
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
	 *	Build a 'select sum(crc32(concat(id, '@', 'NA'))), bit_xor(crc32(concat(id, '@', ifnull(etag, 'NULL')))) statement for given mapping
	 */
	public static String createSelectChecksumStatement(TableMapping mapping) {

		validateMigratableTableMapping(mapping);

		// batch by ranges of backup ids
		// build the statement
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT CONCAT(");
		builder.append(buildAggregateCrc32Call(mapping, "SUM"));
		builder.append(", '%', ");
		builder.append(buildAggregateCrc32Call(mapping, "BIT_XOR"));
		builder.append(") FROM ");
		builder.append(mapping.getTableName());
		buildWhereBackupIdInRange(mapping, builder);
		return builder.toString();
	}
	
	/**
	 * Builds the <aggregate>(crc32()) call
	 * @param mapping
	 * @return
	 */
	private static String buildAggregateCrc32Call(TableMapping mapping, String aggregate) {
		String concatCall = buildCallConcatIdAndEtagIfExists(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append(aggregate);
		builder.append("(CRC32(");
		builder.append(concatCall);
		builder.append("))");
		return builder.toString();
	}
	
	/**
	 * Builds the concat() call
	 */
	private static String buildCallConcatIdAndEtagIfExists(TableMapping mapping) {
		FieldColumn etagCol = getEtagColumn(mapping);
		FieldColumn idCol = getBackupIdColumnName(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("CONCAT(`");
		builder.append(idCol.getColumnName());
		builder.append("`");
		if (etagCol != null) {
			builder.append(", '@', ");
			builder.append("IFNULL(`");
			builder.append(etagCol.getColumnName());
			builder.append("`, 'NULL')");
		}
		// Append salt parameter
		builder.append(", '@@', ?");
		builder.append(")");
		return builder.toString();
		
	}
	
	private static void buildWhereBackupIdInRange(TableMapping mapping, StringBuilder builder) {
		String idColName = getBackupIdColumnName(mapping).getColumnName();
		builder.append(" WHERE `");
		builder.append(idColName);
		builder.append("` >= ? AND `");
		builder.append(idColName);
		builder.append("` <= ?");
		return;
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
	 * 'ID' >= :BIND_MIN_ID AND 'ID' < BIND_MAX_ID
	 * @param builder
	 * @param mapping
	 */
	private static void addBackupRange(StringBuilder builder, TableMapping mapping) {
		builder.append("`");
		builder.append(getBackupIdColumnName(mapping).getColumnName());
		builder.append("`");
		builder.append(" >= :");
		builder.append(BIND_MIN_ID);
		builder.append(" AND `");
		builder.append(getBackupIdColumnName(mapping).getColumnName());
		builder.append("`");
		builder.append(" < :");
		builder.append(BIND_MAX_ID);
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
		builder.append(" LIMIT ? OFFSET ?");
		return builder.toString();
	}

	public static String listRowMetadataByRange(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		buildSelectIdAndEtag(mapping, builder);
		builder.append(" FROM ");
		builder.append(mapping.getTableName());
		buildWhereBackupIdInRange(mapping, builder);
		buildBackupOrderBy(mapping, builder, true);
		builder.append(" LIMIT ? OFFSET ?");
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
		return builder.toString();
	}
	

	/**
	 * SQL to list all of the data for a range of IDs.
	 * 
	 * @param mapping
	 * @return
	 */
	public static String getBackupRangeBatch(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT * FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE ");
		addBackupRange(builder, mapping);
		return builder.toString();
	}

	/**
	 * This query will list all unique indices on the column
	 * marked as 'BackupId'.  If there is not at least one
	 * uniqueness constraint (either primary key or unique)
	 * then the column CANNOT be a backup column.
	 * See: PLFM-2512.
	 * @param mapping
	 * @return
	 */
	public static String getBackupUniqueValidation(TableMapping mapping){
		validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SHOW INDEXES FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE Column_name='");
		builder.append(getBackupIdColumnName(mapping).getColumnName());
		builder.append("' AND NOT Non_unique");
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
	
	public static String createChecksumTableStatement(TableMapping mapping) {
		validateMigratableTableMapping(mapping);
		String tableName = mapping.getTableName();
		String stmt = "CHECKSUM TABLE " + tableName;
		return stmt;
	}
	
	public static RowMapper<ChecksumTableResult> getChecksumTableResultMapper() {
		return new RowMapper<ChecksumTableResult>() {
			@Override
			public ChecksumTableResult mapRow(ResultSet rs, int rowNum) throws SQLException {
				ChecksumTableResult ctRes = new ChecksumTableResult();
				ctRes.setTableName(rs.getString(1));
				ctRes.setChecksum(String.valueOf(rs.getLong(2)));
				return ctRes;
			}
		};
	}
	
	/**
	 * Row mapper for select min(id), max(id), count(*) from table
	 * If empty (count == 0) set the minId and maxId to null
	 * @return
	 */
	public static RowMapper<MigrationTypeCount> getMigrationTypeCountResultMapper() {
		return new RowMapper<MigrationTypeCount>() {
			@Override
			public MigrationTypeCount mapRow(ResultSet rs, int rowNum) throws SQLException {
				MigrationTypeCount mtc = new MigrationTypeCount();
				mtc.setCount(rs.getLong(3));
				if (mtc.getCount() == 0L) {
					mtc.setMinid(null);
					mtc.setMaxid(null);
				} else {
					mtc.setMinid(rs.getLong(1));
					mtc.setMaxid(rs.getLong(2));
				}
				return mtc;
			}
		};
	}

	/**
	 * Create the SQL to list primary IDs with all associated secondary cardinality.
	 * @param primaryMapping
	 * @param secondaryMappings
	 * @return
	 */
	public static String createPrimaryCardinalitySql(TableMapping primaryMapping, List<TableMapping> secondaryMappings) {
		StringBuilder builder = new StringBuilder();
		String primaryBackupColumnName = getBackupIdColumnName(primaryMapping).getColumnName();
		// Select
		builder.append("SELECT P0.");
		builder.append(primaryBackupColumnName);
		builder.append(", 1 ");
		int index = 0;
		for(TableMapping secondary: secondaryMappings) {
			builder.append(" + T").append(index).append(".");
			builder.append("CARD");
			index++;
		}
		builder.append(" AS CARD");
		// from
		builder.append(" FROM ").append(primaryMapping.getTableName()).append(" AS P0");
		// Join sub-query for each secondary type
		index = 0;
		for(TableMapping secondary: secondaryMappings) {
			builder.append(" JOIN (");
			builder.append(createCardinalitySubQueryForSecondary(primaryMapping, secondary));
			builder.append(") T").append(index);
			builder.append(" ON (P0.").append(primaryBackupColumnName);
			builder.append(" = ");
			builder.append("T").append(index).append(".").append(primaryBackupColumnName);
			builder.append(")");
			index++;
		}
		// where
		builder.append(" WHERE");
		builder.append(" P0.");
		builder.append(getBackupIdColumnName(primaryMapping).getColumnName());
		builder.append(" >= :").append(BIND_MIN_ID).append(" AND P0.");
		builder.append(getBackupIdColumnName(primaryMapping).getColumnName());
		builder.append(" < :").append(BIND_MAX_ID);
		builder.append(" ORDER BY P0.").append(primaryBackupColumnName).append(" ASC");
		return builder.toString();
	}
	
	/**
	 * Create a cardinality sub-query for a secondary type.
	 * @param primaryMapping
	 * @param secondaryMapping
	 * @return
	 */
	public static String createCardinalitySubQueryForSecondary(TableMapping primaryMapping, TableMapping secondaryMapping) {
		StringBuilder builder = new StringBuilder();
		String primaryBackupIdColumnName = getBackupIdColumnName(primaryMapping).getColumnName();
		String secondaryBackupIdColumnName = getBackupIdColumnName(secondaryMapping).getColumnName();
		// Select
		builder.append("SELECT P.");
		builder.append(primaryBackupIdColumnName);
		builder.append(",");
		builder.append(" + COUNT(S.");
		builder.append(secondaryBackupIdColumnName);
		builder.append(")");
		builder.append(" AS CARD");
		// from
		builder.append(" FROM ").append(primaryMapping.getTableName()).append(" AS P");
		// Join
		builder.append(" LEFT JOIN ").append(secondaryMapping.getTableName()).append(" AS S");
		builder.append(" ON (");
		builder.append("P.").append(primaryBackupIdColumnName);
		builder.append(" = ");
		builder.append(" S.").append(secondaryBackupIdColumnName);
		builder.append(")");
		// where
		builder.append(" WHERE");
		builder.append(" P.");
		builder.append(primaryBackupIdColumnName);
		builder.append(" >= :").append(BIND_MIN_ID).append(" AND P.");
		builder.append(primaryBackupIdColumnName);
		builder.append(" < :").append(BIND_MAX_ID);
		builder.append(" GROUP BY P.");
		builder.append(primaryBackupIdColumnName);
		return builder.toString();
	}

}
