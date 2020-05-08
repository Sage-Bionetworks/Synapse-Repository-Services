package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_CONTAINER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_TYPE_MASK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SCOPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_TYPE;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;


public class ViewScopeDaoImpl implements ViewScopeDao {
	
	private static final String SQL_TRUNCATE_TABLE = "DELETE FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" IS NOT NULL";

	private static final String SQL_SELECT_VIEW_SCOPE_TYPE = "SELECT "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+", "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" = ?";

	private static final String SQL_INSERT_VIEW_TYPE = "INSERT INTO "+TABLE_VIEW_TYPE+" ("+COL_VIEW_TYPE_VIEW_ID+", "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+", "+COL_VIEW_TYPE_VIEW_TYPE_MASK+", "+COL_VIEW_TYPE_ETAG+") VALUES(?,?,?,UUID()) ON DUPLICATE KEY UPDATE "+COL_VIEW_TYPE_VIEW_OBJECT_TYPE+" = ?, "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" = ?, "+COL_VIEW_TYPE_ETAG+" = UUID()";

	private static final String SQL_SELECT_CONTAINERS_FOR_VIEW = "SELECT "+COL_VIEW_SCOPE_CONTAINER_ID+" FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_VIEW_ID+" = ?";

	private static final String SQL_INSERT_VIEW_SCOPE = "INSERT INTO "+TABLE_VIEW_SCOPE+" ("+COL_VIEW_SCOPE_VIEW_ID+", "+COL_VIEW_SCOPE_CONTAINER_ID+") VALUES (?,?)";

	private static final String SQL_DELETE_ALL_FOR_VIEW_ID = "DELETE FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_VIEW_ID+" = ?";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@WriteTransaction
	@Override
	public void setViewScopeAndType(Long viewId, Set<Long> containerIds, ViewScopeType scopeType) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(scopeType.getObjectType(), "scopeType.objectType");
		ValidateArgument.required(scopeType.getTypeMask(), "scopeType.typeMask");
		
		String objectType = scopeType.getObjectType().name();
		Long typeMask = scopeType.getTypeMask();
		
		jdbcTemplate.update(SQL_INSERT_VIEW_TYPE,viewId, objectType, typeMask, objectType, typeMask);
		
		// clear any existing scope for this view
		jdbcTemplate.update(SQL_DELETE_ALL_FOR_VIEW_ID, viewId);
		
		// add the containers
		if(containerIds != null){
			Iterator<Long> it = containerIds.iterator();
			while(it.hasNext()){
				Long containerId = it.next();
				if(containerId != null){
					jdbcTemplate.update(SQL_INSERT_VIEW_SCOPE, viewId,containerId);
				}
			}
		}
	}

	@Override
	public Set<Long> getViewScope(Long viewId) {
		List<Long> list = jdbcTemplate.queryForList(SQL_SELECT_CONTAINERS_FOR_VIEW, Long.class, viewId);
		return new HashSet<Long>(list);
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
