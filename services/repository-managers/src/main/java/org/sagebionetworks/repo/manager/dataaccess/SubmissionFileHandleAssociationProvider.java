package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.RowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	static final RowMapperSupplier ROW_MAPPER_SUPPLIER = new SerializedFieldRowMapperSupplier<>(SubmissionUtils::readSerializedField, SubmissionUtils::extractAllFileHandleIds);
	
	private SubmissionDAO submissionDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public SubmissionFileHandleAssociationProvider(SubmissionDAO submissionDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.submissionDao = submissionDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOSubmission().getTableMapping(), COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED, DEFAULT_BATCH_SIZE, ROW_MAPPER_SUPPLIER);
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.DataAccessSubmissionAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Submission submission = submissionDao.getSubmission(objectId);
		Set<String> associatedIds = SubmissionUtils.extractAllFileHandleIds(submission);
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}
	
	

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_SUBMISSION;
	}

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
