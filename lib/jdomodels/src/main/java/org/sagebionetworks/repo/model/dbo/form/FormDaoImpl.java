package org.sagebionetworks.repo.model.dbo.form;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_GROUP;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FormDaoImpl implements FormDao {

	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	JdbcTemplate jdbcTemplate;

	private static RowMapper<DBOFormGroup> GROUP_MAPPER = new DBOFormGroup().getTableMapping();

	@WriteTransaction
	@Override
	public FormGroup createFormGroup(Long creator, String name) {
		ValidateArgument.required(creator, "creator");
		ValidateArgument.required(name, "name");
		DBOFormGroup dbo = new DBOFormGroup();
		dbo.setName(name);
		dbo.setCreatedBy(creator);
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setGroupId(idGenerator.generateNewId(IdType.FORM_GROUP_ID));
		dbo = basicDao.createNew(dbo);
		return createGroupDto(dbo);
	}

	/**
	 * Create a FormGroup from the DBO.
	 * 
	 * @param dbo
	 * @return
	 */
	public static FormGroup createGroupDto(DBOFormGroup dbo) {
		ValidateArgument.required(dbo, "dbo");
		FormGroup dto = new FormGroup();
		dto.setGroupId(dbo.getGroupId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn().getTime()));
		dto.setName(dbo.getName());
		return dto;
	}

	@Override
	public Optional<FormGroup> lookupGroupByName(String name) {
		ValidateArgument.required(name, "name");
		try {
			DBOFormGroup dbo = jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_FORM_GROUP + " WHERE " + COL_FORM_GROUP_NAME + " = ?", GROUP_MAPPER, name);
			return Optional.of(createGroupDto(dbo));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public FormData createFormData(Long creatorId, String groupId, String name, String dataFileHandleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getFormDataCreator(String id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getFormDataGroupId(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FormData updateFormData(String id, String name, String dataFileHandleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FormData updateFormData(String id, String dataFileHandleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateEnum getFormDataState(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
