package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.repo.web.service.StorageUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Retrieves storage usage data.
 */
@ControllerInfo(displayName="Storage Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class StorageUsageController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Retrieves the aggregated usage for the current user. Aggregation is done over the supplied dimensions,
	 * for example, ['storage_provider', 'content_type']. If no dimension is passed, the returned results
	 * will contain only the grand total.
	 *
	 * @param aggregation
	 *			Aggregating dimensions/columns. This must be concatenated values of the StorageUsageDimension enum.
	 * @throws IllegalArgumentException
	 *			When the supplied list of aggregating dimensions has invalid values. See StorageUsageDimension for valid values.
	 * @throws NotFoundException
	 *			When the user does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForCurrentUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws IllegalArgumentException, NotFoundException, DatastoreException {

		List<StorageUsageDimension> dList = getAggregatingDimensionList(aggregation);
		StorageUsageService service = serviceProvider.getStorageUsageService();
		StorageUsageSummaryList storageSummaries = service.getUsageForUser(userId, userId, dList);
		return storageSummaries;
	}

	/**
	 * Retrieves the aggregated usage for the specified user. Aggregation is done over the supplied dimensions,
	 * for example, ['storage_provider', 'content_type']. If no dimension is passed, the returned results
	 * will contain only the grand total. The current user must have the privilege (e.g. admin) to view
	 * a different user's storage usage.
	 *
	 * @param userId
	 *			The user whose storage usage is being queried.
	 * @param currUserId
	 *			The current user, the user who is querying the storage usage.
	 * @param aggregation
	 *			Aggregating dimensions/columns. This must be concatenated values of the StorageUsageDimension enum.
	 * @throws IllegalArgumentException
	 *			When the supplied list of aggregating dimensions has invalid values. See StorageUsageDimension for valid values.
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the specified user does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY_USER_ID, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) Long userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long currentUserId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws IllegalArgumentException, UnauthorizedException, NotFoundException, DatastoreException {

		List<StorageUsageDimension> dList = getAggregatingDimensionList(aggregation);
		StorageUsageService service = serviceProvider.getStorageUsageService();
		StorageUsageSummaryList storageSummaries = service.getUsageForUser(currentUserId, userId, dList);
		return storageSummaries;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_STORAGE_SUMMARY, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForAdmin(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws IllegalArgumentException, UnauthorizedException, NotFoundException, DatastoreException {

		List<StorageUsageDimension> dList = getAggregatingDimensionList(aggregation);
		StorageUsageService service = serviceProvider.getStorageUsageService();
		StorageUsageSummaryList storageSummaries = service.getUsage(userId, dList);
		return storageSummaries;
	}

	/**
	 * Retrieves detailed, itemized usage for the current user.
	 *
	 * @throws NotFoundException
	 *			When the specified user does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsageForCurrentUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException {

		String url = request.getServletPath() + UrlHelpers.STORAGE_DETAILS; // XXX: Need a better way to wire in the URL
		StorageUsageService service = serviceProvider.getStorageUsageService();
		PaginatedResults<StorageUsage> results = service.getUsageInRangeForUser(userId, userId, offset, limit, url);
		return results;
	}

	/**
	 * Retrieves detailed, itemized usage for the specified user. The current user must have
	 * the privilege (e.g. being administrator) to view the user's storage usage.
	 *
	 * @param userId
	 *			The user whose storage usage is being queried.
	 * @param currUserId
	 *			The current user, the user who is querying the storage usage.
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the specified user does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS_USER_ID, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) Long userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long currentUserId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws UnauthorizedException, NotFoundException, DatastoreException {

		String url = request.getServletPath() + UrlHelpers.STORAGE_DETAILS_USER_ID;
		StorageUsageService service = serviceProvider.getStorageUsageService();
		PaginatedResults<StorageUsage> results = service.getUsageInRangeForUser(currentUserId, userId, offset, limit, url);
		return results;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_STORAGE_SUMMARY_PER_USER, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageByUserForAdmin(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws UnauthorizedException, NotFoundException, DatastoreException {
		StorageUsageService service = serviceProvider.getStorageUsageService();
		return service.getUsageByUserInRange(userId, offset, limit);
	}

	/**
	 * @throws IllegalArgumentException If the dimension is not a valid dimension
	 */
	private List<StorageUsageDimension> getAggregatingDimensionList(String aggregation)
			throws IllegalArgumentException {

		List<StorageUsageDimension> dimList = new ArrayList<StorageUsageDimension>();
		if (aggregation != null && aggregation.length() > 0) {
			String[] splits = aggregation.split(ServiceConstants.AGGREGATION_DIMENSION_VALUE_SEPARATOR);
			for (String split : splits) {
				// Throws IllegalArgumentException
				StorageUsageDimension d = StorageUsageDimension.valueOf(split.toUpperCase());
				dimList.add(d);
			}
		}

		return dimList;
	}
}
