package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TODO
 * <ol>
 * <li>have unit tests use servlet path
 * <li>use a maven profile to switch to integration test config?
 * <li>have a way to explicitly run integration tests via some main method?
 * <li>configuration (1) which servletImpl (2) service endpoint (3) servlet path
 * <li>get wiki output to work too
 * <li>turn up http client logging
 * </ol>
 * 
 * @author deflaux
 * 
 */
public class IntegrationTestMockServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final HttpClient webClient;

	private final String serviceEndpoint;

	static {
		final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		webClient = new HttpClient(connectionManager);
		webClient.getHttpConnectionManager().getParams().setSoTimeout(5000);
		webClient.getHttpConnectionManager().getParams().setConnectionTimeout(
				5000);
	}

	/**
	 * @param serviceEndpoint
	 */
	public IntegrationTestMockServlet(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	/**
	 * This assumes we are always dealing with String content
	 */
	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {

		MockHttpServletRequest request = (MockHttpServletRequest) req;
		MockHttpServletResponse response = (MockHttpServletResponse) res;

		String path = serviceEndpoint + request.getRequestURI();

		HttpMethodBase method = null;
		if (request.getMethod().equals("GET")) {
			method = new GetMethod(path);
		} else if (request.getMethod().equals("POST")) {
			method = new PostMethod(path);
			if (null != request.getInputStream()) {
				((EntityEnclosingMethod) method)
						.setRequestEntity(new InputStreamRequestEntity(request
								.getInputStream()));
			}
		} else if (request.getMethod().equals("PUT")) {
			method = new PutMethod(path);
			if (null != request.getInputStream()) {
				((EntityEnclosingMethod) method)
						.setRequestEntity(new InputStreamRequestEntity(request
								.getInputStream()));
			}
		} else if (request.getMethod().equals("DELETE")) {
			method = new DeleteMethod(path);
		}

		// Copy query string
		Map<String, String[]> parameterMap = request.getParameterMap();
		StringBuilder queryString = new StringBuilder();
		for (Entry<String, String[]> parameter : parameterMap.entrySet()) {
			for (String value : parameter.getValue()) {
				queryString.append(parameter.getKey());
				queryString.append("=");
				queryString.append(URLEncoder.encode(value, "UTF-8"));
				queryString.append("&");
			}
		}
		method.setQueryString(queryString.toString());

		// Copy headers
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			method.addRequestHeader(headerName, request.getHeader(headerName));
		}

		int responseCode = webClient.executeMethod(null, method);

		response.setStatus(responseCode);
		Header headers[] = method.getResponseHeaders();
		for (Header header : headers) {
			response.setHeader(header.getName(), header.getValue());
		}

		String result = method.getResponseBodyAsString();
		if (null != result) {
			PrintWriter writer = response.getWriter();
			writer.write(method.getResponseBodyAsString());
		}
	}
}
