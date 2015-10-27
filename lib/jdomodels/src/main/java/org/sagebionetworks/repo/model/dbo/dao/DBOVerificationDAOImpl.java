package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBOVerificationDAOImpl implements VerificationDAO {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	private static TableMapping<DBOVerificationSubmission> DBO_VERIFICATION_TABLE_MAPPING =
			(new DBOVerificationSubmission()).getTableMapping();
	
	@WriteTransaction
	@Override
	public VerificationSubmission createVerificationSubmission(
			VerificationSubmission dto) throws DatastoreException {
		dto.setId(idGenerator.generateNewId(TYPE.VERIFICATION_SUBMISSION).toString());
		DBOVerificationSubmission dbo = copyDTOtoDBO(dto);
		DBOVerificationSubmission created = basicDao.createNew(dbo);
		VerificationState initialState = new VerificationState();
		initialState.setCreatedBy(dto.getCreatedBy());
		initialState.setCreatedOn(dto.getCreatedOn());
		initialState.setState(VerificationStateEnum.submitted);
		appendVerificationSubmissionState(initialState);
		storeFileHandleIds(dbo.getId(), dto.getFiles());
		return copyDBOtoDTO(created, Collections.singletonList(initialState));
	}
	
	public static DBOVerificationSubmission copyDTOtoDBO(VerificationSubmission dto) {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setId(Long.parseLong(dto.getId()));
		try {
			dbo.setSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return dbo;
	}
	
	public static VerificationSubmission copyDBOtoDTO(DBOVerificationSubmission dbo, List<VerificationState> stateHistory) {
		VerificationSubmission dto = new VerificationSubmission();
		try {
			dto = (VerificationSubmission)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerialized());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dto.setStateHistory(stateHistory);
		return dto;
	}
	
	private void storeFileHandleIds(Long verificationSubmissionId, List<String> fileHandleIds) {
		if (fileHandleIds==null || fileHandleIds.isEmpty()) return;
		List<DBOVerificationSubmissionFile> batch = new ArrayList<DBOVerificationSubmissionFile>();
		for (String fileHandleId : fileHandleIds) {
			DBOVerificationSubmissionFile sf = new DBOVerificationSubmissionFile();
			sf.setId(verificationSubmissionId);
			sf.setFileHandleId(Long.parseLong(fileHandleId));
			batch.add(sf);
		}
		basicDao.createBatch(batch);
	}
	
	@Override
	public List<VerificationSubmission> listVerificationSubmissions(
			VerificationStateEnum state, Long userId, long limit, long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long countVerificationSubmissions(VerificationStateEnum state,
			Long userId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteVerificationSubmission(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void appendVerificationSubmissionState(VerificationState newState) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isFileHandleIdInVerificationSubmission(String id,
			String fileHandleId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VerificationSubmission getActiveVerificationSubmission(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean doesUserHaveActiveVerification(Long userId) {
		// TODO Auto-generated method stub
		return false;
	}

}
