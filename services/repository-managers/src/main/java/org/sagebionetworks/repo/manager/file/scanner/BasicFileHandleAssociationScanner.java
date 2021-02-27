package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.QueryStreamIterable;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ImmutableMap;

/**
 * Basic implementation of a file handle association scanner that automatically generates the needed
 * queries off of a {@link TableMapping}. This implementation can be used when the mapping table
 * contains the file handle id as a defined column and a backup id exists on the mapping. By default
 * a simple row mapper is used that considers the selected column a holder to the file handle id.
 * Additionally providing a custom {@link RowMapperSupplier} allows more flexibility in extracting the file
 * handle ids from the selected file handle column (e.g. if the file handle is stored inside a blob in the column).
 * 
 * @author Marco Marasca
 *
 */
public class BasicFileHandleAssociationScanner implements FileHandleAssociationScanner {

	public static final String DEFAULT_FILE_ID_COLUMN_NAME = "FILE_HANDLE_ID";
	
	public static final long DEFAULT_BATCH_SIZE = 10000;
	
	public static final RowMapper<IdRange> ID_RANGE_MAPPER = (ResultSet rs, int rowNumber) -> {
		long minId = rs.getLong(1);

		if (rs.wasNull()) {
			minId = -1;
		}

		long maxId = rs.getLong(2);

		if (rs.wasNull()) {
			maxId = -1;
		}

		return new IdRange(minId, maxId);
	};

	private NamedParameterJdbcTemplate namedJdbcTemplate;
	private long batchSize;
	private RowMapper<ScannedFileHandleAssociation> rowMapper;

	private FieldColumn backupIdColumn;
	private FieldColumn fileHandleColumn;

	// Cached statement for the min max range query
	private String sqlMinMaxRangeStm;
	// Cached statement for the batch select query
	private String sqlSelectBatchStm;

	/**
	 * Construct a scanner that uses a default FILE_HANDLE_ID column to extract a single file handle id
	 * from each scanned row
	 * 
	 * @param namedJdbcTemplate The jdbc template to use
	 * @param tableMapping      The table mapping used as reference
	 */
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping) {
		this(namedJdbcTemplate, tableMapping, DEFAULT_FILE_ID_COLUMN_NAME, DEFAULT_BATCH_SIZE, null);
	}
	
	/**
	 * Construct a scanner that uses the given fileHandleColumnName to extract a single file handle id from each
	 * scanned row
	 * 
	 * @param namedJdbcTemplate The jdbc template to use
	 * @param tableMapping The table mapping used as reference
	 * @param fileHandleColumnName The name of the column holding the file handle id reference
	 */
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping, String fileHandleColumnName) {
		this(namedJdbcTemplate, tableMapping, fileHandleColumnName, DEFAULT_BATCH_SIZE, null);
	}

	/**
	 * Construct a scanner that uses the given file handle column name reference as the holder of
	 * potential file handles. The name of the column must be defined in the given table mapping. The
	 * {@link ScannedFileHandleAssociation} is built using the given row mapper. If the row mapper is
	 * null a default row mapper is built that treats the given file handle column as a single file
	 * handle id reference.
	 * 
	 * 
	 * @param namedJdbcTemplate    The jdbc template to use
	 * @param tableMapping         The table mapping used as reference
	 * @param fileHandleColumnName The name of the column holding the file handle id reference
	 * @param batchSize            The max batch size of records to fetch in memory
	 * @param rowMapperSupplier    A supplier for a row mapper used to build a {@link ScannedFileHandleAssociation}
	 *                             from a row, if null a default row mapper is used that treats the
	 *                             filehandleColumn as a direct reference to the file handle id
	 */
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping, String fileHandleColumnName, long batchSize, RowMapperSupplier rowMapperSupplier) {
		ValidateArgument.required(namedJdbcTemplate, "The namedJdbcTemplate");
		ValidateArgument.requirement(batchSize > 0, "The batchSize must be greater than zero.");
		ValidateArgument.required(tableMapping, "The tableMapping");
		ValidateArgument.required(fileHandleColumnName, "The fileHandleColumnName");
		
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.batchSize = batchSize;
		
		// Makes sure the file handle column is present in the mapping
		this.fileHandleColumn = Arrays.stream(tableMapping.getFieldColumns())
				.filter(f -> f.getColumnName().equals(fileHandleColumnName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The column " + fileHandleColumnName + " is not defined for mapping " + tableMapping.getClass().getName()));
		
		// Makes sure that the mapping is defined with a backup id
		this.backupIdColumn = Arrays.stream(tableMapping.getFieldColumns())
				.filter(FieldColumn::isBackupId)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The mapping " + tableMapping.getClass().getName() + " does not define a backup id column"));

		this.sqlMinMaxRangeStm = generateMinMaxStatement(tableMapping);
		this.sqlSelectBatchStm = generateSelectBatchStatement(tableMapping, backupIdColumn, fileHandleColumn);

		if (rowMapperSupplier == null) {
			this.rowMapper = getDefaultRowMapper(backupIdColumn, fileHandleColumn);
		} else {
			this.rowMapper = rowMapperSupplier.getRowMapper(backupIdColumn.getColumnName(), fileHandleColumn.getColumnName());
		}
	}

	@Override
	public IdRange getIdRange() {
		// Using a null as the mid parameter allows to create a prepared statement
		return namedJdbcTemplate.getJdbcTemplate().queryForObject(sqlMinMaxRangeStm, null, ID_RANGE_MAPPER);
	}

	@Override
	public Iterable<ScannedFileHandleAssociation> scanRange(IdRange range) {
		ValidateArgument.required(range, "The range");
		ValidateArgument.requirement(range.getMinId() <= range.getMaxId(), "Invalid range, the minId must be lesser or equal than the maxId");

		final Map<String, Object> params = ImmutableMap.of(DMLUtils.BIND_MIN_ID, range.getMinId(), DMLUtils.BIND_MAX_ID, range.getMaxId());

		return new QueryStreamIterable<>(namedJdbcTemplate, rowMapper, sqlSelectBatchStm, params, batchSize);
	}

	/**
	 * Construct a row mapper that extracts a single file handle id from the given column
	 * 
	 * @param backupIdColumn   The {@link FieldColumn} holding the backup id
	 * @param fileHandleColumn The {@link FieldColumn} holding the file handle id
	 * @return A row mapper that builds a {@link ScannedFileHandleAssociation} with a single file handle
	 *         extracted from the given fileHandleIdColumn
	 */
	private static RowMapper<ScannedFileHandleAssociation> getDefaultRowMapper(FieldColumn backupIdColumn, FieldColumn fileHandleColumn) {
		return (ResultSet rs, int rowNumber) -> {

			final String objectId = rs.getString(backupIdColumn.getColumnName());

			ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId);
			
			Long fileHandleId = rs.getLong(fileHandleColumn.getColumnName());
			
			if (rs.wasNull()) {
				scanned.withFileHandleIds(Collections.emptyList());
			} else {
				scanned.withFileHandleIds(Collections.singletonList(fileHandleId));
			}

			return scanned;
		};
	}

	/**
	 * Generates the SQL statement to select the min and max backup id from the given mapping
	 * </p>
	 * <code>SELECT MIN(ID), MAX(ID) FROM TABLE</code>
	 */
	private static String generateMinMaxStatement(TableMapping<?> mapping) {
		return DMLUtils.createGetMinMaxByBackupKeyStatement(mapping);
	}

	/**
	 * Generates the SQL statement to select the (distinct) backup id and file handle id columns in a
	 * range of backup ids using the given mapping
	 * </p>
	 * <code>SELECT DISTINCT ID, FILE_HANLDE_ID FROM TABLE WHERE ID BETWEEN :MIN AND :MAX AND FILE_HANDLE_ID IS NOT NULL ORDER BY ID, OTHER_PK_ID</code>
	 */
	private static String generateSelectBatchStatement(TableMapping<?> mapping, FieldColumn backupIdColumn, FieldColumn fileHandleColumn) {
		DMLUtils.validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT `");
		builder.append(backupIdColumn.getColumnName());
		builder.append("`, `");
		builder.append(fileHandleColumn.getColumnName());
		builder.append("`");
		builder.append(" FROM ");
		builder.append(mapping.getTableName());
		builder.append(" WHERE `");
		builder.append(backupIdColumn.getColumnName());
		builder.append("` BETWEEN :");
		builder.append(DMLUtils.BIND_MIN_ID);
		builder.append(" AND :");
		builder.append(DMLUtils.BIND_MAX_ID);
		builder.append(" AND ");
		builder.append(fileHandleColumn.getColumnName());
		builder.append(" IS NOT NULL");
		builder.append(" ORDER BY ");
		builder.append(Arrays.stream(mapping.getFieldColumns()).filter(c -> c.isPrimaryKey()).map(FieldColumn::getColumnName)
				.collect(Collectors.joining("`, `", "`", "`")));

		return builder.toString();
	}

}
