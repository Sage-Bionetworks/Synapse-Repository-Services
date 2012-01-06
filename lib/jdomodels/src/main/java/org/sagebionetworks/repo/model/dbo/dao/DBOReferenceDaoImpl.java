package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the DBOReferenceDao.
 * 
 * @author John
 *
 */
@Transactional(readOnly = true)
public class DBOReferenceDaoImpl implements DBOReferenceDao {
	
	private static final String DELETE_SQL = "DELETE FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ?";
	private static final String SELECT_SQL = "SELECT "+COL_REFERENCE_GROUP_NAME+", "+COL_REFERENCE_TARGET_NODE+", "+COL_REFERENCE_TARGET_REVISION_NUMBER+" FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ?";
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;

	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Map<String, Set<Reference>> replaceReferences(Long ownerId, Map<String, Set<Reference>> references) throws DatastoreException {
		if(ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		if(references == null) throw new IllegalArgumentException("References cannot be null");
		// First delete all references for this entity.
		simpleJdbcTempalte.update(DELETE_SQL, ownerId);
		// Create the list of references
		List<DBOReference> batch = ReferenceUtil.createDBOReferences(ownerId, references);
		if(batch.size() > 0 ){
			dboBasicDao.createBatch(batch);
		}
		return references;
	}

	@Transactional(readOnly = true)
	@Override
	public Map<String, Set<Reference>> getReferences(Long ownerId) {
		if(ownerId == null) throw new IllegalArgumentException("OwnerId cannot be null");
		// Build up the results from the DB.
		final Map<String, Set<Reference>> results = new HashMap<String, Set<Reference>>();
		simpleJdbcTempalte.query(SELECT_SQL, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String groupName = rs.getString(COL_REFERENCE_GROUP_NAME);
				Set<Reference> set = results.get(groupName);
				if(set == null){
					set = new HashSet<Reference>();
					results.put(groupName, set);
				}
				// Create the reference
				Reference reference = new Reference();
				try {
					reference.setTargetId(KeyFactory.keyToString(rs.getLong(COL_REFERENCE_TARGET_NODE)));
					reference.setTargetVersionNumber(rs.getLong(COL_REFERENCE_TARGET_REVISION_NUMBER));
				} catch (DatastoreException e) {
					throw new SQLException(e);
				}
				// Add it to its group
				set.add(reference);
				return groupName;
			}
		}, ownerId);
		return results;
	}

}
