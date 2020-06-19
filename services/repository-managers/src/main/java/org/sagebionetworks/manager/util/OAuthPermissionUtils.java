package org.sagebionetworks.manager.util;

import org.sagebionetworks.repo.model.oauth.OAuthScope;

public class OAuthPermissionUtils {
	public static String scopeDescription(OAuthScope scope) {
		switch (scope) {
		case openid:
			return "To see your identity";
		case view:
			return "To view the content which you can view";
		case modify:
			return "To modify the content which you can modify (create, change, delete)";
		case download:
			return "To download the content which you can download";
		case authorize:
			return "To authorize others to access to resources you control";
		case offline_access:
			return "To access the resources authorized here when you are not logged in, until you revoke access from this application.";
		}
		return null;
	}
}
