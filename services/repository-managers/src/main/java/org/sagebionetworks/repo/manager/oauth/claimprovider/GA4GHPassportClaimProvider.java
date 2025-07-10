package org.sagebionetworks.repo.manager.oauth.claimprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.oauth.GA4GHByType;
import org.sagebionetworks.repo.model.oauth.GA4GHVisa;
import org.sagebionetworks.repo.model.oauth.GA4GHVisaPayload;
import org.sagebionetworks.repo.model.oauth.GA4GHVisaType;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
/*
 * From
 * https://github.com/ga4gh-duri/ga4gh-duri.github.io/tree/master/researcher_ids
 * 
 * The login token request contains an OIDC scope of "ga4gh_passport_v1" to indicate that it 
 * wishes to have a Passport accessible by presenting the access token.
 * 
 * The Passport Broker asks the user which Passport Visas the researcher wishes to release 
 * to the downstream system (Passport Clearinghouse) that wants to use the Passport.
 * 
 * The Passport Broker packages up all the Passport Visas the researcher wishes to release 
 * and mints an OIDC access token, and signs the token with the Passport Broker's private key.
 * This signature will be used by downstream systems to verify the authenticity of the Passport 
 * and maintain its integrity (i.e. prevents any party from tampering with the contents).
 * 
 * --
 * 
 * To implement the above, we require that the authorization request contain 
 * (1) the "ga4gh_passport_v1" scope and,
 * (2) a claim which is a list of access requirements of interest.
 * Each access requirement is of the form https://{host}/repo/v1/accessRequirement/{arId}
 * where {arId} is the id of the access requirement of interest.
 * 
 * For those access requirements for which the user is approved, a GA4GH Visa will be included
 * in the array returned in the "ga4gh_passport_v1" claim of the OIDC user-info.
 */
public class GA4GHPassportClaimProvider implements OIDCClaimProvider {
	// The source organization’s information system has made the assertion based on system data or metadata that it stores.
	// from https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md
	private static final String ACCESS_REQUIREMENT_CLAIM = "%s/repo/v1/accessRequirement/%s";
	private static final Pattern DETAIL_FORMAT = Pattern.compile("/accessRequirement/([0-9]*)$");
	private static final long VISA_EXPIRATION_SECONDS = 3600*24L; // a day
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;

	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	@Autowired
	private Clock clock;
	
	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.ga4gh_passport_v1;
	}

	@Override
	public String getDescription() {
		return "To see your GA4GH Passport, indicating data access approvals you have been granted";
	}
	
	static String createVisaValueFromARId(String baseUri, String arId) {
		return String.format(ACCESS_REQUIREMENT_CLAIM, baseUri, arId);
	}
	
	static String getArIdFromDetail(String detail) {
		Matcher matcher = DETAIL_FORMAT.matcher(detail);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Illegal detail for access requirement: "+detail);		
		}
		return matcher.group(1);
	}
	
	GA4GHVisaPayload getVisaForAccessRequirement(String arId, String subject, String concreteType, String oauthEndpoint) {
		GA4GHVisaPayload result = new GA4GHVisaPayload();
		result.setIss(oauthEndpoint);
		long issuedAtSeconds = clock.currentTimeMillis()/1000L;
		result.setIat(issuedAtSeconds);
		result.setExp(issuedAtSeconds+VISA_EXPIRATION_SECONDS);
		result.setSub(subject);
		GA4GHVisa visa = new GA4GHVisa();
		result.setGa4gh_visa_v1(visa);
		visa.setSource(oauthEndpoint);
		String baseUri=oauthEndpoint.replace("/auth/v1","");
		visa.setValue(createVisaValueFromARId(baseUri, arId));
		visa.setAsserted(issuedAtSeconds);
		if (SelfSignAccessRequirement.class.getName().equals(concreteType)) {
			visa.setType(GA4GHVisaType.AcceptedTermsAndPolicies);	
			visa.setBy(GA4GHByType.self);
		} else if (ACTAccessRequirement.class.getName().equals(concreteType) || 
				ManagedACTAccessRequirement.class.getName().equals(concreteType)) {
			visa.setType(GA4GHVisaType.ControlledAccessGrants);
			visa.setBy(GA4GHByType.dac);
		} else {
			throw new IllegalArgumentException("Unexpected AccessRequirement type: "+concreteType);
		}
		return result;
	}

	@Override
	public Object getClaim(String userId, String subject, OIDCClaimsRequestDetails details, String oauthEndpoint) {
		if (details==null) {
			return Collections.EMPTY_LIST;
		}
		List<String> requestedArIds = new ArrayList<String>();
		if (StringUtils.isNotEmpty(details.getValue())) {
			requestedArIds.add(getArIdFromDetail(details.getValue()));
		}
		if (details.getValues()!=null) {
			for (String detail : details.getValues()) {
				requestedArIds.add(getArIdFromDetail(detail));
			}
		}
		Set<String> approvedArIds = accessApprovalDao.getRequirementsUserHasApprovals(userId, requestedArIds);
		Map<String, String> accessRequirementTypes = accessRequirementDao.getConcreteTypes(approvedArIds);
		List<Object> result = new ArrayList<>(approvedArIds.size());
		for (Map.Entry<String, String> entry : accessRequirementTypes.entrySet()) {
			result.add(getVisaForAccessRequirement(entry.getKey(), subject, entry.getValue(), oauthEndpoint));
		}
		return result;
	}

}
