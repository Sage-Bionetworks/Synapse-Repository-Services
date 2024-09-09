package org.sagebionetworks.javadoc.velocity;

import java.util.List;

/**
 * Abstraction for a generator the produces ClassContext for classes found in a Java Doc DocletEnvironment.
 *  
 * @author John
 *
 */
public interface ClassContextGenerator {

	/**
	 * Given a Java Doc Root, implementation should generate a list of ClassContext for each 
	 * instance of a given type of class.
	 * @param factory
	 * @param root
	 * @return
	 * @throws Exception
	 */
	public List<ClassContext> generateContext(ContextInput input) throws Exception;
}
