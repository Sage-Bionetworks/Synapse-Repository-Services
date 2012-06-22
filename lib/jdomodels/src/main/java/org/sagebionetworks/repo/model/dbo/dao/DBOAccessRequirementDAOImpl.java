/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
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
public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;
	

	private static final String NODE_ID_PARAM = "NODE_ID";
	private static final String SELECT_FOR_NODE_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_ACCESS_REQUIREMENT+" WHERE "+COL_ACCESS_REQUIREMENT_NODE_ID+"=:"+NODE_ID_PARAM;
	
	private static final RowMapper<DBOAccessRequirement> rowMapper = (new DBOAccessRequirement()).getTableMapping();


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		basicDao.deleteObjectById(DBOAccessRequirement.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(AccessRequirement dto) throws DatastoreException,
			InvalidModelException {
		DBOAccessRequirement jdo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, jdo);
		if (jdo.geteTag()==null) jdo.seteTag(0L);
		jdo = basicDao.createNew(jdo);
		return jdo.getId().toString();
	}

	@Override
	public AccessRequirement get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		try {
			DBOAccessRequirement jdo = basicDao.getObjectById(DBOAccessRequirement.class, param);
			AccessRequirement dto = new AccessRequirement();
			AccessRequirementUtils.copyDboToDto(jdo, dto);
			return dto;
		} catch (Exception e) {
			throw new RuntimeException(param.getValues().toString(), e);
		}
	}

	@Override
	public Collection<AccessRequirement> getForNode(String nodeId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();		
		param.addValue(COL_ACCESS_REQUIREMENT_NODE_ID, KeyFactory.stringToKey(nodeId));
		List<DBOAccessRequirement> dbos = simpleJdbcTempalte.query(SELECT_FOR_NODE_SQL, rowMapper, param);
		List<AccessRequirement> dtos = new ArrayList<AccessRequirement>();
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = new AccessRequirement();
			AccessRequirementUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessRequirement update(AccessRequirement dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		// LOCK the record
		DBOAccessRequirement dbo = null;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, dto.getId());
		try{
			dbo = simpleJdbcTempalte.queryForObject(SELECT_FOR_UPDATE_SQL, TABLE_MAPPING, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		// check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.geteTag().equals(Long.parseLong(dto.getEtag())))
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		dbo.seteTag(1L+dbo.geteTag());
		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user Access Requirement in database.");
		AccessRequirement resultantDto = new AccessRequirement();
		AccessRequirementUtils.copyDboToDto(dbo,  resultantDto);
		return resultantDto;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'

	private static final String SELECT_FOR_UPDATE_SQL = "select * from "+TABLE_ACCESS_REQUIREMENT+" where "+COL_ACCESS_REQUIREMENT_ID+
			"=:"+COL_ACCESS_REQUIREMENT_ID+" for update";
	
	private static final TableMapping<DBOAccessRequirement> TABLE_MAPPING = (new DBOAccessRequirement()).getTableMapping();
	
	@Override
	public void getAccessRequirementParameters(String id, Object paramsDto, ObjectSchema schema)  throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		DBOAccessRequirement dbo = basicDao.getObjectById(DBOAccessRequirement.class, param);
		AccessRequirementUtils.copyRequirementParamsDboToDto(dbo, paramsDto, schema);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String setAccessRequirementParameters(String id, String etag, Object paramsDto, ObjectSchema schema) throws DatastoreException, InvalidModelException,
	NotFoundException, ConflictingUpdateException {
		// LOCK the record
		DBOAccessRequirement dbo = null;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, id);
		try{
			dbo = simpleJdbcTempalte.queryForObject(SELECT_FOR_UPDATE_SQL, TABLE_MAPPING, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		// check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.geteTag().equals(Long.parseLong(etag)))
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		AccessRequirementUtils.copyAccessRequirementParamsDtoToDbo(paramsDto, dbo, schema);
		dbo.seteTag(1L+dbo.geteTag());
		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user Access Requirement in database.");
		return dbo.geteTag().toString();
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'
}
