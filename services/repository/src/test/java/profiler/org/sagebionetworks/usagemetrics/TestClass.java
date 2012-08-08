package profiler.org.sagebionetworks.usagemetrics;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A stub-class that is here solely to help in testing the functionality of the ActivityLogger.
 * @author geoff
 *
 */
public class TestClass {

	@Test
	public void fakeTest() {

	}

	public void testMethod(String arg1, Integer arg2) {
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.ENTITY_ID},
	method = RequestMethod.GET)
	public @ResponseBody String testAnnotationsMethod(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false, defaultValue = "") String userId) {
		return "";
	}
}
