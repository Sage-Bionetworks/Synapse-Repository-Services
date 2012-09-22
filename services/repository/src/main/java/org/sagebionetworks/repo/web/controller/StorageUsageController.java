package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * Retrieves the storage usage data for a user.
 *
 * @author ewu
 */
@Controller
public class StorageUsageController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Retrieves the aggregated usage for the current user. Aggregation is done over the supplied dimensions,
	 * for example, ['storage_provider', 'content_type']. If no dimension is passed, the returned results
	 * will contain only the grand total.
	 *
	 * @param sd1
	 *			First dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @param sd2
	 *			Second dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @param sd3
	 *			Third dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @throws IllegalArgumentException
	 *			When the supplied list of aggregating dimensions has invalid values. See StorageUsageDimension for valid values.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getStorageUsage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String currUserId,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_1_PARAM, required = false) String sd1,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_2_PARAM, required = false) String sd2,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_3_PARAM, required = false) String sd3)
			throws IllegalArgumentException, DatastoreException {

		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		addDimension(sd1, dList);
		addDimension(sd2, dList);
		addDimension(sd3, dList);

		StorageUsageService service = serviceProvider.getStorageUsageService();
		StorageUsageSummaryList storageSummaries = service.getStorageUsage(currUserId, currUserId, dList);
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
	 * @param sd1
	 *			First dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @param sd2
	 *			Second dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @param sd3
	 *			Third dimension for aggregating the numbers. This must be a valid value of the StorageUsageDimension enum.
	 * @throws IllegalArgumentException
	 *			When the supplied list of aggregating dimensions has invalid values. See StorageUsageDimension for valid values.
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY_USER_ID, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getStorageUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) String userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String currUserId,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_1_PARAM, required = false) String sd1,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_2_PARAM, required = false) String sd2,
			@RequestParam(value = ServiceConstants.STORAGE_DIMENSION_3_PARAM, required = false) String sd3)
			throws IllegalArgumentException, DatastoreException, UnauthorizedException {

		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		addDimension(sd1, dList);
		addDimension(sd2, dList);
		addDimension(sd3, dList);

		StorageUsageService service = serviceProvider.getStorageUsageService();
		StorageUsageSummaryList storageSummaries = service.getStorageUsage(currUserId, userId, dList);
		return storageSummaries;
	}

	/**
	 * Retrieves detailed, itemized usage for the current user.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String currUserId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws DatastoreException {
		String url = request.getServletPath() + UrlHelpers.STORAGE_DETAILS; // XXX: Need a better way to wire in the URL
		StorageUsageService service = serviceProvider.getStorageUsageService();
		PaginatedResults<StorageUsage> results = service.getStorageUsage(currUserId, currUserId, offset, limit, url);
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS_USER_ID, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) String userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String currUserId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws DatastoreException, UnauthorizedException {
		String url = request.getServletPath() + UrlHelpers.STORAGE_DETAILS_USER_ID; // XXX: Need a better way to wire in the URL
		StorageUsageService service = serviceProvider.getStorageUsageService();
		PaginatedResults<StorageUsage> results = service.getStorageUsage(currUserId, userId, offset, limit, url);
		return results;
	}

	/**
	 * Reads the dimension enum and adds it to the list. Null or empty string will be skipped.
	 *
	 * @throws IllegalArgumentException If the dimension is not a valid dimension
	 */
	private void addDimension(String d, List<StorageUsageDimension> dList)
			throws IllegalArgumentException {
		if (d != null && d.length() > 0) {
			dList.add(StorageUsageDimension.valueOf(d.toUpperCase()));
		}
	}
}
