package org.sagebionetworks.javadoc.velocity;

import java.util.List;

import com.sun.javadoc.RootDoc;

/**
 * Abstraction for a generator the produces ClassContext for classes found in a Java Doc RootDoc.
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
	public List<ClassContext> generateContext(ContextFactory factory, RootDoc root) throws Exception;
}
