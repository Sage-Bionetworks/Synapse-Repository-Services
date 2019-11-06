package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.web.UrlHelpers;

/**
 * Test for the URL helper
 * @author jmhill
 *
 */
public class UrlHelperTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestNullRequst(){
		String url = UrlHelpers.getUrlPrefixFromRequest(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestBothNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		when(mockRequest.getServletPath()).thenReturn(null);
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
	}
	
	@Test
	public void testGetUrlPrefixFromRequestContextNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("/repo/v1", url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestPathNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn(null);
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("http://localhost:8080", url);
	}
	
	@Test 
	public void testGetUrlPrefixFromRequestBothNotNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("http://localhost:8080/repo/v1", url);
	}


	@Test
	public void testCreateACLRedirectURL(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String redirectURL = UrlHelpers.createACLRedirectURL(mockRequest, "45");
		assertEquals("http://localhost:8080/repo/v1/entity/45/acl", redirectURL);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateACLRedirectURLNullRequst(){
		String redirectURL = UrlHelpers.createACLRedirectURL(null, "45");
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateACLRedirectURLNullId(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String redirectURL = UrlHelpers.createACLRedirectURL(mockRequest, null);
	}

}
