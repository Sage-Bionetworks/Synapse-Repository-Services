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
	 * Get the counts for each migration type in use.
	 */
	@Override
	public MigrationTypeCounts getTypeCounts(Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		// Get the counts for each.
		List<MigrationTypeCount> list = new LinkedList<MigrationTypeCount>();
		for(MigrationType type: MigrationType.values()){
			if (migrationManager.isMigrationTypeUsed(user, type)) {
				long count = migrationManager.getCount(user, type);
				long maxId = migrationManager.getMaxId(user, type);
				MigrationTypeCount tc = new MigrationTypeCount();
				tc.setCount(count);
				tc.setMaxid(maxId);
				tc.setType(type);
				list.add(tc);
			}
		}
		MigrationTypeCounts counts = new MigrationTypeCounts();
		counts.setList(list);
		return counts;
	}
	
	@Override
	public RowMetadataResult getRowMetadaForType(Long userId,	MigrationType type, long limit, long offset) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return migrationManager.getRowMetadaForType(user, type, limit, offset);
	}

	@Override
	public RowMetadataResult getRowMetadataDeltaForType(Long userId,	MigrationType type, List<Long> list) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return migrationManager.getRowMetadataDeltaForType(user, type, list);
	}

	@Override
	public BackupRestoreStatus startBackup(Long userId, MigrationType type, List<Long> list) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.startBackup(user, type, list);
	}

	@Override
	public BackupRestoreStatus startRestore(Long userId, MigrationType type,	String fileName) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.startRestore(user, fileName, type);
	}

	@Override
	public MigrationTypeCount delete(Long userId, MigrationType type, List<Long> list) throws Exception {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		long count = migrationManager.deleteObjectsById(user, type, list);
		MigrationTypeCount tc = new MigrationTypeCount();
		tc.setCount(count);
		tc.setType(type);
		return tc;
	}

	@Override
	public BackupRestoreStatus getStatus(Long userId, String daemonId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		return backupDaemonLauncher.getStatus(user, daemonId);
	}

	@Override
	public MigrationTypeList getPrimaryTypes(Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<MigrationType> list = migrationManager.getPrimaryMigrationTypes(user);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.setList(list);
		return mtl;
	}

}
