package org.sagebionetworks.authutil;

import java.util.List;
import java.util.Map;

public class OpenIDInfo {
	private String identifier;
	private Map<String, List<String>> map;
	
	public static final String DISCOVERY_INFO_PARAM_NAME = "org.sagebionetworks.auth.discoveryInfo";
	public static final String ACCEPTS_TERMS_OF_USE_PARAM_NAME = "org.sagebionetworks.auth.acceptsTermsOfUse";
	public static final String RETURN_TO_URL_PARAM_NAME = "org.sagebionetworks.auth.returnToUrl";
	public static final String REDIRECT_MODE_PARAM_NAME = "org.sagebionetworks.auth.redirectMode";

	public OpenIDInfo() { }

	public OpenIDInfo(String i, Map<String, List<String>> m) {
		identifier = i;
		map = m;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier
	 *            the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the map
	 */
	public Map<String, List<String>> getMap() {
		return map;
	}

	/**
	 * @param map
	 *            the map to set
	 */
	public void setMap(Map<String, List<String>> map) {
		this.map = map;
	}

}
