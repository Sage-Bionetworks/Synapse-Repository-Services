package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.web.UrlHelpers;

public class DefaultControllerUnitTest {
	

	@Test (expected=IllegalArgumentException.class)
	public void testObjectTypeForUnknonwUrl(){
		// This should throw an exception
		EntityType type = EntityType.getFirstTypeInUrl("/some/uknown/url");
	}
	
	@Test
	public void testObjectTypeForDatasetUrl(){
		EntityType type = EntityType.getFirstTypeInUrl(UrlHelpers.DATASET);
		assertEquals(EntityType.dataset, type);
	}

	@Test
	public void testObjectTypeForProjectUrl(){
		EntityType type = EntityType.getFirstTypeInUrl(UrlHelpers.PROJECT);
		assertEquals(EntityType.project, type);
	}
	
	@Test
	public void testObjectTypeForLayerUrl(){
		EntityType type = EntityType.getFirstTypeInUrl(UrlHelpers.LAYER);
		assertEquals(EntityType.layer, type);
	}
	
	@Test
	public void testObjectTypeForFullFurlUrl(){
		EntityType type = EntityType.getFirstTypeInUrl("/reop/v1"+UrlHelpers.LAYER);
		assertEquals(EntityType.layer, type);
	}
}
