package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for CompetitionBackupDriverImpl.
 * @author bkng
 */
public class SubmissionBackupDriverTest {
	
	SubmissionBackupDriver sourceDriver = null;
	SubmissionBackupDriver destinationDriver = null;
	
	Map<String, Submission> srcSubs;
	Map<String, SubmissionStatus> srcStatuses;
	Map<String, List<String>> srcHandleIds;
	Map<String, Submission> dstSubs;
	Map<String, SubmissionStatus> dstStatuses;
	Map<String, List<String>> dstHandleIds;
	
	private SubmissionStatusDAO createSubmissionStatusDAO(final Map<String, SubmissionStatus> statuses) {
		return (SubmissionStatusDAO)Proxy.newProxyInstance(SubmissionBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{SubmissionStatusDAO.class},
                new InvocationHandler() {
					private long nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(SubmissionStatusDAO.class.getMethod("get", String.class))) {
							SubmissionStatus status = statuses.get((String) args[0]);
							if (status==null) throw new NotFoundException();
							return status;
						} else if (method.equals(SubmissionStatusDAO.class.getMethod("create", SubmissionStatus.class))) {
							SubmissionStatus status = (SubmissionStatus) args[0];
							if (status.getId()==null) {
								if (statuses.containsKey(""+nextKey)) throw new IllegalStateException();
								status.setId("" + (nextKey++));
							} else {
								if (statuses.containsKey(status.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(status.getId())+1;
							}
							statuses.put(status.getId(), status);
							return status.getId();
						} else if (method.equals(SubmissionStatusDAO.class.getMethod("createFromBackup", SubmissionStatus.class))) {
							SubmissionStatus status = (SubmissionStatus) args[0];
							if (status.getId()==null) {
								if (statuses.containsKey(""+nextKey)) throw new IllegalStateException();
								status.setId("" + (nextKey++));
							} else {
								if (statuses.containsKey(status.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(status.getId())+1;
							}
							statuses.put(status.getId(), status);
							return status.getId();
						} else if (method.equals(SubmissionStatusDAO.class.getMethod("updateFromBackup", SubmissionStatus.class))) {
							SubmissionStatus status = (SubmissionStatus) args[0];
							if (status.getId()==null || !statuses.containsKey(status.getId())) throw new RuntimeException("doesn't exist");
							statuses.put(status.getId(), status);
							return null;
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	private SubmissionDAO createSubmissionDAO(final Map<String, Submission> subs) {
		return (SubmissionDAO)Proxy.newProxyInstance(SubmissionBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{SubmissionDAO.class},
                new InvocationHandler() {
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(SubmissionDAO.class.getMethod("create", Submission.class))) {
					Submission sub = (Submission) args[0];
					subs.put(sub.getId(), sub);
					return null;
				} else if (method.equals(SubmissionDAO.class.getMethod("get", String.class))) {
					Submission sub = subs.get((String) args[0]);
					if (sub == null)
						throw new NotFoundException();
					return sub;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private SubmissionFileHandleDAO createSubmissionFileHandleDAO(final Map<String, List<String>> fileHandleIds) {
		return (SubmissionFileHandleDAO)Proxy.newProxyInstance(SubmissionBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{SubmissionFileHandleDAO.class},
                new InvocationHandler() {
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(SubmissionFileHandleDAO.class.getMethod("create", String.class, String.class))) {
					String submissionId = (String) args[0];
					String fileHandleId = (String) args[1];
					if (!fileHandleIds.containsKey(submissionId)) {
						fileHandleIds.put(submissionId, new ArrayList<String>());
					}
					fileHandleIds.get(submissionId).add(fileHandleId);
					return null;
				} else if (method.equals(SubmissionFileHandleDAO.class.getMethod("getAllBySubmission", String.class))) {
					List<String> handleIds = fileHandleIds.get((String) args[0]);
					return handleIds == null ? new ArrayList<String>() : handleIds;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private SubmissionStatus createSubmissionStatus(String submissionId) throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		status.setEtag("eTag");
		status.setId(submissionId);
		status.setModifiedOn(new Date());
		status.setScore(0.42);
		status.setStatus(SubmissionStatusEnum.SCORED);
		return status;
	}
	
	private Submission createSubmission(String submissionId) throws Exception {
		Submission sub = new Submission();
		sub.setEvaluationId("123");
		sub.setCreatedOn(new Date());
		sub.setEntityId("42");
		sub.setId(submissionId);
		sub.setName("name" + submissionId);
		sub.setUserId("456");
		sub.setVersionNumber(Long.parseLong(submissionId) + 5);
		return sub;
	}
	
	@Before
	public void before() throws Exception {
		srcSubs = new HashMap<String, Submission>();
		dstSubs = new HashMap<String, Submission>();
		srcStatuses = new HashMap<String, SubmissionStatus>();		
		dstStatuses = new HashMap<String, SubmissionStatus>();
		srcHandleIds = new HashMap<String, List<String>>();
		dstHandleIds = new HashMap<String, List<String>>();
		SubmissionStatusDAO srcSubmissionStatusDAO = createSubmissionStatusDAO(srcStatuses);
		SubmissionDAO srcSubmissionDAO = createSubmissionDAO(srcSubs);
		SubmissionFileHandleDAO srcSubmissionFileHandleDAO = createSubmissionFileHandleDAO(srcHandleIds);
		int numSubs = 5;
		for (int i = 0; i < numSubs; i++) {
			Entity entity = new Folder();
			entity.setName("foo" + i);
			String submissionId = "" + i;
			srcSubmissionDAO.create(createSubmission(submissionId));
			srcSubmissionStatusDAO.create(createSubmissionStatus(submissionId));
			srcSubmissionFileHandleDAO.create(submissionId, "handle1_" + i);
			srcSubmissionFileHandleDAO.create(submissionId, "handle2_" + i);
		}
		assertEquals(numSubs, srcStatuses.size());
		assertEquals(numSubs, srcSubs.size());
		SubmissionStatusDAO dstSubmissionStatusDAO = createSubmissionStatusDAO(dstStatuses);
		SubmissionDAO dstSubmissionDAO = createSubmissionDAO(dstSubs);
		SubmissionFileHandleDAO dstSubmissionFileHandleDAO = createSubmissionFileHandleDAO(dstHandleIds);
		sourceDriver = new SubmissionBackupDriver(srcSubmissionDAO, srcSubmissionStatusDAO, srcSubmissionFileHandleDAO);
		destinationDriver = new SubmissionBackupDriver(dstSubmissionDAO, dstSubmissionStatusDAO, dstSubmissionFileHandleDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("SubmissionBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			Set<String> ids = new HashSet<String>(); 
			for (String key : srcStatuses.keySet()) ids.add(key);
			sourceDriver.writeBackup(temp, progress, ids);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertFalse(srcSubs.isEmpty());
			assertFalse(srcStatuses.isEmpty());
			assertFalse(srcHandleIds.isEmpty());
			assertTrue(dstSubs.isEmpty());
			assertTrue(dstStatuses.isEmpty());
			assertTrue(dstHandleIds.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcSubs, dstSubs);
			assertEquals(srcStatuses, dstStatuses);
			assertEquals(srcHandleIds, dstHandleIds);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}	

}
