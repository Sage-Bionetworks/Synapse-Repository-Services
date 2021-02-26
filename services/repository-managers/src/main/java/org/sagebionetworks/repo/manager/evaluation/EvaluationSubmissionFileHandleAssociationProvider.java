package org.sagebionetworks.repo.manager.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dbo.SubmissionFileHandleDBO;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EvaluationSubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider{

	private SubmissionFileHandleDAO submissionFileHandleDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public EvaluationSubmissionFileHandleAssociationProvider(SubmissionFileHandleDAO submissionFileHandleDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.submissionFileHandleDao = submissionFileHandleDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new SubmissionFileHandleDBO().getTableMapping());
	}

	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.SubmissionAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		List<String> associatedIds = submissionFileHandleDao.getAllBySubmission(objectId);
		Set<String> result = new HashSet<String>();
		for (String fileHandleId : fileHandleIds) {
			if (associatedIds.contains(fileHandleId)) {
				result.add(fileHandleId);
			}
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.EVALUATION_SUBMISSIONS;
	}

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
