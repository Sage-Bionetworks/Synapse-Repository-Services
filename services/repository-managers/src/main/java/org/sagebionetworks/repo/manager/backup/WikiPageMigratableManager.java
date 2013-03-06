package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiPageMigratableManager implements MigratableManager {
	
	@Autowired
	WikiPageDao wikiPageDao;
	
	public WikiPageMigratableManager(){
		
	}

	/**
	 * IoC constructor.
	 * @param wikiPageDao
	 */
	public WikiPageMigratableManager(WikiPageDao wikiPageDao) {
		this.wikiPageDao = wikiPageDao;
	}

	@Override
	public void writeBackupToOutputStream(String idToBackup, OutputStream out) {
		if(idToBackup == null) throw new IllegalArgumentException("idToBackup cannot be null");
		if(out == null) throw new IllegalArgumentException("OuptStream cannot be null");
		try{
			// Get the key
			WikiPageKey key = new WikiPageKey(idToBackup);
			// Get the 
			WikiPageBackup backup = wikiPageDao.getWikiPageBackup(key);
			// Write this to the Stream
			// Write this object to the stream.
			NodeSerializerUtil.writeToStream(backup, out);
		}catch(Exception e){
			// Convert any exception to a runtime.
			throw new RuntimeException(e);
		}
	}

	@Override
	public String createOrUpdateFromBackupStream(InputStream zin) {
		try{
			// Reade the backup from the stream
			WikiPageBackup backup = NodeSerializerUtil.readFromStream(zin, WikiPageBackup.class);
			WikiPageKey key = wikiPageDao.createOrUpdateFromBackup(backup);
			return key.getKeyString();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteByMigratableId(String id) {
		try{
			// Get the key
			WikiPageKey key = new WikiPageKey(id);
			wikiPageDao.delete(key);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
