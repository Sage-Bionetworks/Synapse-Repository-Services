package org.sagebionetworks.repo.manager.backup.daemon;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.backup.GenericBackupDriver;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
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
public class BackupDaemon implements Runnable{
	
	private static final String PREFIX_BACKUP = "Backup-";
	private static final String PREFIX_TEMP = "temp-";
	static private Log log = LogFactory.getLog(BackupDaemon.class);
	public static long NANO_SECONDS_PER_MILISECOND = 1000000;
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String HTTPS = "https://";
	private static final String S3KEY_SEARCH_PREFIX = "Search/";

	private BackupRestoreStatusDAO backupRestoreStatusDao;
	private GenericBackupDriver backupDriver;
	private AmazonS3Client awsClient;
	private String awsBucket;
	private DaemonType type;
	private String backupFileName;
	// The set of entities to backup
	private Set<String> entitiesToBackup;
	
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

	
	/**
	 * Create a new daemon.  This is protected and should only be called from the launcher.
	 * @param dao
	 * @param driver
	 */
	BackupDaemon(BackupRestoreStatusDAO dao, GenericBackupDriver driver, AmazonS3Client client, String bucket, ExecutorService threadPool, ExecutorService threadPool2){
		if(dao == null) throw new IllegalArgumentException("BackupRestoreStatusDAO cannot be null");
		if(driver == null) throw new IllegalArgumentException("GenericBackupDriver cannot be null");
		if(client == null) throw new IllegalArgumentException("AmazonS3Client cannot be null");
		if(bucket == null) throw new IllegalArgumentException("Bucket cannot be null");
		if(threadPool == null) throw new IllegalArgumentException("Thread pool cannot be null");
		this.backupRestoreStatusDao = dao;
		this.backupDriver = driver;
		this.awsClient = client;
		this.awsBucket = bucket;
		this.watcherPool = threadPool;
		this.workerPool = threadPool2;
	}
	
	/**
	 * Create a new daemon.  This is protected and should only be called from the launcher.
	 * @param dao
	 * @param driver
	 */
	BackupDaemon(BackupRestoreStatusDAO dao, GenericBackupDriver driver, AmazonS3Client client, String bucket, ExecutorService threadPool, ExecutorService threadPool2, Set<String> entitiesToBackup){
		this(dao, driver, client, bucket, threadPool, threadPool2);
		this.entitiesToBackup = entitiesToBackup;
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
			String stack = StackConfiguration.getStack();
			String instance = StackConfiguration.getStackInstance();
			String prefix = null;
			if(entitiesToBackup == null){
				// This is a full backup file.
				prefix = PREFIX_BACKUP;
			}
			else {
				// Incremental backup files are temporary.
				prefix = PREFIX_TEMP;
			}
			final File tempBackup = File.createTempFile(prefix+stack+"-"+instance+"-"+status.getId()+"-", ".zip");
			tempToDelete = tempBackup;
			// We have started.
			status.setStatus(DaemonStatus.STARTED);

			// If this is a restore then we need to download the file from S3
			if(DaemonType.RESTORE == type){
				status.setProgresssMessage("Starting to download the file from S3...");
				status.setBackupUrl(getS3URL(this.awsBucket, this.backupFileName));
				updateStatus();
				downloadFileFromS3(tempBackup, awsBucket, backupFileName);
				// If this backup file is a temp file then delete it from S3 after we finish the backup
				if(backupFileName.startsWith(PREFIX_TEMP)){
					deleteFileFromS3(awsBucket, backupFileName);
				}
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
				String backupUrl = uploadFileToS3(tempBackup, null);
				status.setBackupUrl(backupUrl);
			}
			else if(DaemonType.RESTORE == type){
				// If this backup file is a temp file then delete it from S3 now that we have consumed it.
				// We also want to cleanup backup files from builds.
				if(backupFileName.startsWith(PREFIX_TEMP) || "dev".equals(stack) || "bamboo".equals(stack)){
					deleteFileFromS3(awsBucket, backupFileName);
				}
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
						backupDriver.writeBackup(tempBackup, progress, entitiesToBackup);							
					}else if(DaemonType.RESTORE == type) {
						// This is a restore
						backupDriver.restoreFromBackup(tempBackup, progress);		
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
		try {
			long currentTimeNamo = System.nanoTime();
			long elapseMS = (currentTimeNamo-startTimeNano)/NANO_SECONDS_PER_MILISECOND;
			status.setTotalTimeMS(elapseMS);
			backupRestoreStatusDao.update(status);
		}  catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * Start this daemon for a backup..
	 * @param user
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public BackupRestoreStatus startBackup(String userPrincipalId) throws DatastoreException {
		if(userPrincipalId == null) throw new IllegalArgumentException("userPrincipalId cannot be null");
		this.type = DaemonType.BACKUP;
		return start(userPrincipalId);
	}
	
	/**
	 * Start this daemon for a restore.
	 * @param userName
	 * @param backupFileUrl
	 * @return
	 * @throws DatastoreException
	 */
	public BackupRestoreStatus startRestore(String userPrincipalId, String fileName) throws DatastoreException {
		if(userPrincipalId == null) throw new IllegalArgumentException("userPrincipalId cannot be null");
		if(fileName == null) throw new IllegalArgumentException("Backup file name cannot be null");
		this.type = DaemonType.RESTORE;
		this.backupFileName = fileName;
		return start(userPrincipalId);
	}

	private BackupRestoreStatus start(String userPrincipalId) throws DatastoreException {
		// Now create the backup status
		status = new BackupRestoreStatus();
		status.setStartedBy(userPrincipalId);
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
	private String uploadFileToS3(File toUpload, String s3KeyPrefix) {
		String s3Key = s3KeyPrefix + toUpload.getName();
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
