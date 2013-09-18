package org.sagebionetworks.repo.model.util;

import java.util.HashSet;
import java.util.Random;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;

/**
 * A utility for generating a random ACL.
 * @author jmhill
 *
 */
public class RandomAccessControlListUtil {
	
	/**
	 * Create a randomly generated ACL using the passed seed.
	 * @param seed
	 * @return
	 */
	public static AccessControlList generateRandom(long seed){
		Random rand = new Random(seed);
		return generateRandom(rand);
	}

	/**
	 * Create a randomly generated ACL using the Random Object.
	 * @param rand
	 * @return
	 */
	public static AccessControlList generateRandom(Random rand) {
		AccessControlList acl = new AccessControlList();
		acl.setId(""+rand.nextLong());
		acl.setCreationDate(RandomUtils.createRandomStableDate(rand));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		int countToAdd = rand.nextInt(5);
		for(int i=0; i<countToAdd; i++){
			ResourceAccess ra = new ResourceAccess();
			//ra.setGroupName(DEFAULT_GROUPS.values()[rand.nextInt(DEFAULT_GROUPS.values().length)].name());
			ra.setPrincipalId(rand.nextLong());
			ra.setAccessType(new HashSet<ACCESS_TYPE>());
			int accesCount = rand.nextInt(5);
			for(int j=0; j<accesCount; j++){
				ACCESS_TYPE type = ACCESS_TYPE.values()[rand.nextInt(ACCESS_TYPE.values().length)];
				ra.getAccessType().add(type);
			}
			acl.getResourceAccess().add(ra);
		}
		return acl;
	}

}
