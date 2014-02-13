package org.sagebionetworks.bridge.controller;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.bridge.BridgeUrlHelpers;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesTable;
import org.sagebionetworks.bridge.service.BridgeServiceProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ControllerInfo(displayName = "Participant data", path = BridgeUrlHelpers.BASE_V1)
@Controller
public class TimeSeriesController {

	private static final Logger logger = LogManager.getLogger(TimeSeriesController.class.getName());

	@Autowired
	BridgeServiceProvider serviceProvider;

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.TIME_SERIES, method = RequestMethod.GET)
	public @ResponseBody
	TimeSeriesTable getTimeSeries(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@RequestParam(value = BridgeUrlHelpers.COLUMNT_NAME, required = false) String[] columnNames,
			@PathVariable String participantDataDescriptorId) throws Exception {
		return serviceProvider.getTimeSeriesService().getTimeSeries(userId, participantDataDescriptorId,
				columnNames == null ? null : Arrays.asList(columnNames));
	}
}
