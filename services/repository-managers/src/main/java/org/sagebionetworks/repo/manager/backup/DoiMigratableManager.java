package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.DoiMigratableDao;
import org.sagebionetworks.repo.model.doi.Doi;
import org.springframework.beans.factory.annotation.Autowired;

public class DoiMigratableManager implements MigratableManager {

	@Autowired private DoiMigratableDao doiMigratableDao;

	@Override
	public void writeBackupToOutputStream(String idToBackup, OutputStream out) {
		Doi backup = doiMigratableDao.get(idToBackup);
		NodeSerializerUtil.writeToStream(backup, out);
	}

	@Override
	public String createOrUpdateFromBackupStream(InputStream in) {
		Doi backup = NodeSerializerUtil.readFromStream(in, Doi.class);
		doiMigratableDao.createOrUpdate(backup);
		return backup.getId().toString();
	}

	@Override
	public void deleteByMigratableId(String id) {
		doiMigratableDao.delete(id);
	}
}
