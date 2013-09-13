package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dao.WikiPageKey;

public class WikiPageKeyTest {
	
	String ownerObjectId;
	ObjectType ownerObjectType;
	String wikiPageId;
	
	@Before
	public void before(){
		ownerObjectId = "syn123";
		ownerObjectType = ObjectType.ENTITY;
		wikiPageId = "456";
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testOnwerNull(){
		ownerObjectId = null;
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testTypeNull(){
		ownerObjectType = null;
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testWikiIdNull(){
		wikiPageId = null;
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
	}
	
	@Test
	public void testHappyCase(){
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
		assertEquals(ownerObjectId, key.getOwnerObjectId());
		assertEquals(ownerObjectType, key.getOwnerObjectType());
		assertEquals(wikiPageId, key.getWikiPageId());
	}
	
	@Test
	public void testKeyStringRoundTrip(){
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
		String keyString = key.getKeyString();
		assertNotNull(keyString);
		System.out.println(keyString);
		WikiPageKey clone = new WikiPageKey(keyString);
		assertEquals(key, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeyStringBad(){
		WikiPageKey key = new WikiPageKey(ownerObjectId, ownerObjectType, wikiPageId);
		String keyString = ownerObjectId+"-"+wikiPageId;
		assertNotNull(keyString);
		System.out.println(keyString);
		WikiPageKey clone = new WikiPageKey(keyString);
	}

}
