package org.sagebionetworks.repo.manager.file.scanner;

import java.util.Arrays;
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
 * contains the file handle id as a defined column and a backup id exists on the mapping.
 * 
 * @author Marco Marasca
 *
 */
public class BasicFileHandleAssociationScanner implements FileHandleAssociationScanner {
	
	private static final String DEFAULT_FILE_ID_COLUMN_NAME = "FILE_HANDLE_ID";
	
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private FieldColumn backupIdColumn;
	private FieldColumn fileHandleIdColumn;
	
	private String sqlMinMaxRangeStm;
	private String sqlSelectBatchStm;
	
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping) {
		this(namedJdbcTemplate, tableMapping, DEFAULT_FILE_ID_COLUMN_NAME);
	}
	
	public BasicFileHandleAssociationScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableMapping<?> tableMapping, String fileHandleIdColumn) {
		this.namedJdbcTemplate = namedJdbcTemplate;
		initTableMapping(tableMapping, fileHandleIdColumn);
	}
	
	@Override
	public IdRange getIdRange() {
		// Using a null as the mid parameter allows to create a prepared statement
		return namedJdbcTemplate.getJdbcTemplate().queryForObject(sqlMinMaxRangeStm, null, (rs, i) -> {
			long minId = rs.getLong(1);
			
			if (rs.wasNull()) {
				minId = -1;
			}
			
			long maxId = rs.getLong(2);
			
			if (rs.wasNull()) {
				maxId = -1;
			}
			
			return new IdRange(minId, maxId);
		});
	}

	@Override
	public Iterable<ScannedFileHandle> scanRange(IdRange range, long batchSize) {
		ValidateArgument.required(range, "The range");
		ValidateArgument.requirement(range.getMinId() <= range.getMaxId(), "Invalid range, the minId must be lesser or equal than the maxId");
		ValidateArgument.requirement(batchSize > 0, "Invalid batchSize, must be greater than 0");
		
		final Map<String, Object> params = ImmutableMap.of(
				DMLUtils.BIND_MIN_ID, range.getMinId(),
				DMLUtils.BIND_MAX_ID, range.getMaxId()
		);

		final RowMapper<ScannedFileHandle> rowMapper = (rs, i) -> new ScannedFileHandle(rs.getString(backupIdColumn.getColumnName()), rs.getLong(fileHandleIdColumn.getColumnName()));
		
		return new QueryStreamIterable<>(namedJdbcTemplate, rowMapper, sqlSelectBatchStm, params, batchSize);
	}
	
	void initTableMapping(TableMapping<?> tableMapping, String fileHandleIdColumnName) {
		// Makes sure the file handle column is present in the mapping
		fileHandleIdColumn = Arrays.stream(tableMapping.getFieldColumns())
				.filter(f -> f.getColumnName().equals(fileHandleIdColumnName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The column " + fileHandleIdColumnName + " is not defined for mapping " + tableMapping.getClass().getName()));
		
		// Makes sure that the mapping is defined with a backup id
		backupIdColumn = Arrays.stream(tableMapping.getFieldColumns())
				.filter(FieldColumn::isBackupId)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The mapping " + tableMapping.getClass().getName() + " does not define a backup id column"));
		
		sqlMinMaxRangeStm = generateMinMaxStatement(tableMapping);
		sqlSelectBatchStm = generateSelectBatchStatement(tableMapping, backupIdColumn, fileHandleIdColumn);
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
	 * Generates the SQL statement to select the (distinct) backup id and file handle id columns in a range of backup ids using the given mapping
	 * </p>
	 * <code>SELECT DISTINCT ID, FILE_HANLDE_ID FROM TABLE WHERE ID BETWEEN :MIN AND :MAX AND FILE_HANDLE_ID IS NOT NULL ORDER BY ID, OTHER_PK_ID</code>
	 */
	private static String generateSelectBatchStatement(TableMapping<?> mapping, FieldColumn backupIdColumn, FieldColumn fileHandleIDColumn) {
		DMLUtils.validateMigratableTableMapping(mapping);
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT `");
		builder.append(backupIdColumn.getColumnName());
		builder.append("`, `");
		builder.append(fileHandleIDColumn.getColumnName());
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
		builder.append(fileHandleIDColumn.getColumnName());
		builder.append(" IS NOT NULL");
		builder.append(" ORDER BY ");
		builder.append(Arrays.stream(mapping.getFieldColumns())
				.filter(c -> c.isPrimaryKey())
				.map(FieldColumn::getColumnName)
				.collect(Collectors.joining("`, `", "`", "`"))
		);
		
		return builder.toString();
	}

}
