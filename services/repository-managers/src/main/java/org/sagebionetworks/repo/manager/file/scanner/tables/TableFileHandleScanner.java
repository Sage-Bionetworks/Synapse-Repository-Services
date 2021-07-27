package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_HAS_FILE_REFS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;

import java.util.Map;

import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.migration.QueryStreamIterable;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ImmutableMap;

public class TableFileHandleScanner implements FileHandleAssociationScanner {
	
	private static final long MAX_SCAN_ID_RANGE = 10_000;
	private static final long MAX_FETCH_LIMIT = 1000;
	
	private static final String SQL_SELECT_MIN_MAX = "SELECT MIN(" + COL_TABLE_ROW_ID + "), MAX(" + COL_TABLE_ROW_ID+") FROM " + TABLE_ROW_CHANGE;
	
	private static final String SQL_SELECT_BATCH = "SELECT * FROM " + TABLE_ROW_CHANGE 
			+ " WHERE " + COL_TABLE_ROW_ID + " BETWEEN :" + DMLUtils.BIND_MIN_ID + " AND :" + DMLUtils.BIND_MAX_ID
			+ " AND " + COL_TABLE_ROW_TYPE + "='" + TableChangeType.ROW.name() + "' AND (" + COL_TABLE_ROW_HAS_FILE_REFS + " IS NULL OR " + COL_TABLE_ROW_HAS_FILE_REFS + " IS TRUE)"
			+ " ORDER BY " + COL_TABLE_ROW_ID; 
		
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	private TableFileHandleAssociationMapper mapper;
	
	public TableFileHandleScanner(NamedParameterJdbcTemplate namedJdbcTemplate, TableFileHandleAssociationMapper mapper) {
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.mapper = mapper;
	}
	
	@Override
	public long getMaxIdRangeSize() {
		return MAX_SCAN_ID_RANGE;
	}
	
	@Override
	public IdRange getIdRange() {
		return namedJdbcTemplate.getJdbcTemplate().queryForObject(SQL_SELECT_MIN_MAX, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
	}

	@Override
	public Iterable<ScannedFileHandleAssociation> scanRange(IdRange range) {
		ValidateArgument.required(range, "The range");
		ValidateArgument.requirement(range.getMinId() <= range.getMaxId(), "Invalid range, the minId must be lesser or equal than the maxId");
		
		final Map<String, Object> params = ImmutableMap.of(DMLUtils.BIND_MIN_ID, range.getMinId(), DMLUtils.BIND_MAX_ID, range.getMaxId());
		
		return new QueryStreamIterable<>(namedJdbcTemplate, mapper, SQL_SELECT_BATCH, params, MAX_FETCH_LIMIT);
	}
		
}
