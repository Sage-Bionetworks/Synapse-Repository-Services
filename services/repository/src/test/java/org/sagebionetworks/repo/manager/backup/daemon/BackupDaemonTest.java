package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.backup.NodeBackupDriver;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * Mocks are used to test the daemon.
 * @author John
 *
 */

public class BackupDaemonTest {
	
	/**
	 * The max time to allow the daemon to run
	 */
	public static final long TIMEOUT = 10*1000;
	
	AmazonS3Client mockAwsClient = null;
	NodeBackupDriver mockDriver = null;
	BackupRestoreStatusDAO stubDao = null;
	BackupDaemon daemon = null;
	String bucketName = "someFakeBucket";
	ExecutorService threadPool = Executors.newFixedThreadPool(2);
	ExecutorService threadPool2 = Executors.newFixedThreadPool(2);
	
	@Before
	public void before(){
		// Mock the AWS client
		mockAwsClient = Mockito.mock(AmazonS3Client.class);
		mockDriver =  Mockito.mock(NodeBackupDriver.class);
		// Using a stub for the dao gives us more control over the test.
		stubDao = new BackupRestoreStatusDAOStub();
		// The daemon is passed all mock data.
		daemon = new BackupDaemon(stubDao, mockDriver, mockAwsClient, "someFakeBucket", threadPool, threadPool2);
	}
	
	@Test
	public void testSuccessfulBackupRun() throws UnauthorizedException, DatastoreException, NotFoundException, InterruptedException, IOException{
		// Start the daemon
		BackupRestoreStatus status = daemon.startBackup("someUser@sagebase.org");
		assertNotNull(status);
		assertNotNull(status.getId());
		assertNotNull(status.getStatus());
		assertEquals(DaemonType.BACKUP, status.getType());
		assertEquals("someUser@sagebase.org", status.getStartedBy());
		assertNotNull(status.getStartedOn());
		
		String id = status.getId();
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.COMPLETED, id);
		// The url should start off a null
		String fileName = daemon.getBackupFileName();
		assertNotNull(fileName);
		String expectedeUrl = BackupDaemon.getS3URL(bucketName, fileName);
		assertEquals(expectedeUrl , status.getBackupUrl());
		System.out.println(expectedeUrl);
		
		assertNotNull(status.getBackupUrl());
		// The total time should be greater than zero
		assertTrue(status.getTotalTimeMS() > 10);
		assertTrue(status.getErrorMessage() == null);
		assertTrue(status.getErrorDetails() == null);
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// The AWS client should have been called with the file to update once.
		verify(mockAwsClient, atLeastOnce()).putObject((String)any(), (String)any(), (File)any());
		// the driver should have been called to create a backup onece
		verify(mockDriver, atLeastOnce()).writeBackup((File)any(), (Progress)any(), (Set<String>)any());
	}
	
	@Test
	public void testSuccessfulRestoreRun() throws UnauthorizedException, DatastoreException, NotFoundException, InterruptedException, IOException{
		// Start the daemon
		BackupRestoreStatus status = daemon.startRestore("someUser@sagebase.org", "someBackupFileName");
		assertNotNull(status);
		assertNotNull(status.getId());
		assertNotNull(status.getStatus());
		assertEquals(DaemonType.RESTORE, status.getType());
		assertEquals("someUser@sagebase.org", status.getStartedBy());
		assertNotNull(status.getStartedOn());
		
		String id = status.getId();
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.COMPLETED, id);
		// The url should start off a null
		String fileName = daemon.getBackupFileName();
		assertNotNull(fileName);
		String expectedeUrl = BackupDaemon.getS3URL(bucketName, "someBackupFileName");
		assertEquals(expectedeUrl , status.getBackupUrl());
		System.out.println(expectedeUrl);
		// The total time should be greater than zero
		assertTrue(status.getTotalTimeMS() > 10);
		assertTrue(status.getErrorMessage() == null);
		assertTrue(status.getErrorDetails() == null);
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// The AWS client should have been called with the file to update once.
		verify(mockAwsClient, atLeastOnce()).getObject((GetObjectRequest) any(), (File)any());
		// the driver should have been called to create a backup onece
		verify(mockDriver, atLeastOnce()).restoreFromBackup((File)any(), (Progress)any());
	}
	
	@Test
	public void testTerminationBackup() throws Exception, DatastoreException{
		// Test that we can force a termination.
		// Start the daemon
		BackupRestoreStatus status = daemon.startBackup("someUser@sagebase.org");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, true);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertNotNull(status.getId());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	
	@Test
	public void testTerminationRestore() throws Exception, DatastoreException{
		// Test that we can force a termination.
		// Start the daemon
		BackupRestoreStatus status = daemon.startRestore("someUser@sagebase.org", "String filename");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, true);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertNotNull(status.getId());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	@Test
	public void testDriverFailureBackup() throws Exception, DatastoreException{
		// Simulate a driver failure
		when(mockDriver.writeBackup((File)any(), (Progress)any(), (Set<String>)any())).thenThrow(new InterruptedException());
		BackupRestoreStatus status = daemon.startBackup("someUser@sagebase.org");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, false);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	@Test
	public void testDriverFailureRestore() throws Exception, DatastoreException{
		// Simulate a driver failure
		when(mockDriver.restoreFromBackup((File)any(), (Progress)any())).thenThrow(new InterruptedException());
		BackupRestoreStatus status = daemon.startRestore("someUser@sagebase.org", "SomeFileName");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, false);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	@Test
	public void testAwsClientFailureBackup() throws Exception, DatastoreException{
		// This time simulate an AWS failure
		when(mockAwsClient.putObject( (String)any(), (String)any(), (File)any() )).thenThrow(new AmazonClientException("Some error"));
		BackupRestoreStatus status = daemon.startBackup("someUser@sagebase.org");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, false);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	@Test
	public void testAwsClientFailureRestore() throws Exception, DatastoreException{
		// This time simulate an AWS failure
		when(mockAwsClient.getObject((GetObjectRequest) any(),(File)any() )).thenThrow(new AmazonClientException("Some error"));
		BackupRestoreStatus status = daemon.startRestore("someUser@sagebase.org", "some file neam");
		assertNotNull(status);
		assertNotNull(status.getId());
		String id = status.getId();
		stubDao.setForceTermination(id, false);
		
		// Now wait for the daemon to finish
		status = waitForStatus(DaemonStatus.FAILED, id);
		assertNotNull(status.getErrorMessage());
		assertNotNull(status.getErrorDetails());
		assertEquals(DaemonStatus.FAILED, status.getStatus());
	}
	
	/**
	 * Helper method to wait for a given status of the Daemon
	 * @param lookinFor
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private BackupRestoreStatus waitForStatus(DaemonStatus lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException{
		BackupRestoreStatus status = stubDao.get(id);
		long start = System.currentTimeMillis();
		long elapse = 0;
		while(!lookinFor.equals(status.getStatus())){
			// Wait for it to complete
			Thread.sleep(100);
			long end =  System.currentTimeMillis();
			elapse = end-start;
			if(elapse > TIMEOUT){
				fail("Timmed out waiting for the backup deamon to finish");
			}
			status = stubDao.get(id);
			assertEquals(id, status.getId());
			System.out.println(DaemonStatusUtil.printStatus(status));
		}
		return status;
	}


}
