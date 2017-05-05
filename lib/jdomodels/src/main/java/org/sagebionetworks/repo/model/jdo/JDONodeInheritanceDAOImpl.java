package org.sagebionetworks.repo.model.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class JDONodeInheritanceDAOImpl implements NodeInheritanceDAO {
	
	private static final String SQL_COUNT_NODE = "SELECT COUNT(*) FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SELECT_ENTITY_BENEFACTOR_FUNCTION = "SELECT "+FUNCTION_GET_ENTITY_BENEFACTOR_ID+"(?)";
	private static final String SELECT_BENEFICIARIES = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_BENEFACTOR_ID+" = ?";
	private static final String SELECT_BENEFACTOR = "SELECT "+COL_NODE_BENEFACTOR_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
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
		List<String> list = jdbcTemplate.query(SELECT_BENEFICIARIES, new RowMapper<String>(){
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return KeyFactory.keyToString(rs.getLong(COL_NODE_ID));
			}}, id);
		return new HashSet<String>(list);
	}

	@Override
	public String getBenefactorCached(String beneficiaryId) throws NotFoundException, DatastoreException {
		try{
			long benefactorId = jdbcTemplate.queryForObject(SELECT_BENEFACTOR, Long.class, KeyFactory.stringToKey(beneficiaryId));
			return KeyFactory.keyToString(benefactorId);
		} catch (EmptyResultDataAccessException | NullPointerException e) {
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

	@Override
	public String getBenefactor(String beneficiaryId) {
		Long id = KeyFactory.stringToKey(beneficiaryId);
		Long benefactorId = jdbcTemplate.queryForObject(SELECT_ENTITY_BENEFACTOR_FUNCTION, Long.class, id);
		if(benefactorId == null){
			/*
			 * Benefactor will be null if the node does not exist or if the node
			 * is in the trash. In either case a NotFoundException should be
			 * thrown.
			 */
			throw new NotFoundException("Benefactor not found for: "+beneficiaryId);
		}else if (benefactorId < 0){
			throw new IllegalStateException("Infinite loop detected for: "+beneficiaryId);
		}
		return KeyFactory.keyToString(benefactorId);
	}


	@Override
	public boolean doesNodeExist(String id){
		ValidateArgument.required(id, "id");
		long count = jdbcTemplate.queryForObject(SQL_COUNT_NODE, Long.class, KeyFactory.stringToKey(id));
		return count > 0;
	}
}
