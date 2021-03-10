package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.IteratorUtils;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.migration.QueryStreamIterable;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.util.NestedMappingIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ImmutableMap;

public class TableFileHandleScanner implements FileHandleAssociationScanner {

	private static final long MAX_SCAN_ID_RANGE = 10000;
	private static final long MAX_TABLES_LIMIT = 10000;
	
	private static final String SQL_SELECT_MIN_MAX = "SELECT MIN(" + COL_TABLE_ROW_TABLE_ID + "), MAX(" + COL_TABLE_ROW_TABLE_ID+") FROM " + TABLE_ROW_CHANGE;
	
	private static final String SQL_SELECT_BATCH = "SELECT DISTINCT " + COL_TABLE_ROW_TABLE_ID + " FROM " + TABLE_ROW_CHANGE 
			+ " WHERE " + COL_TABLE_ROW_TABLE_ID + " BETWEEN :" + DMLUtils.BIND_MIN_ID + " AND :" + DMLUtils.BIND_MAX_ID
			+ " ORDER BY " + COL_TABLE_ROW_TABLE_ID; 
	
	private static final RowMapper<Long> TABLE_ID_MAPPER = (ResultSet rs, int rowNumber) -> rs.getLong(COL_TABLE_ROW_TABLE_ID);
	
	private TableEntityManager tableManager;
	
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	public TableFileHandleScanner(TableEntityManager tableManager, NamedParameterJdbcTemplate namedJdbcTemplate) {
		this.tableManager = tableManager;
		this.namedJdbcTemplate = namedJdbcTemplate;
	}
	
	@Override
	public long getMaxIdRangeSize() {
		return MAX_SCAN_ID_RANGE;
	}
	
	@Override
	public IdRange getIdRange() {
		return namedJdbcTemplate.getJdbcTemplate().queryForObject(SQL_SELECT_MIN_MAX, null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
	}

	@Override
	public Iterable<ScannedFileHandleAssociation> scanRange(IdRange range) {
		ValidateArgument.required(range, "The range");
		ValidateArgument.requirement(range.getMinId() <= range.getMaxId(), "Invalid range, the minId must be lesser or equal than the maxId");
		
		final Map<String, Object> params = ImmutableMap.of(DMLUtils.BIND_MIN_ID, range.getMinId(), DMLUtils.BIND_MAX_ID, range.getMaxId());
		
		// The iterator is used to stream over the tables in the range
		final Iterator<Long> tablesIterator = new QueryStreamIterable<>(namedJdbcTemplate, TABLE_ID_MAPPER, SQL_SELECT_BATCH, params, MAX_TABLES_LIMIT);
		
		// The iterator is wrapped into another iterator that for each table will extract the file handles
		final Iterator<ScannedFileHandleAssociation> tableRangeFilesIterator = new NestedMappingIterator<>(tablesIterator, this::getTableFileHandleIterator);
		
		return IteratorUtils.asIterable(tableRangeFilesIterator);
	}
	
	private Iterator<ScannedFileHandleAssociation> getTableFileHandleIterator(Long tableId) {
		return new TableFileHandleIterator(tableManager, tableId);
	}
	
}
