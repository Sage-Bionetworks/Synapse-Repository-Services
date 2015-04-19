package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;


/**
 * @author brucehoff
 *
 */
public class DBOAccessApprovalDAOImpl implements AccessApprovalDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String SELECT_FOR_REQUIREMENT_SQL = 
		"SELECT * FROM "+TABLE_ACCESS_APPROVAL+" WHERE "+
		COL_ACCESS_APPROVAL_REQUIREMENT_ID+"=:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID;

	private static final String SELECT_FOR_REQUIREMENT_AND_PRINCIPAL_SQL = 
		"SELECT * FROM "+TABLE_ACCESS_APPROVAL+" WHERE "+
		COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+
		") AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" IN (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")";

	private static final String SELECT_FOR_UPDATE_SQL = "select "+
	COL_ACCESS_APPROVAL_CREATED_BY+", "+
	COL_ACCESS_APPROVAL_CREATED_ON+", "+
	COL_ACCESS_APPROVAL_ETAG+
	" from "+TABLE_ACCESS_APPROVAL+" where "+COL_ACCESS_APPROVAL_ID+
	"=:"+COL_ACCESS_APPROVAL_ID+" for update";

	private static final RowMapper<DBOAccessApproval> rowMapper = (new DBOAccessApproval()).getTableMapping();


	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class, param);
	}

	@WriteTransaction
	@Override
	public <T extends AccessApproval> T create(T dto) throws DatastoreException,
			InvalidModelException {
		
		// don't create if it already exists
		List<AccessApproval> existingApprovals = getForAccessRequirementsAndPrincipals(
				Collections.singletonList(dto.getRequirementId().toString()),
				Collections.singletonList(dto.getAccessorId()));
		if (!existingApprovals.isEmpty()) {
			return (T)existingApprovals.get(0);
		}
		
		DBOAccessApproval dbo = new DBOAccessApproval();
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId());
		}
		if (dbo.geteTag() == null) {
			dbo.seteTag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		T result = (T)AccessApprovalUtils.copyDboToDto(dbo);
		return result;
	}

	@Override
	public AccessApproval get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		DBOAccessApproval dbo = basicDao.getObjectByPrimaryKey(DBOAccessApproval.class, param);
		AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
		return dto;
	}

	@Override
	public List<AccessApproval> getForAccessRequirementsAndPrincipals(Collection<String> accessRequirementIds, Collection<String> principalIds) throws DatastoreException {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		if (accessRequirementIds.isEmpty() || principalIds.isEmpty()) return dtos;
		MapSqlParameterSource params = new MapSqlParameterSource();		
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementIds);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		List<DBOAccessApproval> dbos = simpleJdbcTemplate.query(SELECT_FOR_REQUIREMENT_AND_PRINCIPAL_SQL, rowMapper, params);
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
			// validate:  The principal ID and accessor ID should each be from the passed in lists
			if (!principalIds.contains(dto.getAccessorId())) 
				throw new IllegalStateException("PrincipalIDs: "+principalIds+" but accessorId: "+dto.getAccessorId());
			if (!accessRequirementIds.contains(dto.getRequirementId().toString()))
				throw new IllegalStateException("accessRequirementIds: "+accessRequirementIds+" but requirementId: "+dto.getRequirementId());
			dtos.add(dto);
		}
		return dtos;
	}

	@WriteTransaction
	@Override
	public <T extends AccessApproval> T  update(T dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID, dto.getId());
		List<DBOAccessApproval> aas = null;
		try{
			aas = simpleJdbcTemplate.query(SELECT_FOR_UPDATE_SQL, new RowMapper<DBOAccessApproval>(){
				@Override
				public DBOAccessApproval mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					DBOAccessApproval aa = new DBOAccessApproval();
					aa.setCreatedOn(rs.getLong(COL_ACCESS_APPROVAL_CREATED_ON));
					aa.setCreatedBy(rs.getLong(COL_ACCESS_APPROVAL_CREATED_BY));
					aa.seteTag(rs.getString(COL_ACCESS_APPROVAL_ETAG));
					return aa;
				}
			}, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		if (aas.isEmpty()) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");			
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		DBOAccessApproval dbo = aas.get(0);
		if (!dbo.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Access Approval was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		
		// Update with a new e-tag
		dbo.seteTag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user Access Approval in database.");

		T resultantDto = (T)AccessApprovalUtils.copyDboToDto(dbo);

		return resultantDto;
	}

	private static final TableMapping<DBOAccessApproval> TABLE_MAPPING = (new DBOAccessApproval()).getTableMapping();

	@Override
	public List<AccessApproval> getForAccessRequirement(String accessRequirementId) throws DatastoreException {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		MapSqlParameterSource params = new MapSqlParameterSource();		
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		List<DBOAccessApproval> dbos = simpleJdbcTemplate.query(SELECT_FOR_REQUIREMENT_SQL, rowMapper, params);
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
			if (!accessRequirementId.equals(dto.getRequirementId().toString()))
				throw new IllegalStateException("accessRequirementId: "+accessRequirementId+
						" but dto.getRequirementId(): "+dto.getRequirementId());
			dtos.add(dto);
		}
		return dtos;
	}
}
