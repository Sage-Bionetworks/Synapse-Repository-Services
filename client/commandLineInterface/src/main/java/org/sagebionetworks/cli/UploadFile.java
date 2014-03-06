package org.sagebionetworks.cli;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * @author deflaux
 * 
 */
public class UploadFile {
	CommandLineInterface commandLineInterface;
	String filePath;
	String entityId;

	UploadFile(String args[]) throws SynapseException {
		
		commandLineInterface = new CommandLineInterface("Upload Tool");

		commandLineInterface.addOption("i", "entityId", true,
						"the id of the Synapse entity to which we want to upload this file", true);
		commandLineInterface.addOption("f", "file", true,
				"the path of the file we wish to upload", true);

		commandLineInterface.processArguments(args);

		filePath = commandLineInterface.getArgument("file");
		entityId = commandLineInterface.getArgument("entityId");
	}

	/**
	 * @param args
	 * @throws SynapseException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String args[]) throws Exception {
		throw new Exception("No longer supported.");
	}
}
