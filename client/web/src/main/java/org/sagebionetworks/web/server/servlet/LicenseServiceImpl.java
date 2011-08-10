package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceService;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.WhereCondition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

public class LicenseServiceImpl extends RemoteServiceServlet implements LicenceService {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(LicenseServiceImpl.class.getName());
	
	private SearchServiceImpl searchService;
	/**
	 * The template is injected with Gin
	 */
	private RestTemplateProvider templateProvider;

	/**
	 * Injected with Gin
	 */
	private ColumnConfigProvider columnConfig;
	
	/**
	 * Injected with Gin
	 */
	private ServiceUrlProvider urlProvider;
	

	/**
	 * Injected via Gin.
	 * 
	 * @param template
	 */
	@Inject
	public void setRestTemplate(RestTemplateProvider template) {
		this.templateProvider = template;
	}


	/**
	 * Injected via Gin
	 * 
	 * @param columnConfig
	 */
	@Inject
	public void setColunConfigProvider(ColumnConfigProvider columnConfig) {
		this.columnConfig = columnConfig;
	}
	
	/**
	 * Injected vid Gin
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
	}
	
	/**
	 * Validate that the service is ready to go. If any of the injected data is
	 * missing then it cannot run. Public for tests.
	 */
	public void validateService() {
		if (templateProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider was not injected into this service");
		if (templateProvider.getTemplate() == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider returned a null template");
		if (urlProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.rest.api.root.url was not set");
	}



	/**
	 * Returns true if the user has accepted the license agreement for the given dataset 
	 * @return true for accepted, false otherwise 
	 */
	@Override
	public boolean hasAccepted(String username, String eulaId, String datasetId) {
		// query for license acceptance
		// First make sure the service is ready to go.
		validateService();

		List<WhereCondition> where = new ArrayList<WhereCondition>();
		where.add(new WhereCondition("datasetId", WhereOperator.EQUALS, datasetId));
		where.add(new WhereCondition("eulaId", WhereOperator.EQUALS, eulaId));
		where.add(new WhereCondition("createdBy", WhereOperator.EQUALS, username));
		List<String> select = new ArrayList<String>();
		select.add("*");
		
		// Build the uri from the parameters
		SearchParameters params = new SearchParameters(select, ObjectType.agreement.name(), where, 1, 10, null, false);

		// Build the uri from the parameters
		URI uri = QueryStringUtils.writeQueryUri(urlProvider.getBaseUrl() + "/", params);

		logger.info("GET: " + uri.toASCIIString());

		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);

		// Make the actual call.
		try {
			ResponseEntity<Object> response = templateProvider.getTemplate().exchange(uri, HttpMethod.GET, entity, Object.class);
			LinkedHashMap<String, Object> body = (LinkedHashMap<String, Object>) response.getBody();
			Integer numResults = (Integer) body.get(SearchService.KEY_TOTAL_NUMBER_OF_RESULTS);
			if(numResults > 0) {
				return true;
			} 			
		} catch (Exception ex) {
			logger.severe(ex.getMessage());
		}				
		return false;
	}


	@Override
	public void logUserDownload(String username, String objectUri, String fileUri) {
		throw new NotImplementedException();
	}

}


