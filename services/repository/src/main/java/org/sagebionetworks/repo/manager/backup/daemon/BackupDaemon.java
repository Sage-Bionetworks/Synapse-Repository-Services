package org.sagebionetworks.repo.manager.backup.daemon;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.backup.NodeBackupDriver;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.BackupRestoreStatus.STATUS;
import org.sagebionetworks.repo.model.BackupRestoreStatus.TYPE;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
	
	static private Log log = LogFactory.getLog(BackupDaemon.class);
	public static long NANO_SECONDS_PER_MILISECOND = 1000000;
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String HTTPS = "https://";
	
	private BackupRestoreStatusDAO backupRestoreStatusDao;
	private NodeBackupDriver backupDriver;
	private AmazonS3Client awsClient;
	private String awsBucket;
	private TYPE type;
	private String backupFileName;
	// The set of entities to backup
	private Set<String> entitiesToBackup;
	
	private BackupRestoreStatus status;
	private long startTimeNano;
	// The thread pool controls how many daemon threads we start.
	ExecutorService threadPool;
	// Is the driver daemon done?
	private volatile boolean isDriverDone;
	private volatile Throwable driverError;

	
	/**
	 * Create a new daemon.  This is protected and should only be called from the launcher.
	 * @param dao
	 * @param driver
	 */
	BackupDaemon(BackupRestoreStatusDAO dao, NodeBackupDriver driver, AmazonS3Client client, String bucket, ExecutorService threadPool){
		if(dao == null) throw new IllegalArgumentException("BackupRestoreStatusDAO cannot be null");
		if(driver == null) throw new IllegalArgumentException("NodeBackupDriver cannot be null");
		if(client == null) throw new IllegalArgumentException("AmazonS3Client cannot be null");
		if(bucket == null) throw new IllegalArgumentException("Bucket cannot be null");
		if(threadPool == null) throw new IllegalArgumentException("Thread pool cannot be null");
		this.backupRestoreStatusDao = dao;
		this.backupDriver = driver;
		this.awsClient = client;
		this.awsBucket = bucket;
		this.threadPool = threadPool;
	}
	
	/**
	 * Create a new daemon.  This is protected and should only be called from the launcher.
	 * @param dao
	 * @param driver
	 */
	BackupDaemon(BackupRestoreStatusDAO dao, NodeBackupDriver driver, AmazonS3Client client, String bucket, ExecutorService threadPool, Set<String> entitiesToBackup){
		this(dao, driver, client, bucket, threadPool);
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
			final File tempBackup = File.createTempFile("BackupDaemonJob"+status.getId()+"-", ".zip");
			tempToDelete = tempBackup;
			// We have started.
			status.setStatus(STATUS.STARTED.name());

			// If this is a restore then we need to download the file from S3
			if(TYPE.RESTORE == type){
				status.setProgresssMessage("Starting to download the file from S3...");
				status.setBackupUrl(getS3URL(this.awsBucket, this.backupFileName));
				updateStatus();
				downloadFileFromS3(tempBackup, awsBucket, backupFileName);
				// Let the user know we finished dowloading file from S#
				status.setProgresssMessage("Finished dowloading file from S3: "+this.backupFileName);
				updateStatus();
			}
			
			// Start the driver
			progress.setMessage("Starting Driver thread...");
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
				// We add 10% to the total because we will need to upload the file to S3 after
				// the driver is done creating it.
				long totalProgress = progress.getTotalCount()+(long)(progress.getTotalCount()*.1);
				status.setProgresssTotal(totalProgress);
				// Update the status
				updateStatus();
			}
			// If this a backup then update the file to s3
			if(TYPE.BACKUP == type){
				// Once the driver is done upload the file to S3.
				status.setProgresssMessage("Starting to upload temp file: "+tempBackup.getAbsolutePath()+" to S3...");
				updateStatus();
				// Now upload the file to S3
				String backupUrl = uploadFileToS3(tempBackup);
				status.setBackupUrl(backupUrl);
			}

			// We are done
			status.setStatus(STATUS.COMPLETED.name());
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
		// The type determines which driver method we call
		// Start the working thread
		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				// Tell the driver to do its thing
				try {
					if(TYPE.BACKUP == type){
						// This is a backup
						backupDriver.writeBackup(tempBackup, progress, entitiesToBackup);							
					}else if(TYPE.RESTORE == type){
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
		// Set the status to failed
		status.setProgresssMessage("");
		status.setStatus(STATUS.FAILED.name());
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
	public BackupRestoreStatus startBackup(String userName) throws DatastoreException {
		if(userName == null) throw new IllegalArgumentException("Username cannot be null");
		this.type = TYPE.BACKUP;
		return start(userName);
	}
	
	/**
	 * Start this daemon for a restore.
	 * @param userName
	 * @param backupFileUrl
	 * @return
	 * @throws DatastoreException
	 */
	public BackupRestoreStatus startRestore(String userName, String fileName) throws DatastoreException {
		if(userName == null) throw new IllegalArgumentException("Username cannot be null");
		if(fileName == null) throw new IllegalArgumentException("Backup file name cannot be null");
		this.type = TYPE.RESTORE;
		this.backupFileName = fileName;
		return start(userName);
	}

	private BackupRestoreStatus start(String userName) throws DatastoreException {
		// Now create the backup status
		status = new BackupRestoreStatus();
		status.setStartedBy(userName);
		status.setStartedOn(new Date());
		status.setStatus(STATUS.IN_QUEUE.name());
		status.setType(this.type.name());
		status.setProgresssMessage("Pushed to the thread pool queue...");
		status.setProgresssCurrent(0);
		status.setProgresssTotal(0);
		startTimeNano = System.nanoTime();
		// Create the new status.
		String id = backupRestoreStatusDao.create(status);
		status.setId(id);
		// Now that we have our status we are ready to go
		threadPool.execute(this);
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
	private String uploadFileToS3(File toUpload){
		log.info("Atempting to upload: "+getS3URL(awsBucket, toUpload.getName()));
		PutObjectResult results = this.awsClient.putObject(awsBucket, toUpload.getName(), toUpload);
		log.info(results);
		this.backupFileName = toUpload.getName();
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

}
