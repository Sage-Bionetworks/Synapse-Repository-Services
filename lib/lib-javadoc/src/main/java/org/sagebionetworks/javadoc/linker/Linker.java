package org.sagebionetworks.javadoc.linker;

import java.io.File;
import java.util.List;

/**
 * The job of the linker it to revolve all hyper links within files to their actual files.
 * While files are being processed they link to each other using symbolic links.  That
 * way a file processor does not need to know where the final files will reside.
 * The Job of liker is to resolve the symbolic links to actual links.
 * 
 * @author jmhill
 *
 */
public interface Linker {
	
	/**
	 * For each FileLink, replace all symbolic links in the file with actual links to real files
	 * @param toLink
	 * @throws Exception 
	 */
	public void link(File baseDirectory, List<FileLink> toLink) throws Exception;

}
