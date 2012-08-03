package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface AdministrationService {
	PaginatedResults<MigratableObjectData> getAllBackupObjects(String userId, Integer offset, Integer limit, Boolean includeDependencies) throws DatastoreException, NotFoundException, UnauthorizedException;

	BackupRestoreStatus startBackup(String userId, String type, HttpHeaders header, HttpServletRequest request) throws IOException, DatastoreException, NotFoundException, UnauthorizedException;

	BackupRestoreStatus startRestore(String userId, RestoreSubmission file, String type) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	void deleteMigratableObject(String userId, String objectId, String type) throws UnauthorizedException, DatastoreException, NotFoundException;
	 
	BackupRestoreStatus startSearchDocument(String userId, HttpHeaders header, HttpServletRequest request) throws UnauthorizedException, DatastoreException, IOException, NotFoundException;
	
	BackupRestoreStatus getStatus(String userId, String daemonId) throws UnauthorizedException, DatastoreException, IOException, NotFoundException ;
	
	void terminateDaemon(String userId, String daemonId) throws UnauthorizedException, DatastoreException, IOException, NotFoundException ;
	
	StackStatus getStackStatus();
	
	StackStatus updateStatusStackStatus(String userId, HttpHeaders header, HttpServletRequest request) throws UnauthorizedException, DatastoreException, IOException, NotFoundException ;
	
	
}
