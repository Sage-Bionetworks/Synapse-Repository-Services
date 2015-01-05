package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;

public class V2DBOWikiOwnerTest {
	
	MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwnerBackup> translator;
	
	@Before
	public void before(){
		translator = new V2DBOWikiOwner().getTranslator();
	}

	/**
	 * Between release-71 and release-72 'ownerType' was renamed 'ownerTypeEnum'.
	 * Also etags were added and cannot be null.
	 */
	@Test
	public void testTranslationRelease71ToRelease72(){
		V2DBOWikiOwnerBackup r71Backup = new V2DBOWikiOwnerBackup();
		r71Backup.setEtag(null);
		r71Backup.setOwnerType(ObjectType.ENTITY);
		r71Backup.setOwnerId(123L);
		r71Backup.setRootWikiId(456L);
		
		V2DBOWikiOwner r72DBO = translator.createDatabaseObjectFromBackup(r71Backup);
		assertNotNull(r72DBO);
		assertNotNull("Etag should not be null",r72DBO.getEtag());
		assertEquals(ObjectType.ENTITY, r72DBO.getOwnerTypeEnum());
		assertEquals(r71Backup.getOwnerId(), r72DBO.getOwnerId());
		assertEquals(r71Backup.getRootWikiId(), r72DBO.getRootWikiId());
		assertEquals(null, r72DBO.getOrderHint());
	}
	
	/**
	 * Must round trip dbo -> backup -> dbo' such that dbo == dbo'
	 */
	@Test
	public void testTranslationRoundTripRelease72(){
		V2DBOWikiOwner r72DBO = new V2DBOWikiOwner();
		r72DBO.setEtag("etag");
		r72DBO.setOrderHint("data".getBytes());
		r72DBO.setOwnerId(123L);
		r72DBO.setOwnerTypeEnum(ObjectType.EVALUATION);
		r72DBO.setRootWikiId(345L);
		
		V2DBOWikiOwnerBackup backup = translator.createBackupFromDatabaseObject(r72DBO);
		V2DBOWikiOwner clone = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(r72DBO, clone);
	}
}
