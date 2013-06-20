package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.util.List;

import org.sagebionetworks.javadoc.linker.FileLink;

import com.sun.javadoc.RootDoc;

public interface FileWriter {

	/**
	 * Write all of the files and return the meta data about the files that were created.
	 * 
	 * @param outputDirectory
	 * @param root
	 * @return
	 * @throws Exception 
	 */
	List<FileLink> writeAllFiles(File outputDirectory, RootDoc root) throws Exception;
}
