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
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for FavoriteBackupDriverImpl.
 * @author dburdick
 *
 */
public class FavoriteBackupDriverTest {
	
	FavoriteBackupDriver sourceDriver = null;
	FavoriteBackupDriver destinationDriver = null;
	
	Map<String, Favorite> srcFavorites;
	Map<String, Favorite> dstFavorites;
	
	// implement get, getAll, update, create
	private FavoriteDAO createFavoriteDAO(final Map<String, Favorite> favorites) {
		return (FavoriteDAO)Proxy.newProxyInstance(FavoriteBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{FavoriteDAO.class},
                new InvocationHandler() {
					private long nextPrincipalKey = 0;
					private long nextEntityKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(FavoriteDAO.class.getMethod("getIndividualFavorite", String.class, String.class))) {
							String principalId = (String) args[0];
							String entityId = (String) args[1];							
							Favorite favorite = favorites.get(UserProfileUtils.getFavoriteId(principalId, entityId));
							if (favorite==null) throw new NotFoundException();
							return favorite;
						} else if (method.equals(FavoriteDAO.class.getMethod("add", Favorite.class))) {
							Favorite favorite = (Favorite)args[0];
							if (UserProfileUtils.getFavoriteId(favorite)==null) {
								if (favorites.containsKey(""+UserProfileUtils.getFavoriteId(String.valueOf(nextPrincipalKey), String.valueOf(nextEntityKey)))) throw new IllegalStateException();
								favorite.setPrincipalId(String.valueOf(nextPrincipalKey++));
								favorite.setEntityId(String.valueOf(nextEntityKey++));
							} else {
								if (favorites.containsKey(UserProfileUtils.getFavoriteId(favorite))) throw new  RuntimeException("already exists");
								nextPrincipalKey = Long.parseLong(favorite.getPrincipalId())+1;
								nextEntityKey = Long.parseLong(favorite.getEntityId())+1;
							}
							favorites.put(UserProfileUtils.getFavoriteId(favorite), favorite);
							return favorite;
						} else if(method.equals(FavoriteDAO.class.getMethod("getIds"))) {
							List<String> results = new ArrayList<String>();
							for (String id : favorites.keySet()) results.add(id);
							return results;						
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	private static Random rand = new Random();
		
	@Before
	public void before() throws Exception {
		srcFavorites = new HashMap<String, Favorite>();
		dstFavorites = new HashMap<String, Favorite>();
		FavoriteDAO srcFavoriteDAO = createFavoriteDAO(srcFavorites);
		Favorite favorite = new Favorite();
		favorite = srcFavoriteDAO.add(favorite);		
		assertNotNull(favorite.getPrincipalId());
		assertNotNull(favorite.getEntityId());
		
		favorite = new Favorite();
		favorite = srcFavoriteDAO.add(favorite);
		assertNotNull(favorite.getPrincipalId());
		assertNotNull(favorite.getEntityId());

		assertEquals(2, srcFavorites.size());

		FavoriteDAO dstFavoriteDAO = createFavoriteDAO(dstFavorites);
		sourceDriver = new FavoriteBackupDriver(srcFavoriteDAO);
		destinationDriver = new FavoriteBackupDriver(dstFavoriteDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("FavoriteBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			Set<String> ids = new HashSet<String>(); 
			for (String key : srcFavorites.keySet()) ids.add(key.toString());
			sourceDriver.writeBackup(temp, progress, ids);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstFavorites.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcFavorites, dstFavorites);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}	

}
