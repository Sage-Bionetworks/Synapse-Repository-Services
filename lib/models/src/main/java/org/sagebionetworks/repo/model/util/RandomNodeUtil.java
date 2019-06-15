package org.sagebionetworks.repo.model.util;

import java.util.Random;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;

/**
 * A utility to generate a random node.
 * @author jmhill
 *
 */
public class RandomNodeUtil {
	
	/**
	 * Generate a random node using the specified seed.
	 * 
	 * @param seed
	 * @return
	 */
	public static Node generateRandom(long seed){
		Random rand = new Random(seed);
		return generateRandom(rand);
	}

	/**
	 * Generate a random node using the Random object.
	 * @param rand
	 * @return
	 */
	public static Node generateRandom(Random rand) {
		Node node = new Node();
		node.setName("name"+rand.nextLong());
		node.setETag(""+rand.nextLong());
		node.setId(""+rand.nextLong());
		node.setModifiedByPrincipalId(rand.nextLong());
		node.setCreatedByPrincipalId(rand.nextLong());
		node.setCreatedOn(RandomUtils.createRandomStableDate(rand));
		node.setModifiedOn(RandomUtils.createRandomStableDate(rand));
		node.setVersionComment("comment: "+rand.nextLong());
		node.setVersionNumber(new Long(rand.nextInt(10)));
		node.setVersionLabel("0.0"+rand.nextDouble());
		int typeIndex = rand.nextInt(EntityType.values().length);
		node.setNodeType(EntityType.values()[typeIndex]);
		if(rand.nextBoolean()){
			node.setParentId(""+rand.nextLong());
		}
		return node;
	}

}
