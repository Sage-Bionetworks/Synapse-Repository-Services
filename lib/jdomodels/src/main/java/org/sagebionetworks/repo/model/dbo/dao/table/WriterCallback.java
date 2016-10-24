package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Callback for a writer to an output stream.
 * 
 * @author jhill
 *
 */
public interface WriterCallback {

	/**
	 * Write the data to the given output stream.
	 * @param out
	 * @throws IOException 
	 */
	void write(OutputStream out) throws IOException;
	
}
