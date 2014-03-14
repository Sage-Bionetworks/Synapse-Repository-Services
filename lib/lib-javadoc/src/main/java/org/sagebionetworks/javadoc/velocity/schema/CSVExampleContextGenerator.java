package org.sagebionetworks.javadoc.velocity.schema;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;

import com.sun.javadoc.RootDoc;

/**
 * 
 * @author jmhill
 *
 */
public class CSVExampleContextGenerator implements ClassContextGenerator {

	@Override
	public List<ClassContext> generateContext(ContextFactory factory,
			RootDoc root) throws Exception {
		List<ClassContext> list = new LinkedList<ClassContext>();
		return list;
	}

}
