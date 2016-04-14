package org.sagebionetworks.repo.model.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;

public class JDONodeInheritanceDAOImpl implements NodeInheritanceDAO {
	
	private static final String SELECT_BENEFICIARIES = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_BENEFACTOR_ID+" = ?";
	private static final String SELECT_BENEFACTOR = "SELECT "+COL_NODE_BENEFACTOR_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	private DBONode getNodeById(Long id) throws NotFoundException, DatastoreException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		return dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
	}

	@Override
	public Set<String> getBeneficiaries(String benefactorId) throws NotFoundException, DatastoreException {
		Long id = KeyFactory.stringToKey(benefactorId);
		List<String> list = simpleJdbcTemplate.query(SELECT_BENEFICIARIES, new RowMapper<String>(){
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return KeyFactory.keyToString(rs.getLong(COL_NODE_ID));
			}}, id);
		return new HashSet<String>(list);
	}

	@Override
	public String getBenefactor(String beneficiaryId) throws NotFoundException, DatastoreException {
		try{
			return KeyFactory.keyToString(simpleJdbcTemplate.queryForLong(SELECT_BENEFACTOR, KeyFactory.stringToKey(beneficiaryId)));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Entity id: "+beneficiaryId+" not found");
		}
	}

	@WriteTransaction
	@Override
	public void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException, DatastoreException {
		// By default we do not want to keep the etag
		boolean keepOldEtag = false;
		addBeneficiary(beneficiaryId, toBenefactorId, keepOldEtag);
	}

	@WriteTransaction
	@Override
	public void addBeneficiary(String beneficiaryId, String toBenefactorId,
			boolean keepOldEtag) throws NotFoundException, DatastoreException {
		DBONode benefactor = getNodeById(KeyFactory.stringToKey(toBenefactorId));
		DBONode beneficiary = getNodeById(KeyFactory.stringToKey(beneficiaryId));
		beneficiary.setBenefactorId(benefactor.getId());
		// Make sure the etag changes. See PLFM-1467 and PLFM-1517.
		if (!keepOldEtag) {
			// Update the etag
			beneficiary.seteTag(UUID.randomUUID().toString());
		}
		transactionalMessenger.sendMessageAfterCommit(beneficiary, ChangeType.UPDATE);
		dboBasicDao.update(beneficiary);
	}


}
