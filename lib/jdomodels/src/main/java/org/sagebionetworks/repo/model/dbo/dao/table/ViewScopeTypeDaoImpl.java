package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_TYPE_MASK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_TYPE;

import java.sql.ResultSet;

import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ViewScopeTypeDaoImpl implements ViewScopeTypeDao {
	
	private static final String SQL_TRUNCATE_TABLE = "DELETE FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" IS NOT NULL";

	private static final String SQL_SELECT_VIEW_SCOPE_TYPE = "SELECT "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+", "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" = ?";

	private static final String SQL_INSERT_VIEW_TYPE = "INSERT INTO "+TABLE_VIEW_TYPE+" ("+COL_VIEW_TYPE_VIEW_ID+", "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+", "+COL_VIEW_TYPE_VIEW_TYPE_MASK+", "+COL_VIEW_TYPE_ETAG+") VALUES(?,?,?,UUID()) ON DUPLICATE KEY UPDATE "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+" = ?, "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" = ?, "+COL_VIEW_TYPE_ETAG+" = UUID()";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@WriteTransaction
	@Override
	public void setViewScopeType(Long viewId, ViewScopeType scopeType) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(scopeType.getObjectType(), "scopeType.objectType");
		ValidateArgument.required(scopeType.getTypeMask(), "scopeType.typeMask");
		
		String objectType = scopeType.getObjectType().name();
		Long typeMask = scopeType.getTypeMask();
		
		jdbcTemplate.update(SQL_INSERT_VIEW_TYPE,viewId, objectType, typeMask, objectType, typeMask);
	}
	
	@Override
	public ViewScopeType getViewScopeType(Long viewId) {
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_VIEW_SCOPE_TYPE, (ResultSet rs, int rowNum) -> {
				
				ViewObjectType objectType = ViewObjectType.valueOf(rs.getString(COL_VIEW_TYPE_VIEW_OBJECT_TYPE));
				Long typeMask = rs.getLong(COL_VIEW_TYPE_VIEW_TYPE_MASK);
				
				return new ViewScopeType(objectType, typeMask);
			}, viewId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(""+viewId);
		}
	}

	@WriteTransaction
	public void truncateAll(){
		jdbcTemplate.update(SQL_TRUNCATE_TABLE);
	}
}
