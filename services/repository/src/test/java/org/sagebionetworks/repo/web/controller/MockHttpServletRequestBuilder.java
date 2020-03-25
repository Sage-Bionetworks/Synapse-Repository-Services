package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.springframework.mock.web.MockHttpServletRequest;

public class MockHttpServletRequestBuilder {
	private String method;
	private List<NameValuePair> headers;
	private List<NameValuePair> parameters;
	private String uri;
	private byte[] content;
	
	public MockHttpServletRequestBuilder() {
		headers = new ArrayList<NameValuePair>();
		parameters = new ArrayList<NameValuePair>();
	}
	public String getMethod() {
		return method;
	}
	public MockHttpServletRequestBuilder withMethod(String method) {
		this.method = method;
		return this;
	}
	public List<NameValuePair> getHeaders() {
		return headers;
	}

	public MockHttpServletRequestBuilder withHeader(final String name, final String value) {
		headers.add(new NameValuePair() {
			@Override
			public String getName() {return name;}

			@Override
			public String getValue() {return value;}
		});
		return this;
	}
	
	public List<NameValuePair> getParameters() {
		return parameters;
	}

	public MockHttpServletRequestBuilder withParameter(final String name, final String value) {
		parameters.add(new NameValuePair() {
			@Override
			public String getName() {return name;}

			@Override
			public String getValue() {return value;}
		});
		return this;
	}
	
	public String getUri() {
		return uri;
	}
	public MockHttpServletRequestBuilder withUri(String uri) {
		this.uri = uri;
		return this;
	}
	public byte[] getContent() {
		return content;
	}
	public MockHttpServletRequestBuilder withContent(byte[] content) {
		this.content = content;
		return this;
	}
	
	public MockHttpServletRequest build() {
		MockHttpServletRequest result = new MockHttpServletRequest();
		result.setMethod(method);
		for (NameValuePair h : headers) {
			result.addHeader(h.getName(), h.getValue());
		}
		for (NameValuePair p : parameters) {
			result.addParameter(p.getName(), p.getValue());
		}
		result.setRequestURI(uri);
		result.setContent(content);
		return result;
	}

}
