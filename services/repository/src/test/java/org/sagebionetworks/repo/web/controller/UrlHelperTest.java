package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.VersionableEntity;
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

	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullClass(){
		UrlHelpers.createEntityUri("12", null, "http://localhost:8080/repo/v1");
	}
	
	
	@Test 
	public void testsetEntityUriAllTypes(){
		EntityType[] array = EntityType.values();
		String uriPrefix = "/repo/v1";
		String id = "123";
		for(EntityType type: array){
			String expectedUri = uriPrefix+UrlHelpers.ENTITY+"/"+id;
			String uri = UrlHelpers.createEntityUri(id, EntityTypeUtils.getClassForType(type), "");
			assertEquals(expectedUri, uri);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetBaseUriForEntityNullEntity(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		UrlHelpers.setBaseUriForEntity(null, mockRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllEntityUrlsNull(){
		UrlHelpers.setAllEntityUrls(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllEntityNullUri(){
		Preview preview = new Preview();
		preview.setUri(null);
		UrlHelpers.setAllEntityUrls(preview);
	}
	
	@Test
	public void testSetAllEntityUrls(){
		Preview preview = new Preview();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1/entity/42";
		preview.setUri(baseUri);
		UrlHelpers.setAllEntityUrls(preview);
		assertEquals(baseUri+UrlHelpers.ACL, preview.getAccessControlList());
		assertEquals(baseUri+UrlHelpers.ANNOTATIONS, preview.getAnnotations());
	}
	

	
	@Test
	public void testSetAllUrlsForEntity() throws InstantiationException, IllegalAccessException{
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		String base = "/repo/v1";
		String id = "56";
		when(mockRequest.getServletPath()).thenReturn("");
		// Test each type
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			Entity entity = EntityTypeUtils.getClassForType(type).newInstance();
			entity.setId(id);
			if(entity instanceof VersionableEntity){
				VersionableEntity able = (VersionableEntity) entity;
				// Make sure it has a version number
				able.setVersionNumber(43l);
			}
			UrlHelpers.setAllUrlsForEntity(entity, mockRequest);
			String expectedBase = base+UrlHelpers.ENTITY+"/"+id;
			assertEquals(expectedBase, entity.getUri());
			String expected = expectedBase+UrlHelpers.ANNOTATIONS;
			assertEquals(expected, entity.getAnnotations());
			expected =  expectedBase+UrlHelpers.ACL;
			assertEquals(expected, entity.getAccessControlList());
			// VersionableEntity
			if(entity instanceof VersionableEntity){
				VersionableEntity able = (VersionableEntity) entity;
				// Make sure it has a version number
				expected = expectedBase+UrlHelpers.VERSION;
				assertEquals(expected, able.getVersions());
				expected = expectedBase+UrlHelpers.VERSION+"/43";
				assertEquals(expected, able.getVersionUrl());
			}
		}
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
