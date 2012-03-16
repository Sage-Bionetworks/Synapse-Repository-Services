package org.sagebionetworks.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import static org.sagebionetworks.repo.model.AuthorizationConstants.TERMS_OF_USE_AGREEMENT_URI;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCEPTS_TERMS_OF_USE_ATTRIBUTE;

@Controller
public class TermsOfUseAgreementController extends BaseController {
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = TERMS_OF_USE_AGREEMENT_URI, method = RequestMethod.POST)
	public @ResponseBody TermsOfUseAgreement createAgreement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody TermsOfUseAgreement agreement
			) throws Exception {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userId)) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Anonymous request is not allowed.", null);
		Map<String,Collection<String>> attributes = new HashMap<String,Collection<String>>();
		attributes.put(ACCEPTS_TERMS_OF_USE_ATTRIBUTE, Arrays.asList(new String[]{""+agreement.isAgrees()}));
		CrowdAuthUtil.setUserAttributes(userId, attributes);
		return agreement;
	}
	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = TERMS_OF_USE_AGREEMENT_URI, method = RequestMethod.GET)
	public @ResponseBody TermsOfUseAgreement getAgreement(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userId)) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Anonymous request is not allowed.", null);
		TermsOfUseAgreement agreement = new TermsOfUseAgreement();
		// get the info from Crowd
		Map<String,Collection<String>> attributes = CrowdAuthUtil.getUserAttributes(userId);
		Collection<String> values = attributes.get(ACCEPTS_TERMS_OF_USE_ATTRIBUTE);
		// set the info in agreement
		agreement.setAgrees(values!=null && values.size()>0 && Boolean.parseBoolean(values.iterator().next()));
		return agreement;
	}


}
