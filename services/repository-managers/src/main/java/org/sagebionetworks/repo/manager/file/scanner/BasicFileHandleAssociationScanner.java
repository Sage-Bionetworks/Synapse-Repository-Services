package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.QueryStreamIterable;
import org.sagebionetworks.repo.model.file.IdRange;
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
	 * Construct a scanner using the given {@link TableMapping} as reference. A {@link FieldColumn} with {@link FieldColumn#hasFileHandleRef()} must exist in the mapping and will be used
	 * to extract a single file handle id from each scanned row
	 * 
	 * @param namedJdbcTemplate The jdbc template to use
	 * @param tableMapping      The table mapping used as reference, must contain a {@link FieldColumn} with {@link FieldColumn#hasFileHandleRef()} true
	 */
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping) {
		this(namedJdbcTemplate, tableMapping, DEFAULT_BATCH_SIZE, BasicFileHandleAssociationScanner::getDefaultRowMapper);
	}

	/**
	 * Construct a scanner that uses the given {@link TableMapping} as reference and will use the {@link FieldColumn#isBackupId()} column together with the {@link FieldColumn#hasFileHandleRef()} column
	 * to process each row with the given {@link RowMapperSupplier}.
	 * 
	 * @param namedJdbcTemplate    The jdbc template to use
	 * @param tableMapping         The table mapping used as reference, must contain two {@link FieldColumn} with {@link FieldColumn#isBackupId()} and {@link FieldColumn#hasFileHandleRef()} set to true  
	 * @param batchSize            The max batch size of records to fetch in memory
	 * @param rowMapperSupplier    A supplier for a row mapper used to build a {@link ScannedFileHandleAssociation} from a row
	 */
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping, long batchSize, RowMapperSupplier rowMapperSupplier) {
		ValidateArgument.required(namedJdbcTemplate, "The namedJdbcTemplate");
		ValidateArgument.requirement(batchSize > 0, "The batchSize must be greater than zero.");
		ValidateArgument.required(tableMapping, "The tableMapping");
		ValidateArgument.required(rowMapperSupplier, "The rowMapperSupplier");
		
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.batchSize = batchSize;
		
		// Makes sure the file handle column is present in the mapping
		List<FieldColumn> candidates = Arrays.stream(tableMapping.getFieldColumns())
				.filter(f -> f.hasFileHandleRef()).collect(Collectors.toList());
		
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("No column found that is a fileHandleRef for mapping " + tableMapping.getClass().getName());
		}
		
		if (candidates.size() > 1) {
			throw new IllegalArgumentException("Only one fileHandleRef is currentlty supported, found " + candidates.size() + " for mapping " + tableMapping.getClass().getName());
		}
		// Makes sure that the mapping is defined with a backup id
		this.backupIdColumn = Arrays.stream(tableMapping.getFieldColumns())
				.filter(FieldColumn::isBackupId)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The mapping " + tableMapping.getClass().getName() + " does not define a backup id column"));
		this.fileHandleColumn = candidates.iterator().next();
		this.sqlMinMaxRangeStm = generateMinMaxStatement(tableMapping);
		this.sqlSelectBatchStm = generateSelectBatchStatement(tableMapping, backupIdColumn, fileHandleColumn);
		this.rowMapper = rowMapperSupplier.getRowMapper(backupIdColumn.getColumnName(), fileHandleColumn.getColumnName());
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
	 * @param backupIdColumnName   The {@link FieldColumn} holding the backup id
	 * @param fileHandleColumnName The {@link FieldColumn} holding the file handle id
	 * @return A row mapper that builds a {@link ScannedFileHandleAssociation} with a single file handle
	 *         extracted from the given fileHandleIdColumn
	 */
	public static RowMapper<ScannedFileHandleAssociation> getDefaultRowMapper(String backupIdColumnName, String fileHandleColumnName) {
		return (ResultSet rs, int rowNumber) -> {

			final Long objectId = rs.getLong(backupIdColumnName);

			ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId);
			
			Long fileHandleId = rs.getLong(fileHandleColumnName);
			
			if (rs.wasNull()) {
				scanned.withFileHandleIds(Collections.emptySet());
			} else {
				scanned.withFileHandleIds(Collections.singleton(fileHandleId));
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
