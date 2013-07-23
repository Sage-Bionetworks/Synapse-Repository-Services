package org.sagebionetworks.client.fileuploader;

import java.io.IOException;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.wtk.Window;

public class WidgetFactory {

	public static final FileUploader createFileUploader() throws IOException, SerializationException {
		BXMLSerializer bxmlSerializer = new BXMLSerializer();
        Window window = (Window)bxmlSerializer.readObject(FileUploaderViewImpl.class, "FileUploaderViewImpl.bxml");
        FileUploader fileUploader = new FileUploader((FileUploaderViewImpl) window);        
        return fileUploader;
	}
	
}
