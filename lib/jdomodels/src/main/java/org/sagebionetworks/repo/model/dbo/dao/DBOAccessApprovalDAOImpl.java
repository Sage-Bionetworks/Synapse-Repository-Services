/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.AccessClassHelper;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author brucehoff
 *
 */
public class DBOAccessApprovalDAOImpl implements AccessApprovalDAO {
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;
	

	private static final String SELECT_FOR_REQUIREMENT_AND_PRINCIPAL_SQL = 
			"SELECT * FROM "+TABLE_ACCESS_APPROVAL+" WHERE "+
			COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+
			") AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" IN (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")";
	
	private static final RowMapper<DBOAccessApproval> rowMapper = (new DBOAccessApproval()).getTableMapping();


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		basicDao.deleteObjectById(DBOAccessApproval.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessApproval> T create(T dto) throws DatastoreException,
			InvalidModelException {
		DBOAccessApproval jdo = new DBOAccessApproval();
		AccessApprovalUtils.copyDtoToDbo(dto, jdo);
		if (jdo.geteTag()==null) jdo.seteTag(0L);
		jdo = basicDao.createNew(jdo);
		AccessApprovalUtils.copyDboToDto(jdo, dto);
		return dto;
	}

	public static AccessApproval instanceForType(String typeString) throws DatastoreException {
		AccessApprovalType type = AccessApprovalType.valueOf(typeString);
		try {
			return AccessClassHelper.getClass(type).newInstance();
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public AccessApproval get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		DBOAccessApproval dbo = basicDao.getObjectById(DBOAccessApproval.class, param);
		AccessApproval dto = instanceForType(dbo.getApprovalType());
		AccessApprovalUtils.copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public List<AccessApproval> getForAccessRequirementsAndPrincipals(Collection<String> accessRequirementIds, Collection<String> principalIds) throws DatastoreException {
		MapSqlParameterSource params = new MapSqlParameterSource();		
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementIds);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		List<DBOAccessApproval> dbos = simpleJdbcTempalte.query(SELECT_FOR_REQUIREMENT_AND_PRINCIPAL_SQL, rowMapper, params);
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = instanceForType(dbo.getApprovalType());
			AccessApprovalUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessApproval> T  update(T dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		// LOCK the record
		DBOAccessApproval dbo = null;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID, dto.getId());
		try{
			dbo = simpleJdbcTempalte.queryForObject(SELECT_FOR_UPDATE_SQL, TABLE_MAPPING, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		// check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.geteTag().equals(Long.parseLong(dto.getEtag())))
			throw new ConflictingUpdateException("Access Approval was updated since you last fetched it, retrieve it again and reapply the update.");
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		dbo.seteTag(1L+dbo.geteTag());
		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user Access Approval in database.");
		T resultantDto = (T)instanceForType(dbo.getApprovalType());
		AccessApprovalUtils.copyDboToDto(dbo,  resultantDto);
		return resultantDto;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'

	private static final String SELECT_FOR_UPDATE_SQL = "select * from "+TABLE_ACCESS_APPROVAL+" where "+COL_ACCESS_APPROVAL_ID+
			"=:"+COL_ACCESS_APPROVAL_ID+" for update";
	
	private static final TableMapping<DBOAccessApproval> TABLE_MAPPING = (new DBOAccessApproval()).getTableMapping();
	

}
