package org.sagebionetworks.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sagebionetworks.client.DataUploader;
import org.sagebionetworks.client.DataUploaderMultipartImpl;
import org.sagebionetworks.client.ProgressListener;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.TextProgressListener;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Locationable;

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

	Locationable doUpload() throws SynapseException {
		Synapse synapse = commandLineInterface.getSynapseClient();
		File file = new File(filePath);
		Locationable locationable = (Locationable) synapse
				.getEntityById(entityId);

		ProgressListener progressListener = new TextProgressListener();
		DataUploader uploader = new DataUploaderMultipartImpl();
		uploader.setProgressListener(progressListener);
		synapse.setDataUploader(uploader);

		return synapse.uploadLocationableToSynapse(locationable, file);
	}

	/**
	 * @param args
	 * @throws SynapseException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String args[]) throws SynapseException, FileNotFoundException, IOException {
		UploadFile uploadTool = new UploadFile(args);
		Locationable locationable = uploadTool.doUpload();
		System.out.println("Successfully updated: " + locationable.toString());
	}
}
