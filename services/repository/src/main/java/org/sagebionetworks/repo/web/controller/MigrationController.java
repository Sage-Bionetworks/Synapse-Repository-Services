package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationTypeNames;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
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
@RequestMapping(UrlHelpers.REPO_PATH)
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
	 * Get the counts for a migration type.
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MIGRATION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeCount getTypeCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required=true) String type)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getTypeCount(userId, MigrationType.valueOf(type));
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

	/**
	 * The list of primary migration type names
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_PRIMARY_NAMES }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeNames getPrimaryTypeNames(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getPrimaryTypeNames(userId);
	}

	/**
	 * The list of  migration types.
	 * @param userId
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_TYPES }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeList getMigrationTypes(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getMigrationTypes(userId);
	}

	/**
	 * The list of primary migration type names
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_TYPE_NAMES }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeNames getMigrationTypeNames(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, NotFoundException {
		return serviceProvider.getMigrationService().getMigrationTypeNames(userId);
	}

	/**
	 * A checksum on ETAG or backup ID for a given range and a given migration type
	 * @throws NotFoundException 
	 */	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_RANGE_CHECKSUM }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationRangeChecksum getChecksumForIdRange(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String migrationType,
			@RequestParam(required = true) String salt,
			@RequestParam(required = true) Long minId,
			@RequestParam(required = true) Long maxId) throws NotFoundException {
		return serviceProvider.getMigrationService().getChecksumForIdRange(userId, MigrationType.valueOf(migrationType), salt, minId, maxId);
	}
	
	/**
	 * A (table) checksum on a given migration type
	 * @throws NotFoundException 
	 */	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MIGRATION_TYPE_CHECKSUM }, method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeChecksum getChecksumForType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String migrationType) throws NotFoundException {
		return serviceProvider.getMigrationService().getChecksumForType(userId, MigrationType.valueOf(migrationType));
	}
	
	
}
