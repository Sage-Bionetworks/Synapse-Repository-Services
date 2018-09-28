package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.SessionId;
import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerInfo(displayName="SessionServices", path=UrlHelpers.REPO_PATH)
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SessionIdController {

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SESSION_ID, method = RequestMethod.GET)
	public @ResponseBody SessionId getSessionId(){
		SessionId sessionId = new SessionId();
		sessionId.setSessionId(HttpRequestIdentifierUtils.generateSessionId());
		return sessionId;
	}
}
