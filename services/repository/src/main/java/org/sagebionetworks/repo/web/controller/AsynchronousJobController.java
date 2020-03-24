package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * This is a generic set of services that provides support for both launching
 * asynchronous jobs and monitoring the progress of jobs.
 * 
 * 
 */
@ControllerInfo(displayName = "Asynchronous Job Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class AsynchronousJobController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * <p>
	 * This method is used to launch new jobs. The type of job that will be launched is determined by the passed
	 * AsynchronousJobBody.
	 * </p>
	 * The following are the currently supported job types:
	 * <ul>
	 * <li><a href="${org.sagebionetworks.repo.model.table.UploadToTableRequest}">UploadToTableRequest</a></li>
	 * <li><a href="${org.sagebionetworks.repo.model.table.DownloadFromTableRequest}">DownloadFromTableRequest</a></li>
	 * </ul>
	 * <p>
	 * Note: Each job types has different access requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param body There is a AsynchronousJobBody implementation for each job type. This body determines the type of job
	 *        that will be launched.
	 * @return Each new job launched will have a unique jobId that can be use to monitor the status of the job with
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ASYNCHRONOUS_JOB, method = RequestMethod.POST)
	public @ResponseBody
	AsynchronousJobStatus launchNewJob(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AsynchronousRequestBody body) throws NotFoundException {
		return serviceProvider.getAsynchronousJobServices().startJob(userId, body);
	}
	
	/**
	 * Once a job is launched its progress can be monitored by getting its status with this method.
	 * 
	 * @param userId
	 * @param jobId The jobId issued to a job that has been launched with <a href="${POST.asynchronous.job}">POST
	 *        /asynchronous/job</a>
	 * @return
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ASYNCHRONOUS_JOB_ID, method = RequestMethod.GET)
	public @ResponseBody
	AsynchronousJobStatus getJobStatus(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String jobId)
			throws NotFoundException, AsynchJobFailedException, NotReadyException {
		return serviceProvider.getAsynchronousJobServices().getJobStatus(userId, jobId);
	}

	/**
	 * Once a job is launched it can be cancelled if the job is set up to be cancelable.
	 * 
	 * @param userId
	 * @param jobId The jobId issued to a job that has been launched with <a href="${POST.asynchronous.job}">POST
	 *        /asynchronous/job</a>
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.ASYNCHRONOUS_JOB_CANCEL, method = RequestMethod.GET)
	public void stopJob(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String jobId)
			throws NotFoundException, AsynchJobFailedException, NotReadyException {
		serviceProvider.getAsynchronousJobServices().cancelJob(userId, jobId);
	}
}
