package org.sagebionetworks.repo.manager.backup.daemon;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * This object is not autowired.  A new instance should be created each time one is needed.
 * This is a daemon that will perform a single backup or restore.  The object should not be re-used.
 * @author John
 *
 */
public class BackupRestoreDaemon implements Runnable{
	
	static private Log log = LogFactory.getLog(BackupRestoreDaemon.class);
	public static long NANO_SECONDS_PER_MILISECOND = 1000000;
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String HTTPS = "https://";

	private BackupRestoreStatusDAO backupRestoreStatusDao;
	private BackupDriver backupDriver;
	private AmazonS3Client awsClient;
	private String awsBucket;
	private DaemonType type;
	private String backupFileName;
	// The list of IDs to backup.
	private List<Long> idsToBackup;
	
	private BackupRestoreStatus status;
	private long startTimeNano;
	// The first pool is for watcher threads.
	ExecutorService watcherPool;
	// The second pool is for worker threads.
	// We need two pools because the first pool can fill up
	// with watcher requests. If we tried to get works from the
	// watcher pool we could have dead lock. With two pools
	// the we are never blocked.
	ExecutorService workerPool;
	// Is the driver daemon done?
	private volatile boolean isDriverDone;
	private volatile Throwable driverError;
	private UserInfo user;
	private MigrationType migrationType;

	
	/**
	 * Create a new daemon.  This is protected and should only be called from the launcher.
	 * @param dao
	 * @param driver
	 */
	BackupRestoreDaemon(UserInfo user, BackupRestoreStatusDAO dao, BackupDriver driver, AmazonS3Client client, String bucket, ExecutorService threadPool, ExecutorService threadPool2, List<Long> idsToBackup, MigrationType migrationType){
		if(dao == null) throw new IllegalArgumentException("BackupRestoreStatusDAO cannot be null");
		if(driver == null) throw new IllegalArgumentException("GenericBackupDriver cannot be null");
		if(client == null) throw new IllegalArgumentException("AmazonS3Client cannot be null");
		if(bucket == null) throw new IllegalArgumentException("Bucket cannot be null");
		if(threadPool == null) throw new IllegalArgumentException("Thread pool cannot be null");
		if(migrationType == null) throw new IllegalArgumentException("Type cannot be null");
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		this.backupRestoreStatusDao = dao;
		this.backupDriver = driver;
		this.awsClient = client;
		this.awsBucket = bucket;
		this.watcherPool = threadPool;
		this.workerPool = threadPool2;
		this.idsToBackup = idsToBackup;
		this.migrationType = migrationType;
		this.user = user;
	}
	
	
	/**
	 * Get the backup file name
	 * @return
	 */
	public String getBackupFileName(){
		return backupFileName;
	}

	@Override
	public void run() {
		File tempToDelete = null;
		final Progress progress = new Progress();
		try{
			// Before we start, make sure it has not been terminated.
			checkForTermination();
			// Create the temporary file to write the backup too.
			String stack = StackConfiguration.singleton().getStack();
			String instance = StackConfiguration.getStackInstance();
			final File tempBackup = File.createTempFile(stack+"-"+instance+"-"+status.getId()+"-", ".zip");
			tempToDelete = tempBackup;
			// We have started.
			status.setStatus(DaemonStatus.STARTED);

			// If this is a restore then we need to download the file from S3
			if(DaemonType.RESTORE == type){
				status.setProgresssMessage("Starting to download the file from S3...");
				status.setBackupUrl(getS3URL(this.awsBucket, this.backupFileName));
				updateStatus();
				downloadFileFromS3(tempBackup, awsBucket, backupFileName);
				deleteFileFromS3(awsBucket, backupFileName);
				// Let the user know we finished dowloading file from S#
				status.setProgresssMessage("Finished dowloading file from S3: "+this.backupFileName);
				updateStatus();
			}
			
			// Start the driver
			progress.setMessage("Starting Driver thread...");
			progress.appendLog("JVM Name: "+ManagementFactory.getRuntimeMXBean().getName());
			progress.appendLog("Thread ID:"+Thread.currentThread().getId());
			progress.setCurrentIndex(0);
			progress.setTotalCount(Long.MAX_VALUE);
			progress.setTerminate(false);
			startDriverThread(tempBackup, progress);
			// Now watch the progress of the driver
			while(!isDriverDone){
				// If there is a driver error then fail
				if(driverError != null){
					throw new Throwable(driverError);
				}
				// First sleep to give the drive a chance to work
				Thread.sleep(1000);
				// Should we terminate?
				progress.setTerminate(backupRestoreStatusDao.shouldJobTerminate(status.getId()));
				// Update the status from the progress
				status.setProgresssMessage(progress.getMessage());
				status.setProgresssCurrent(progress.getCurrentIndex());
				status.setLog(progress.getLog());
				// We add 10% to the total because we will need to upload the file to S3 after
				// the driver is done creating it.
				long totalProgress = progress.getTotalCount()+(long)(progress.getTotalCount()*.1);
				status.setProgresssTotal(totalProgress);
				// Update the status
				updateStatus();
			}
			// If this a backup then update the file to s3
			if(DaemonType.BACKUP == type){
				// Once the driver is done upload the file to S3.
				status.setProgresssMessage("Starting to upload temp file: "+tempBackup.getAbsolutePath()+" to S3...");
				updateStatus();
				// Now upload the file to S3
				String backupUrl = uploadFileToS3(tempBackup, migrationType);
				status.setBackupUrl(backupUrl);
			}

			// We are done
			status.setStatus(DaemonStatus.COMPLETED);
			status.setProgresssMessage("Finished: "+this.type);
			status.setProgresssCurrent(status.getProgresssTotal());
			// update the status for the last time.
			updateStatus();
		}catch(Throwable e){
			// Make sure the thread terminates
			progress.setTerminate(true);
			// If there are any errors then change the status
			setFailed(e);
		}finally{
			if(tempToDelete != null && tempToDelete.exists()){
				// Delete the temp file
//				System.out.println("Not deleting: "+tempToDelete.getAbsolutePath());
				tempToDelete.delete();
			}
		}
	}

	/**
	 * Start the driver.
	 * @param tempBackup
	 * @param progress
	 */
	private void startDriverThread(final File tempBackup, final Progress progress) {
		isDriverDone = false;
		// The second level pool is used to do the actual work.
		// We need a second pool to prevent deadlock.
		workerPool.execute(new Runnable(){
			@Override
			public void run() {
				// Tell the driver to do its thing
				try {
					if(DaemonType.BACKUP == type){
						// This is a backup
						backupDriver.writeBackup(user, tempBackup, progress, migrationType, idsToBackup);							
					}else if(DaemonType.RESTORE == type) {
						// This is a restore
						backupDriver.restoreFromBackup(user, tempBackup,progress);		
					}else{
						throw new IllegalArgumentException("Unknown type: "+type);
					}
					isDriverDone = true;
				} catch (Throwable e) {
					// Keep track of the error.
					driverError = e;
				} 
		}});
	}

	/**
	 * Check to see if this job has been terminated.  If so, throw an exception.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	public void checkForTermination() throws DatastoreException,
			NotFoundException, InterruptedException {
		if(backupRestoreStatusDao.shouldJobTerminate(status.getId())){
			throw new InterruptedException("Backup was terminated by the user");
		}
	}

	private void setFailed(Throwable e) {
		// Log the failure.
		log.error(e);
		e.printStackTrace();
		// Set the status to failed
		status.setProgresssMessage("");
		status.setStatus(DaemonStatus.FAILED);
		status.setErrorMessage(e.getMessage());
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		status.setErrorDetails(writer.toString());
		try {
			updateStatus();
		} catch (DatastoreException e1) {
			// If we failed to write the failure to the database we are stuck logging it
			log.error(e1);
		}
	}

	private void updateStatus() throws DatastoreException {
		long currentTimeNamo = System.nanoTime();
		long elapseMS = (currentTimeNamo - startTimeNano) / NANO_SECONDS_PER_MILISECOND;
		status.setTotalTimeMS(elapseMS);
		backupRestoreStatusDao.update(status);
	}

	/**
	 * Start this daemon for a backup..
	 * @param user
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public BackupRestoreStatus startBackup() throws DatastoreException {
		this.type = DaemonType.BACKUP;
		return start();
	}
	
	/**
	 * Start this daemon for a restore.
	 * @param userName
	 * @param backupFileUrl
	 * @return
	 * @throws DatastoreException
	 */
	public BackupRestoreStatus startRestore(String fileName) throws DatastoreException {
		if(fileName == null) throw new IllegalArgumentException("Backup file name cannot be null");
		this.type = DaemonType.RESTORE;
		this.backupFileName = fileName;
		return start();
	}

	private BackupRestoreStatus start() throws DatastoreException {
		// Now create the backup status
		status = new BackupRestoreStatus();
		status.setStartedBy(user.getId().toString());
		status.setStartedOn(new Date());
		status.setStatus(DaemonStatus.IN_QUEUE);
		status.setType(this.type);
		status.setProgresssMessage("Pushed to the thread pool queue...");
		status.setProgresssCurrent(0l);
		status.setProgresssTotal(0l);
		status.setTotalTimeMS(0l);
		startTimeNano = System.nanoTime();
		// Create the new status.
		String id = backupRestoreStatusDao.create(status);
		status.setId(id);
		// Now that we have our status we are ready to go.
		// Start the watcher thread.
		watcherPool.execute(this);
		// Return a copy of the status from the DB.
		try {
			return backupRestoreStatusDao.get(id);
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Upload the file to S3
	 * @param toUpload
	 * @param id
	 */
	private String uploadFileToS3(File toUpload, MigrationType type) {
		String s3Key = type.name() +"-"+ toUpload.getName();
		log.info("Atempting to upload: "+getS3URL(awsBucket, s3Key));
		PutObjectResult results = this.awsClient.putObject(awsBucket, s3Key, toUpload);
		log.info(results);
		this.backupFileName = s3Key;
		return getS3URL(this.awsBucket, this.backupFileName);
	}

	/**
	 * Build the S3 URL from the name.
	 * @param fileName
	 * @return
	 */
	public static String getS3URL(String bucket, String fileName) {
		StringBuilder url = new StringBuilder();
		url.append(HTTPS);
		url.append(S3_DOMAIN);
		url.append("/");
		url.append(bucket);
		url.append("/");
		url.append(fileName);
		return url.toString();
	}
	
	/**
	 * Download the given file from 
	 * @param tempFile
	 * @return
	 */
	private boolean downloadFileFromS3(File tempFile, String bucket, String fileName){
		log.info("Atempting to dowload: "+getS3URL(bucket, fileName));
		GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, fileName);
		this.awsClient.getObject(getObjectRequest, tempFile);
		return true;
	}
	
	private boolean deleteFileFromS3(String bucket, String fileName){
		log.info("Atempting to delete a temp file from S3 dowload: "+getS3URL(bucket, fileName));
		this.awsClient.deleteObject(bucket, fileName);
		return true;
	}

}
