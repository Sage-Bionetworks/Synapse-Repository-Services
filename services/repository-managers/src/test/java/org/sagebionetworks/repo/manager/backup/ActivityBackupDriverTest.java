package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for ActivityBackupDriverImpl.
 * @author dburdick
 *
 */
public class ActivityBackupDriverTest {
	
	ActivityBackupDriver sourceDriver = null;
	ActivityBackupDriver destinationDriver = null;
	
	Map<String, Activity> srcActivities;
	Map<String, Activity> dstActivities;
	
	// implement get, getAll, update, create
	private ActivityDAO createActivityDAO(final Map<String, Activity> acts) {
		return (ActivityDAO)Proxy.newProxyInstance(ActivityBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{ActivityDAO.class},
                new InvocationHandler() {
					private long nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(ActivityDAO.class.getMethod("get", String.class))) {
							Activity act = acts.get(args[0]);
							if (act==null) throw new NotFoundException();
							return act;
						} else if (method.equals(ActivityDAO.class.getMethod("create", Activity.class))) {
							Activity act = (Activity)args[0];
							if (act.getId()==null) {
								if (acts.containsKey(""+nextKey)) throw new IllegalStateException();
								act.setId(String.valueOf(nextKey++));
							} else {
								if (acts.containsKey(act.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(act.getId())+1;
							}
							acts.put(act.getId(), act);
							return act.getId();
						} else if(method.equals(ActivityDAO.class.getMethod("getIds"))) {
							List<String> results = new ArrayList<String>();
							for (String id : acts.keySet()) results.add(id);
							return results;
						} else if (method.equals(ActivityDAO.class.getMethod("update", Object.class))) {
							Activity ug = (Activity)args[0];
							if (ug.getId()==null || !acts.containsKey(ug.getId())) throw new RuntimeException("doesn't exist");
							acts.put(ug.getId(), ug);
							return null;
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	private static Random rand = new Random();
		
	@Before
	public void before() throws Exception {
		srcActivities = new HashMap<String, Activity>();
		dstActivities = new HashMap<String, Activity>();
		ActivityDAO srcActivityDAO = createActivityDAO(srcActivities);
		Activity act = new Activity();
		String actId = srcActivityDAO.create(act);
		act.setId(actId);
		assertNotNull(act.getId());
		
		act = new Activity();
		actId = srcActivityDAO.create(act);
		act.setId(actId);
		assertNotNull(act.getId());

		assertEquals(2, srcActivities.size());

		ActivityDAO dstActivityDAO = createActivityDAO(dstActivities);
		sourceDriver = new ActivityBackupDriver(srcActivityDAO);
		destinationDriver = new ActivityBackupDriver(dstActivityDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("ActivityBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			Set<String> ids = new HashSet<String>(); 
			for (String key : srcActivities.keySet()) ids.add(key.toString());
			sourceDriver.writeBackup(temp, progress, ids);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstActivities.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcActivities, dstActivities);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}	

}
