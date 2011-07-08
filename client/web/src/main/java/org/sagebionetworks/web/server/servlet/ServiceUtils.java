package org.sagebionetworks.web.server.servlet;

import org.sagebionetworks.web.shared.NodeType;

public class ServiceUtils {

	public static final String PATH_DATASET = "dataset";
	public static final String PATH_LAYER = "layer";
	public static final String PATH_PROJECT = "project";
	public static final String ANNOTATIONS_PATH = "annotations";
	public static final String PREVIEW_PATH = "preview";
	public static final String LOCATION_PATH = "location";
	
	public static final String PATH_SCHEMA = "schema";
	public static final String PATH_ACL = "acl"; 	

	
	public static StringBuilder getBaseUrlBuilder(ServiceUrlProvider urlProvider, NodeType type) {
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl() + "/");
		// set path based on type
		switch(type) {
		case DATASET:
			builder.append(PATH_DATASET);
			break;
		case PROJECT:
			builder.append(PATH_PROJECT);
			break;
		case LAYER:
			builder.append(PATH_LAYER);
			break;
		default:
			throw new IllegalArgumentException("Unsupported type:" + type.toString());
		}
		return builder;
	}
	
}
