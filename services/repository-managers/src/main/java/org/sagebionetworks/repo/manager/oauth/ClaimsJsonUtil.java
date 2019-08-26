package org.sagebionetworks.repo.manager.oauth;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import net.minidev.json.JSONObject;

public class ClaimsJsonUtil {
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getClaimsMapFromJSONObject(JSONObject claimsJson, boolean ignoreUnknownClaims) {
		Map<OIDCClaimName, OIDCClaimsRequestDetails> result = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		if (claimsJson==null) return result;
		for (String claimName : claimsJson.keySet()) {
			OIDCClaimName claim;
			try {
				claim = OIDCClaimName.valueOf(claimName);
			} catch (IllegalArgumentException e) {	
				if (ignoreUnknownClaims) {
					continue;
				} else {
					throw e;
				}
			}
			String detailsString = claimsJson.getAsString(claimName);
			OIDCClaimsRequestDetails details = null;
			if (detailsString!=null) {
				details = new OIDCClaimsRequestDetails();
				try {
					JSONObjectAdapter adapter = new JSONObjectAdapterImpl(detailsString);
					details.initializeFromJSONObject(adapter);
				} catch (JSONObjectAdapterException e) {
					throw new IllegalArgumentException(e);
				}
			}
			result.put(claim, details);
		}
		return result;
	}
}
