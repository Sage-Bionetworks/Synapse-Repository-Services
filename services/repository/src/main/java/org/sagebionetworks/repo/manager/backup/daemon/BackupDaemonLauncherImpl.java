package org.sagebionetworks.repo.manager.backup.daemon;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.backup.NodeBackupDriver;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
public class BackupDaemonLauncherImpl implements BackupDaemonLauncher {
	
	@Autowired
	BackupRestoreStatusDAO backupRestoreStatusDao;
	@Autowired
	NodeBackupDriver backupDriver;

	@Override
	public BackupRestoreStatus startBackup(UserInfo username)	throws UnauthorizedException, DatastoreException {
		UserInfo.validateUserInfo(username);
		// Only an admin can start a backup Daemon
		if(!username.isAdmin()) throw new UnauthorizedException("Must be an administrator to start a backup daemon");
		
		AmazonS3Client client = createNewAWSClient();
		String bucket = StackConfiguration.getS3Bucket();
		if(bucket == null) 	throw new IllegalArgumentException("Bucket cannot be null null");
		// Create a new daemon and start it
		BackupDaemon daemon = new BackupDaemon(backupRestoreStatusDao, backupDriver, client, bucket);
		// Start that bad boy up!
		return daemon.startBackup(username.getUser().getUserId());
	}
	
	@Override
	public BackupRestoreStatus startRestore(UserInfo username, String fileName)	throws UnauthorizedException, DatastoreException {
		UserInfo.validateUserInfo(username);
		// Only an admin can start a backup Daemon
		if(!username.isAdmin()) throw new UnauthorizedException("Must be an administrator to start a restoration daemon");
		
		AmazonS3Client client = createNewAWSClient();
		String bucket = StackConfiguration.getS3Bucket();
		if(bucket == null) 	throw new IllegalArgumentException("Bucket cannot be null null");
		// Create a new daemon and start it
		BackupDaemon daemon = new BackupDaemon(backupRestoreStatusDao, backupDriver, client, bucket);
		return daemon.startRestore(username.getUser().getUserId(), fileName);
	}

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



}
