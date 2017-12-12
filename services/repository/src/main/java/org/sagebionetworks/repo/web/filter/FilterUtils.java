package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;

/**
 * Helper functions for filters.
 *
 */
public class FilterUtils {

	/**
	 * Send a Failed response status.
	 * @param req
	 * @param resp
	 * @param reason
	 * @param status
	 * @throws IOException
	 */
	public static void sendFailedResponse(HttpServletResponse resp, String reason, HttpStatus status) throws IOException  {
		resp.setStatus(status.value());
		ErrorResponse er = new ErrorResponse();
		er.setReason(reason);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		try {
			er.writeToJSONObject(joa);
			resp.getWriter().println(joa.toJSONString());
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(reason, e);
		}
	}
}
