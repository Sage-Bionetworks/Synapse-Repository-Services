package org.sagebionetworks.repo.web.service;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class MigrationServiceImpl implements MigrationService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	MigrationManager migrationManager;
	@Autowired
	BackupDaemonLauncher backupDaemonLauncher;	

	/**
	 * Get the counts for each migration type.
	 */
	@Override
	public MigrationTypeCounts getTypeCounts(String userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		// Get the counts for each.
		List<MigrationTypeCount> list = new LinkedList<MigrationTypeCount>();
		for(MigrationType type: MigrationType.values()){
			long count = migrationManager.getCount(user, type);
			long maxId = migrationManager.getMaxId(user, type);
			MigrationTypeCount tc = new MigrationTypeCount();
			tc.setCount(count);
			tc.setMaxid(maxId);
			tc.setType(type);
			list.add(tc);
		}
		MigrationTypeCounts counts = new MigrationTypeCounts();
		counts.setList(list);
		return counts;
	}
	
	@Override
	public RowMetadataResult getRowMetadaForType(String userId,	MigrationType type, long limit, long offset) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return migrationManager.getRowMetadaForType(user, type, limit, offset);
	}

	@Override
	public RowMetadataResult getRowMetadataDeltaForType(String userId,	MigrationType type, List<Long> list) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return migrationManager.getRowMetadataDeltaForType(user, type, list);
	}

	@Override
	public BackupRestoreStatus startBackup(String userId, MigrationType type, List<Long> list) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.startBackup(user, type, list);
	}

	@Override
	public BackupRestoreStatus startRestore(String userId, MigrationType type,	String fileName) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.startRestore(user, fileName, type);
	}

	@Override
	public MigrationTypeCount delete(String userId, MigrationType type, List<Long> list) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		long count = migrationManager.deleteObjectsById(user, type, list);
		MigrationTypeCount tc = new MigrationTypeCount();
		tc.setCount(count);
		tc.setType(type);
		// Make sure the user cache is cleared because we could have deleted users.
		userManager.clearCache();
		return tc;
	}

	@Override
	public BackupRestoreStatus getStatus(String userId, String daemonId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.getStatus(user, daemonId);
	}

	@Override
	public MigrationTypeList getPrimaryTypes(String userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<MigrationType> list = migrationManager.getPrimaryMigrationTypes(user);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.setList(list);
		return mtl;
	}

}
