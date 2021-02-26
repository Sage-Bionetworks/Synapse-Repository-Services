package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED;

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
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBORequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RequestFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private static Logger LOG = LogManager.getLogger(RequestFileHandleAssociationProvider.class);
	
	static final RowMapper<ScannedFileHandleAssociation> SCANNED_MAPPER = (ResultSet rs, int i) -> {
		
		final String objectId = rs.getString(COL_DATA_ACCESS_REQUEST_ID);
		
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(objectId);

		final java.sql.Blob blob = rs.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED);
		
		if (blob == null) {
			return association;
		}
		
		byte[] serializedField = blob.getBytes(1, (int) blob.length());
		
		RequestInterface request;
		
		try {
			request = RequestUtils.readSerializedField(serializedField);
		} catch (DatastoreException e) {
			LOG.warn(e.getMessage(),  e);
			return association;
		}

		Set<String> fileHandleIds = extractAllFileHandles(request);
		
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
	

	private RequestDAO requestDao;
	private FileHandleAssociationScanner scanner;

	@Autowired
	public RequestFileHandleAssociationProvider(RequestDAO requestDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.requestDao = requestDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORequest().getTableMapping(), COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED, DEFAULT_BATCH_SIZE, SCANNED_MAPPER);
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.DataAccessRequestAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		
		RequestInterface request = requestDao.get(objectId);
		
		Set<String> associatedIds = extractAllFileHandles(request);
		
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}
	
	private static Set<String> extractAllFileHandles(RequestInterface request) {
		Set<String> fileHandleIds = new HashSet<String>();
		if (request.getAttachments()!= null && !request.getAttachments().isEmpty()) {
			fileHandleIds.addAll(request.getAttachments());
		}
		if (request.getDucFileHandleId() != null) {
			fileHandleIds.add(request.getDucFileHandleId());
		}
		if (request.getIrbFileHandleId() != null) {
			fileHandleIds.add(request.getIrbFileHandleId());
		}
		return fileHandleIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_REQUEST;
	}
	
	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
