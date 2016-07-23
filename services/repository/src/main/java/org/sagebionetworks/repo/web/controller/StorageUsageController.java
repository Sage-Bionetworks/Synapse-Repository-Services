package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.DeprecatedServiceException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 * 
 * Update about deprecation: This feature was built so that we can put a limit
 * on how much users can store in Synapse. We abort the project, and have no
 * supports on this feature. It's reporting the wrong number and is not useful.
 * Also, it put a heavy load on the DB and need to be removed.
 * 
 */
@Deprecated
@ControllerInfo(displayName="Storage Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class StorageUsageController extends BaseController {

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForCurrentUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_SUMMARY_USER_ID, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) Long userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long currentUserId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_STORAGE_SUMMARY, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageForAdmin(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.AGGREGATION_DIMENSION, required = false) String aggregation,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsageForCurrentUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STORAGE_DETAILS_USER_ID, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<StorageUsage> getItemizedStorageUsageForUser(
			@PathVariable(value = UrlHelpers.STORAGE_USER_ID) Long userId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long currentUserId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}

	/**
	 * This method is deprecated and should not be used.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_STORAGE_SUMMARY_PER_USER, method = RequestMethod.GET)
	public @ResponseBody StorageUsageSummaryList getUsageByUserForAdmin(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request)
			throws DeprecatedServiceException {

		throw new DeprecatedServiceException();
	}
}
