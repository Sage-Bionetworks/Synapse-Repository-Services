package org.sagebionetworks.web.server.servlet;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.sagebionetworks.web.shared.NodeType;
import org.springframework.web.client.HttpClientErrorException;

public class ServiceUtils {

	public static final String REPOSVC_PATH_DATASET = "dataset";
	public static final String REPOSVC_PATH_LAYER = "layer";
	public static final String REPOSVC_PATH_PROJECT = "project";
	public static final String REPOSVC_PATH_EULA = "eula";
	public static final String REPOSVC_PATH_AGREEMENT = "agreement";
	public static final String REPOSVC_ANNOTATIONS_PATH = "annotations";
	public static final String REPOSVC_PREVIEW_PATH = "preview";
	public static final String REPOSVC_LOCATION_PATH = "location";	
	public static final String REPOSVC_PATH_SCHEMA = "schema";
	public static final String REPOSVC_PATH_ACL = "acl"; 	
	public static final String REPOSVC_HAS_ACCESS_PATH = "access";
	public static final String REPOSVC_GET_USERS_PATH = "user";
	
	public static final String AUTHSVC_SEND_PASSWORD_CHANGE_PATH = "userPasswordEmail";
	public static final String AUTHSVC_SET_PASSWORD_PATH = "userPassword";
	public static final String AUTHSVC_INITIATE_SESSION_PATH = "session";
	public static final String AUTHSVC_CREATE_USER_PATH = "user";
	public static final String AUTHSVC_GET_USER_PATH = "user";
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
		builder.append(urlProvider.getBaseUrl() + "/");
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
			return obj.toString();
		} catch (JSONException e) {
			throw new UnknownError();
		}
	}
}
