package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

/**
 * This is a unit test for NodeBackupDriverImpl.
 * @author jmhill
 *
 */
public class PrincipalBackupDriverTest {
	
	PrincipalBackupDriver sourceDriver = null;
	PrincipalBackupDriver destinationDriver = null;
	
	Map<String, UserGroup> srcGroups;
	Map<String, UserProfile> srcProfiles;
	Map<String, UserGroup> dstGroups;
	Map<String, UserProfile> dstProfiles;
	
	// implement get, getAll, update, create
	private UserGroupDAO getUserGroupDAO(final Map<String, UserGroup> groups) {
		return (UserGroupDAO)Proxy.newProxyInstance(PrincipalBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{UserGroupDAO.class},
                new InvocationHandler() {
					private int nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(UserGroupDAO.class.getMethod("get", String.class))) {
							UserGroup ug = groups.get(args[0]);
							if (ug==null) throw new NotFoundException();
							return ug;
						} else if (method.equals(UserGroupDAO.class.getMethod("getAll", Boolean.TYPE))) {
							boolean isIndividual = (Boolean)args[0];
							Collection<UserGroup> ans = new HashSet<UserGroup>();
							for (UserGroup ug : groups.values()) if (ug.getIsIndividual().equals(isIndividual)) ans.add(ug);
							return ans;
						} else if (method.equals(UserGroupDAO.class.getMethod("create", Object.class))) {
							UserGroup ug = (UserGroup)args[0];
							if (ug.getId()==null) {
								if (groups.containsKey(""+nextKey)) throw new IllegalStateException();
								ug.setId(""+(nextKey++));
							} else {
								if (groups.containsKey(ug.getId())) throw new  RuntimeException("already exists");
								nextKey = Integer.parseInt(ug.getId())+1;
							}
							groups.put(ug.getId(), ug);
							return ug.getId();
						} else if (method.equals(UserGroupDAO.class.getMethod("update", Object.class))) {
							UserGroup ug = (UserGroup)args[0];
							if (ug.getId()==null || !groups.containsKey(ug.getId())) throw new RuntimeException("doesn't exist");
							groups.put(ug.getId(), ug);
							return null;
						} else if (method.equals(UserGroupDAO.class.getMethod("findGroup", String.class, Boolean.TYPE))) {
							String name = (String)args[0];
							boolean isIndividual = (Boolean)args[1];
							for (UserGroup ug : groups.values()) {
								if (ug.getName().equals(name) && ug.getIsIndividual().equals(isIndividual)) return ug;
							}
							return null;
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	// implement get, update, create
	private UserProfileDAO getUserProfileDAO(final Map<String, UserProfile> profiles) {
		return (UserProfileDAO)Proxy.newProxyInstance(PrincipalBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{UserProfileDAO.class},
                new InvocationHandler() {
			private int nextKey = 0;
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(UserProfileDAO.class.getMethod("get", String.class, ObjectSchema.class))) {
					UserProfile up = profiles.get(args[0]);
					if (up==null) throw new NotFoundException();
					return up;
				} else if (method.equals(UserProfileDAO.class.getMethod("create", UserProfile.class, ObjectSchema.class))) {
					UserProfile up = (UserProfile)args[0];
					if (up.getOwnerId()==null) {
						if (profiles.containsKey(""+nextKey)) throw new IllegalStateException();
						up.setOwnerId(""+(nextKey++));
					} else {
						if (profiles.containsKey(up.getOwnerId())) throw new  RuntimeException("already exists");
						nextKey = Integer.parseInt(up.getOwnerId())+1;
					}
					profiles.put(up.getOwnerId(), up);
					return up.getOwnerId();
				} else if (method.equals(UserProfileDAO.class.getMethod("update", UserProfile.class, ObjectSchema.class))) {
					UserProfile up = (UserProfile)args[0];
					if (up.getOwnerId()==null || !profiles.containsKey(up.getOwnerId())) throw new RuntimeException("doesn't exist");
					profiles.put(up.getOwnerId(), up);
					return null;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private static Random rand = new Random();
	
	private UserGroup createUserGroup(boolean isIndivdual) throws Exception {
		UserGroup ug = new UserGroup();
		ug.setName(""+rand.nextLong());
		ug.setIsIndividual(isIndivdual);
		return ug;
	}
	
	private UserProfile createUserProfile(String ownerId) throws Exception {
		UserProfile up = new UserProfile();
		up.setOwnerId(ownerId);
		up.setDisplayName("foo");
		up.setEtag("1");
		return up;
	}
	
	@Before
	public void before() throws Exception {
		srcGroups = new HashMap<String, UserGroup>();
		srcProfiles = new HashMap<String, UserProfile>();
		dstGroups = new HashMap<String, UserGroup>();
		dstProfiles = new HashMap<String, UserProfile>();
		UserGroupDAO srcGroupDAO = getUserGroupDAO(srcGroups);
		UserGroup ug = createUserGroup(true);
		srcGroupDAO.create(ug);
		UserProfileDAO srcProfileDAO = getUserProfileDAO(srcProfiles);
		srcProfileDAO.create(createUserProfile(ug.getId()), null);
		ug = createUserGroup(false);
		srcGroupDAO.create(ug);
		assertEquals(2, srcGroups.size());
		UserGroupDAO dstGroupDAO = getUserGroupDAO(dstGroups);
		UserProfileDAO dstProfileDAO = getUserProfileDAO(dstProfiles);
		sourceDriver = new PrincipalBackupDriver(srcGroupDAO, srcProfileDAO);
		destinationDriver = new PrincipalBackupDriver(dstGroupDAO, dstProfileDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("PrincipalBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, srcGroups.keySet());
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstGroups.isEmpty());
			assertTrue(dstProfiles.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcGroups, dstGroups);
			assertEquals(srcProfiles, dstProfiles);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	

}
