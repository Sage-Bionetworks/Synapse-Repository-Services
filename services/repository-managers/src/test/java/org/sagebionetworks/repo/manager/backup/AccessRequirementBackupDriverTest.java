package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for NodeBackupDriverImpl.
 * @author jmhill
 *
 */
public class AccessRequirementBackupDriverTest {
	
	AccessRequirementBackupDriver sourceDriver = null;
	AccessRequirementBackupDriver destinationDriver = null;
	
	Map<Long, AccessRequirement> srcARs;
	Map<Long, AccessApproval> srcAAs;
	Map<Long, AccessRequirement> dstARs;
	Map<Long, AccessApproval> dstAAs;
	
	// implement get, getAll, update, create
	private AccessRequirementDAO createAccessRequirementDAO(final Map<Long, AccessRequirement> ars) {
		return (AccessRequirementDAO)Proxy.newProxyInstance(AccessRequirementBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{AccessRequirementDAO.class},
                new InvocationHandler() {
					private long nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(AccessRequirementDAO.class.getMethod("get", String.class))) {
							AccessRequirement ar = ars.get(Long.parseLong((String)args[0]));
							if (ar==null) throw new NotFoundException();
							return ar;
						} else if (method.equals(AccessRequirementDAO.class.getMethod("create", AccessRequirement.class))) {
							AccessRequirement ar = (AccessRequirement)args[0];
							if (ar.getId()==null) {
								if (ars.containsKey(""+nextKey)) throw new IllegalStateException();
								ar.setId((long)(nextKey++));
							} else {
								if (ars.containsKey(ar.getId())) throw new  RuntimeException("already exists");
								nextKey = ar.getId()+1;
							}
							ars.put(ar.getId(), ar);
							return ar;
						} else if(method.equals(AccessRequirementDAO.class.getMethod("getIds"))) {
							List<String> results = new ArrayList<String>();
							for (Long id : ars.keySet()) results.add(id.toString());
							return results;
						} else if (method.equals(AccessRequirementDAO.class.getMethod("update", Object.class))) {
							AccessRequirement ug = (AccessRequirement)args[0];
							if (ug.getId()==null || !ars.containsKey(ug.getId())) throw new RuntimeException("doesn't exist");
							ars.put(ug.getId(), ug);
							return null;
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	// implement get, update, create
	private AccessApprovalDAO createAccessApprovalDAO(final Map<Long, AccessApproval> aas) {
		return (AccessApprovalDAO)Proxy.newProxyInstance(AccessRequirementBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{AccessApprovalDAO.class},
                new InvocationHandler() {
			private long nextKey = 0;
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(AccessApprovalDAO.class.getMethod("get", String.class))) {
					AccessApproval up = aas.get(Long.parseLong((String)args[0]));
					if (up==null) throw new NotFoundException();
					return up;
				} else if (method.equals(AccessApprovalDAO.class.getMethod("create", AccessApproval.class))) {
					AccessApproval aa = (AccessApproval)args[0];
					if (aa.getId()==null) {
						if (aas.containsKey(""+nextKey)) throw new IllegalStateException();
						aa.setId(nextKey++);
					} else {
						if (aas.containsKey(aa.getId())) throw new  RuntimeException("already exists");
						nextKey = aa.getId()+1;
					}
					aas.put(aa.getId(), aa);
					return aa;
				} else if (method.equals(AccessApprovalDAO.class.getMethod("update", AccessApproval.class))) {
					AccessApproval aa = (AccessApproval)args[0];
					if (aa.getId()==null || !aas.containsKey(aa.getId())) throw new RuntimeException("doesn't exist");
					aas.put(aa.getId(), aa);
					return null;
				} else if (method.equals(AccessApprovalDAO.class.getMethod("getForAccessRequirement", String.class))) {
					Long arId = Long.parseLong((String)args[0]);
					List<AccessApproval> result =new ArrayList<AccessApproval>();
					for (AccessApproval aa : aas.values()) if (arId.equals(aa.getRequirementId())) result.add(aa);
					return result;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private static Random rand = new Random();
	
	private AccessRequirement createAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>()); // note, this doesn't point to any entity
		ar.setEntityType(TermsOfUseAccessRequirement.class.getName());
		ar.setTermsOfUse("foo");
		return ar;
	}
	
	private AccessApproval createAccessApproval(Long arId) throws Exception {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setId(101L);
		aa.setRequirementId(arId);
		aa.setAccessorId("202");
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		return aa;
	}
	
	@Before
	public void before() throws Exception {
		srcARs = new HashMap<Long, AccessRequirement>();
		srcAAs = new HashMap<Long, AccessApproval>();
		dstARs = new HashMap<Long, AccessRequirement>();
		dstAAs = new HashMap<Long, AccessApproval>();
		AccessRequirementDAO srcAccessRequirementDAO = createAccessRequirementDAO(srcARs);
		AccessRequirement ar = createAccessRequirement();
		ar = srcAccessRequirementDAO.create(ar);
		assertNotNull(ar.getId());
		AccessApprovalDAO srcAccessApprovalDAO = createAccessApprovalDAO(srcAAs);
		srcAccessApprovalDAO.create(createAccessApproval(ar.getId()));
		ar = createAccessRequirement();
		srcAccessRequirementDAO.create(ar);
		assertEquals(2, srcARs.size());
		AccessRequirementDAO dstAccessRequirementDAO = createAccessRequirementDAO(dstARs);
		AccessApprovalDAO dstAccessApprovalDAO = createAccessApprovalDAO(dstAAs);
		sourceDriver = new AccessRequirementBackupDriver(srcAccessRequirementDAO, srcAccessApprovalDAO);
		destinationDriver = new AccessRequirementBackupDriver(dstAccessRequirementDAO, dstAccessApprovalDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("AccessRequirementBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			Set<String> ids = new HashSet<String>(); 
			for (Long key : srcARs.keySet()) ids.add(key.toString());
			sourceDriver.writeBackup(temp, progress, ids);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstARs.isEmpty());
			assertTrue(dstAAs.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcARs, dstARs);
			assertEquals(srcAAs, dstAAs);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	@Test
	public void testMigrateAll() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("AccessRequirementBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null/*null means migrate all */);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstARs.isEmpty());
			assertTrue(dstAAs.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcARs, dstARs);
			assertEquals(srcAAs, dstAAs);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	

}
