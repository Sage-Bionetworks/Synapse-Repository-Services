package org.sagebionetworks.repo.web.service;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationTypeNames;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class MigrationServiceImpl implements MigrationService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	MigrationManager migrationManager;
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
	
	/**
	 * Get count for a single migration type
	 */
	@Override
	public MigrationTypeCount getTypeCount(Long userId, MigrationType type) {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		MigrationTypeCount mtc = migrationManager.getMigrationTypeCount(user, type);
		return mtc;
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

	@Override
	public MigrationTypeNames getPrimaryTypeNames(Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<String> list = migrationManager.getPrimaryMigrationTypeNames(user);
		MigrationTypeNames mtl = new MigrationTypeNames();
		mtl.setList(list);
		return mtl;
	}

	@Override
	public MigrationTypeList getMigrationTypes(Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<MigrationType> list = migrationManager.getMigrationTypes(user);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.setList(list);
		return mtl;
	}

	@Override
	public MigrationTypeNames getMigrationTypeNames(Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new IllegalArgumentException("userId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<String> list = migrationManager.getMigrationTypeNames(user);
		MigrationTypeNames mtl = new MigrationTypeNames();
		mtl.setList(list);
		return mtl;
	}

	@Override
	public MigrationRangeChecksum getChecksumForIdRange(Long userId, MigrationType type,
			String salt, long minId, long maxId) throws NotFoundException {
		if (userId == null) {
			throw new IllegalArgumentException("userId cannot be null");
		}
		UserInfo user = userManager.getUserInfo(userId);
		
		MigrationRangeChecksum rChecksum = migrationManager.getChecksumForIdRange(user, type, salt, minId, maxId);
		return rChecksum;
	}
	
	@Override
	public MigrationTypeChecksum getChecksumForType(Long userId, MigrationType type) throws NotFoundException {
		if (userId == null) {
			throw new IllegalArgumentException("userId cannot be null");
		}
		UserInfo user = userManager.getUserInfo(userId);
		MigrationTypeChecksum tChecksum = migrationManager.getChecksumForType(user, type);
		return tChecksum;
	}

}
