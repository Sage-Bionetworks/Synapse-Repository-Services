package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.HasLayers;
import org.sagebionetworks.repo.model.HasLocations;
import org.sagebionetworks.repo.model.HasPreviews;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StoredLayerPreview;
import org.sagebionetworks.repo.model.Versionable;
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
	public void testsetEntityUriNullId(){
		UrlHelpers.createEntityUri(null, Dataset.class, "http://localhost:8080/repo/v1");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullClass(){
		UrlHelpers.createEntityUri("12", null, "http://localhost:8080/repo/v1");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullPrefix(){
		UrlHelpers.createEntityUri("12", LayerLocation.class, null);
	}
	
	@Test 
	public void testsetEntityUriAllTypes(){
		ObjectType[] array = ObjectType.values();
		String uriPrefix = "/repo/v1";
		String id = "123";
		for(ObjectType type: array){
			String expectedUri = uriPrefix+type.getUrlPrefix()+"/"+id;
			String uri = UrlHelpers.createEntityUri(id, type.getClassForType(), uriPrefix);
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
	public void testSetBaseUriForEntityNullRequest(){
		Dataset mockDs = Mockito.mock(Dataset.class);
		when(mockDs.getId()).thenReturn("123");
		UrlHelpers.setBaseUriForEntity(mockDs, null);
	}
	
	@Test
	public void testSetBaseUriForEntity(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		Dataset ds = new Dataset();
		ds.setId("456");
		UrlHelpers.setBaseUriForEntity(ds, mockRequest);
		String expectedUri = "http://localhost:8080/repo/v1/dataset/456";
		assertEquals(expectedUri, ds.getUri());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllNodeableUrlsNull(){
		UrlHelpers.setAllNodeableUrls(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllNodeableNullUri(){
		StoredLayerPreview preview = new StoredLayerPreview();
		preview.setUri(null);
		UrlHelpers.setAllNodeableUrls(preview);
	}
	
	@Test
	public void testSetAllNodeableUrls(){
		StoredLayerPreview preview = new StoredLayerPreview();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1"+ObjectType.preview.getUrlPrefix()+"/42";
		preview.setUri(baseUri);
		UrlHelpers.setAllNodeableUrls(preview);
		assertEquals(baseUri+UrlHelpers.ACL, preview.getAccessControlList());
		assertEquals(baseUri+UrlHelpers.ANNOTATIONS, preview.getAnnotations());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasLayersUrlEntityNull(){
		UrlHelpers.setHasLayersUrl(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasLayersUrlNullUri(){
		Dataset ds = new Dataset();
		ds.setUri(null);
		UrlHelpers.setHasLayersUrl(ds);
	}
	
	@Test
	public void testHasLayersUrlUrls(){
		Dataset ds = new Dataset();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1"+ObjectType.dataset.getUrlPrefix()+"/42";
		ds.setUri(baseUri);
		UrlHelpers.setHasLayersUrl(ds);
		assertEquals(baseUri+UrlHelpers.LAYER, ds.getLayers());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasLocationsEntityNull(){
		UrlHelpers.setHasLocationsUrl(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasLocationsNullUri(){
		Dataset ds = new Dataset();
		ds.setUri(null);
		UrlHelpers.setHasLocationsUrl(ds);
	}
	
	@Test
	public void testHasLocationsUrls(){
		Dataset ds = new Dataset();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1"+ObjectType.dataset.getUrlPrefix()+"/42";
		ds.setUri(baseUri);
		UrlHelpers.setHasLocationsUrl(ds);
		assertEquals(baseUri+UrlHelpers.LOCATION, ds.getLocations());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasPreviewEntityNull(){
		UrlHelpers.setHasPreviewsUrl(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testHasPreviewNullUri(){
		InputDataLayer layer = new InputDataLayer();
		layer.setUri(null);
		UrlHelpers.setHasPreviewsUrl(layer);
	}
	
	@Test
	public void testHasPreviewUrls(){
		InputDataLayer layer = new InputDataLayer();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1"+ObjectType.layer.getUrlPrefix()+"/42";
		layer.setUri(baseUri);
		UrlHelpers.setHasPreviewsUrl(layer);
		assertEquals(baseUri+UrlHelpers.PREVIEW, layer.getPreviews());
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetVersionableNullUri(){
		LayerLocation location = new LayerLocation();
		location.setUri(null);
		UrlHelpers.setVersionableUrl(location);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetVersionableNullVersionNumber(){
		LayerLocation location = new LayerLocation();
		String baseUri = "/repo/v1"+ObjectType.location.getUrlPrefix()+"/42";
		location.setUri(baseUri);
		// set the version number to be null
		location.setVersionNumber(null);
		UrlHelpers.setVersionableUrl(location);
	}
	
	@Test
	public void testSetVersionable(){
		LayerLocation location = new LayerLocation();
		location.setVersionNumber(new Long(12));
		// Make sure the location has a uri
		String baseUri = "/repo/v1"+ObjectType.location.getUrlPrefix()+"/42";
		location.setUri(baseUri);
		UrlHelpers.setVersionableUrl(location);
		assertEquals(baseUri+UrlHelpers.VERSION, location.getVersions());
		assertEquals(baseUri+UrlHelpers.VERSION+"/12", location.getVersionUrl());
	}
	
	@Test
	public void testSetAllUrlsForEntity() throws InstantiationException, IllegalAccessException{
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		String base = "/repo/v1";
		String id = "56";
		when(mockRequest.getServletPath()).thenReturn(base);
		// Test each type
		ObjectType[] array = ObjectType.values();
		for(ObjectType type: array){
			Nodeable entity = type.getClassForType().newInstance();
			entity.setId(id);
			if(entity instanceof Versionable){
				Versionable able = (Versionable) entity;
				// Make sure it has a version number
				able.setVersionNumber(43l);
			}
			UrlHelpers.setAllUrlsForEntity(entity, mockRequest);
			String expectedBase = base+type.getUrlPrefix()+"/"+id;
			assertEquals(expectedBase, entity.getUri());
			String expected = expectedBase+UrlHelpers.ANNOTATIONS;
			assertEquals(expected, entity.getAnnotations());
			expected =  expectedBase+UrlHelpers.ACL;
			assertEquals(expected, entity.getAccessControlList());
			// Has layers
			if(entity instanceof HasLayers){
				HasLayers hasLayers = (HasLayers) entity;
				expected = expectedBase+UrlHelpers.LAYER;
				assertEquals(expected, hasLayers.getLayers());
			}
			// Has locations
			if(entity instanceof HasLocations){
				HasLocations has = (HasLocations) entity;
				expected = expectedBase+UrlHelpers.LOCATION;
				assertEquals(expected, has.getLocations());
			}
			// Has preview
			if(entity instanceof HasPreviews){
				HasPreviews has = (HasPreviews) entity;
				expected = expectedBase+UrlHelpers.PREVIEW;
				assertEquals(expected, has.getPreviews());
			}
			// Versionable
			if(entity instanceof Versionable){
				Versionable able = (Versionable) entity;
				// Make sure it has a version number
				expected = expectedBase+UrlHelpers.VERSION;
				assertEquals(expected, able.getVersions());
				expected = expectedBase+UrlHelpers.VERSION+"/43";
				assertEquals(expected, able.getVersionUrl());
			}
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllUrlsNullBase(){
		InputDataLayer layer = new InputDataLayer();
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullAnnos(){
		InputDataLayer layer = new InputDataLayer();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllNodeableUrls(layer);
		layer.setAnnotations(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullACL(){
		InputDataLayer layer = new InputDataLayer();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllNodeableUrls(layer);
		layer.setAccessControlList(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullLocations(){
		InputDataLayer layer = new InputDataLayer();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllNodeableUrls(layer);
		layer.setLocations(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullPreview(){
		InputDataLayer layer = new InputDataLayer();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllNodeableUrls(layer);
		layer.setPreviews(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullVersions(){
		LayerLocation location = new LayerLocation();
		location.setVersionNumber(45l);
		location.setUri("repo/v1/location/33");
		UrlHelpers.setAllNodeableUrls(location);
		location.setVersions(null);
		UrlHelpers.validateAllUrls(location);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullVersionUrl(){
		LayerLocation location = new LayerLocation();
		location.setVersionNumber(1l);
		location.setUri("repo/v1/location/33");
		UrlHelpers.setAllNodeableUrls(location);
		location.setVersionUrl(null);
		UrlHelpers.validateAllUrls(location);
	}


}
