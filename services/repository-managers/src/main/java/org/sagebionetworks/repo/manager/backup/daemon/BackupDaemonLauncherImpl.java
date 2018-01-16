package org.sagebionetworks.repo.manager.backup.daemon;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * The launcher is responsible for creating new daemons, providing them with their dependencies
 * and starting them.
 * @author John
 *
 */
@Deprecated
public class BackupDaemonLauncherImpl implements BackupDaemonLauncher {
	
	private static String backupBucket = StackConfiguration.getSharedS3BackupBucket();
	
	@Autowired
	BackupRestoreStatusDAO backupRestoreStatusDao;
	
	@Autowired
	ExecutorService backupDaemonThreadPool;
	
	@Autowired
	ExecutorService backupDaemonThreadPool2;
	
	@Autowired
	BackupDriver backupDriver;

	/**
	 * Create a new AWS client using the configuration credentials.
	 * @return
	 */
	private AmazonS3Client createNewAWSClient() {
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null) throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)	throw new IllegalArgumentException("IAM key cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		return client;
	}

	/**
	 * Terminate an running daemon
	 */
	@Override
	public void terminate(UserInfo user, String id)	throws UnauthorizedException, DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(user);
		// Only an admin can stop deamon;
		if(!user.isAdmin()) throw new UnauthorizedException("Must be an administrator to terminate a backup/restore daemon");
		// To terminate a daemon we simply flip the its termination bit in the database.  The daemon will be watching this bit.
		backupRestoreStatusDao.setForceTermination(id, true);
	}

	@Override
	public BackupRestoreStatus getStatus(UserInfo user, String id) throws UnauthorizedException, DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(user);
		// Only an admin can stop deamon;
		if(!user.isAdmin()) throw new UnauthorizedException("Must be an administrator to get the status of a backup/restore daemon");
		return backupRestoreStatusDao.get(id);
	}


	@Override
	public BackupRestoreStatus startBackup(UserInfo user, MigrationType type, List<Long> idsToBackup, BackupAliasType backupAliasType) {
		// Create a new daemon and start it
		AmazonS3Client client = createNewAWSClient();
		BackupRestoreDaemon daemon = new BackupRestoreDaemon(user, backupRestoreStatusDao, backupDriver, client, backupBucket, backupDaemonThreadPool, backupDaemonThreadPool2, idsToBackup, type, backupAliasType);
		return daemon.startBackup();
	}

	@Override
	public BackupRestoreStatus startRestore(UserInfo user, String fileName,
	                                        MigrationType type, BackupAliasType backupAliasType) {
		AmazonS3Client client = createNewAWSClient();
		BackupRestoreDaemon daemon = new BackupRestoreDaemon(user, backupRestoreStatusDao, backupDriver, client, backupBucket, backupDaemonThreadPool, backupDaemonThreadPool2, null, type, backupAliasType);
		return daemon.startRestore(fileName);
	}

}
