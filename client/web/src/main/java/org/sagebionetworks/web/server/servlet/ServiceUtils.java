package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.web.shared.NodeType;
import org.springframework.web.client.HttpClientErrorException;

public class ServiceUtils {

	private static final String ERROR_REASON = "reason";

	private static Logger logger = Logger.getLogger(ServiceUtils.class.getName());

	public static final String REPOSVC_PATH_DATASET = "dataset";
	public static final String REPOSVC_PATH_LAYER = "layer";
	public static final String REPOSVC_PATH_PROJECT = "project";
	public static final String REPOSVC_PATH_EULA = "eula";
	public static final String REPOSVC_PATH_AGREEMENT = "agreement";
	public static final String REPOSVC_PATH_ENTITY = "entity";
	public static final String REPOSVC_PATH_HAS_ACCESS = "access";
	public static final String REPOSVC_PATH_GET_USERS = "user";
	public static final String REPOSVC_PATH_ANALYSIS = "analysis";
	public static final String REPOSVC_PATH_STEP = "step";
	public static final String REPOSVC_SUFFIX_PATH_ANNOTATIONS = "annotations";
	public static final String REPOSVC_SUFFIX_PATH_PREVIEW = "preview";
	public static final String REPOSVC_SUFFIX_LOCATION_PATH = "location";	
	public static final String REPOSVC_SUFFIX_PATH_SCHEMA = "schema";
	public static final String REPOSVC_SUFFIX_PATH_ACL = "acl"; 	
	public static final String REPOSVC_SUFFIX_PATH_TYPE = "type";
	public static final String REPOSVC_SUFFIX_PATH_BENEFACTOR = "benefactor"; 
	
	public static final String AUTHSVC_SEND_PASSWORD_CHANGE_PATH = "userPasswordEmail";
	public static final String AUTHSVC_SEND_API_PASSWORD_PATH = "apiPasswordEmail";
	public static final String AUTHSVC_SET_PASSWORD_PATH = "userPassword";
	public static final String AUTHSVC_INITIATE_SESSION_PATH = "session";
	public static final String AUTHSVC_CREATE_USER_PATH = "user";
	public static final String AUTHSVC_GET_USER_PATH = "user";
	public static final String AUTHSVC_UPDATE_USER_PATH = "user";
	public static final String AUTHSVC_TERMINATE_SESSION_PATH = "session";
	public static final String AUTHSVC_REFRESH_SESSION_PATH = "session";
	public static final String AUTHSVC_GET_GROUPS_PATH = "userGroup";
	
	public static final String AUTHSVC_ACL_PRINCIPAL_NAME = "name";
	public static final String AUTHSVC_ACL_PRINCIPAL_ID = "id";
	public static final String AUTHSVC_ACL_PRINCIPAL_CREATION_DATE = "creationDate";
	public static final String AUTHSVC_ACL_PRINCIPAL_URI = "uri";
	public static final String AUTHSVC_ACL_PRINCIPAL_ETAG = "etag";
	public static final String AUTHSVC_ACL_PRINCIPAL_INDIVIDUAL = "individual";
	
	public static StringBuilder getBaseUrlBuilder(ServiceUrlProvider urlProvider, NodeType type) {
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getRepositoryServiceUrl() + "/");
		// set path based on type
		switch(type) {
		case DATASET:
			builder.append(REPOSVC_PATH_DATASET);
			break;
		case PROJECT:
			builder.append(REPOSVC_PATH_PROJECT);
			break;
		case LAYER:
			builder.append(REPOSVC_PATH_LAYER);
			break;
		case EULA:
			builder.append(REPOSVC_PATH_EULA);
			break;
		case AGREEMENT:
			builder.append(REPOSVC_PATH_AGREEMENT);
			break;
		case ANALYSIS:
			builder.append(REPOSVC_PATH_ANALYSIS);
			break;
		case STEP:
			builder.append(REPOSVC_PATH_STEP);
			break;
		case ENTITY:
			builder.append(REPOSVC_PATH_ENTITY);
			break;
		default:
			throw new IllegalArgumentException("Unsupported type:" + type.toString());
		}
		return builder;
	}

	
	public static String handleHttpClientErrorException(HttpClientErrorException ex) {
	//		if(ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
	//		throw new UnauthorizedException();
	//	} else if(ex.getStatusCode() == HttpStatus.FORBIDDEN) {
	//		throw new ForbiddenException();
	//	} else {
	//		throw new UnknownError("Status code:" + ex.getStatusCode().value());
	//	}
		
		// temporary solution to not being able to throw caught exceptions (due to Gin 1.0)
		JSONObject obj = new JSONObject();
		JSONObject errorObj = new JSONObject();
		try {
			Integer code = ex.getStatusCode().value(); 
			if(code != null) errorObj.put("statusCode", code);
			obj.put("error", errorObj);
			
		} catch (JSONException e) {
			throw new UnknownError();
		}
		
		String body = ex.getResponseBodyAsString();
		JSONObject reasonObj;
		try {
			reasonObj = new JSONObject(body);
			if(reasonObj.has(ERROR_REASON)) {
				String message = reasonObj.getString(ERROR_REASON);
				logger.info("Error Reason: " + message);
				obj.put(ERROR_REASON, message);
			}
		} catch (JSONException e) {
			logger.info(e.getMessage());			
		}
		return obj.toString();
	}
}
