package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.RowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBORequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RequestFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	static final RowMapperSupplier ROW_MAPPER_SUPPLIER = new SerializedFieldRowMapperSupplier<>(RequestUtils::readSerializedField, RequestUtils::extractAllFileHandleIds);
	
	private RequestDAO requestDao;
	private FileHandleAssociationScanner scanner;

	@Autowired
	public RequestFileHandleAssociationProvider(RequestDAO requestDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.requestDao = requestDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORequest().getTableMapping(), COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED, DEFAULT_BATCH_SIZE, ROW_MAPPER_SUPPLIER);
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.DataAccessRequestAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		
		RequestInterface request = requestDao.get(objectId);
		
		Set<String> associatedIds = RequestUtils.extractAllFileHandleIds(request);
		
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
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
