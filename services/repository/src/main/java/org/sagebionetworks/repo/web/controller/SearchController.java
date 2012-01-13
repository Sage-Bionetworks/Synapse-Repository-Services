package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.json.JSONException;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author deflaux
 * 
 */
@Controller
public class SearchController extends BaseController {

	private static final Logger log = Logger.getLogger(SearchController.class
			.getName());

	// TODO make sure we allow many connections to this one host
	private static final HttpClient httpClient = HttpClientHelper
			.createNewClient(true);

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { "/search" }, method = RequestMethod.GET)
	public ModelAndView proxySearch(
			@RequestParam(value = "q", required = false) String q,
			HttpServletRequest request) throws ClientProtocolException,
			IOException, HttpClientHelperException, JSONException {

		// TODO authorization

		String url = StackConfiguration.getSearchServiceEndpoint() + "?"
				+ q;
		log.log(Level.FINE, "Got query |"+ q +"|, about to request |"+ url+"|");
		
		String response = HttpClientHelper.getFileContents(httpClient, url);

		// TODO make a response pojo in the pojo2schema project
		ModelAndView mav = new ModelAndView();
		mav.addObject("result", response);
		mav.addObject("url", url);
		return mav;
	}
}
