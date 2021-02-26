package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private static Logger LOG = LogManager.getLogger(SubmissionFileHandleAssociationProvider.class);
	
	static final RowMapper<ScannedFileHandleAssociation> SCANNED_MAPPER = (ResultSet rs, int i) -> {
		
		final String objectId = rs.getString(COL_DATA_ACCESS_SUBMISSION_ID);
		
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(objectId);

		final java.sql.Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
		
		if (blob == null) {
			return association;
		}
		
		byte[] serializedField = blob.getBytes(1, (int) blob.length());
		
		Submission submission;
		
		try {
			submission = SubmissionUtils.readSerializedField(serializedField);
		} catch (DatastoreException e) {
			LOG.warn(e.getMessage(),  e);
			return association;
		}

		Set<String> fileHandleIds = extractAllFileHandles(submission);
		
		if (fileHandleIds.isEmpty()) {
			return association;
		}
		
		return association.withFileHandleIds(fileHandleIds.stream()
				.map(idString -> {
					Long id = null;
					try {
						 id = KeyFactory.stringToKey(idString);
					} catch (IllegalArgumentException e) {
						LOG.warn(e.getMessage(), e);
					}
					return id;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));
	};

	private SubmissionDAO submissionDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public SubmissionFileHandleAssociationProvider(SubmissionDAO submissionDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.submissionDao = submissionDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOSubmission().getTableMapping(), COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED, DEFAULT_BATCH_SIZE, SCANNED_MAPPER);
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Submission submission = submissionDao.getSubmission(objectId);
		Set<String> associatedIds = extractAllFileHandles(submission);
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}
	
	private static Set<String> extractAllFileHandles(Submission submission) {
		Set<String> associatedIds = new HashSet<String>();
		if (submission.getAttachments()!= null && !submission.getAttachments().isEmpty()) {
			associatedIds.addAll(submission.getAttachments());
		}
		if (submission.getDucFileHandleId() != null) {
			associatedIds.add(submission.getDucFileHandleId());
		}
		if (submission.getIrbFileHandleId() != null) {
			associatedIds.add(submission.getIrbFileHandleId());
		}
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
