package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller for Migrating data between stacks.
 * 
 * @author jmhill
 * 
 */
@Controller
public class MigrationController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get the counts for each migration type.
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MIGRATION_COUNTS, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeCounts getTypeCounts(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getTypeCounts(userId);
	}
	
	/**
	 * This method is used to query a source stack for all of its metadata.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MIGRATION_ROWS, method = RequestMethod.GET)
	public @ResponseBody
	RowMetadataResult getRowMetadata(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String type,
			@RequestParam(required = true) Long limit,
			@RequestParam(required = true) Long offset)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getRowMetadaForType(
				userId, MigrationType.valueOf(type), limit, offset);
	}

	/**
	 * This method is called on the destination stack to compare compare its
	 * metadata with the source stack metadata
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MIGRATION_DELTA, method = RequestMethod.GET)
	public @ResponseBody
	RowMetadataResult getRowMetadataDelta(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String type,
			@RequestBody IdList request) throws DatastoreException,
			NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return serviceProvider.getMigrationService().getRowMetadataDeltaForType(userId,	MigrationType.valueOf(type), request.getList());
	}

	/**
	 * Start a backup daemon. Monitor the status of the daemon with the
	 * getStatus method.
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.MIGRATION_BACKUP }, method = RequestMethod.POST)
	public @ResponseBody
	BackupRestoreStatus startBackup(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String type,
			@RequestBody IdList request) throws DatastoreException, NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return serviceProvider.getMigrationService().startBackup(userId, MigrationType.valueOf(type), request.getList());
	}
	
	/**
	 * Start a system restore daemon using the passed file name.  The file must be in the 
	 * the bucket belonging to this stack.
	 * 
	 * @param fileName
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.MIGRATION_RESTORE }, method = RequestMethod.POST)
	public @ResponseBody
	BackupRestoreStatus startRestore(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String type,
			@RequestBody RestoreSubmission request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		return serviceProvider.getMigrationService().startRestore(userId, MigrationType.valueOf(type), request.getFileName());
	}
	
	/**
	 * Delete a migratable object
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_DELETE	}, method = RequestMethod.PUT)
	public @ResponseBody MigrationTypeCount deleteMigratableObject(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String type,
			@RequestBody IdList request) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return serviceProvider.getMigrationService().delete(userId,  MigrationType.valueOf(type), request.getList());
	}
	
	/**
	 * Get the status of a running daemon (either a backup or restore)
	 * @param daemonId
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_STATUS }, method = RequestMethod.GET)
	public @ResponseBody
	BackupRestoreStatus getStatus(
			@RequestParam(required = true) String daemonId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		return serviceProvider.getMigrationService().getStatus(userId, daemonId);
	}
	
	/**
	 * The list of primary migration types represents types that either stand-alone or are the owner's of other types.
	 * Migration is driven off this list as secondary types are migrated with their primary owners.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_PRIMARY }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeList getPrimaryTypes(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getPrimaryTypes(userId);
	}
	
}
