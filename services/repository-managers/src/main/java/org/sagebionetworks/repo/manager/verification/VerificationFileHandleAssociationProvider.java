package org.sagebionetworks.repo.manager.verification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class VerificationFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private VerificationDAO verificationDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public VerificationFileHandleAssociationProvider(VerificationDAO verificationDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.verificationDao = verificationDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOVerificationSubmissionFile().getTableMapping());
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		List<Long> associatedIds = verificationDao.listFileHandleIds(Long.parseLong(objectId));
		Set<String> result = new HashSet<String>();
		for (String id : fileHandleIds) {
			if (associatedIds.contains(Long.parseLong(id))) result.add(id);
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.VERIFICATION_SUBMISSION;
	}

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
