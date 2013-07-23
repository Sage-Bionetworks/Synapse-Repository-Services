package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.util.List;

public interface FileUploaderView {

	public interface Presenter {
		
		void uploadFiles(List<File> files);
		
		UploadStatus getFileUplaodStatus(File file);
	}

	public void setPresenter(Presenter presenter);
	
	public void alert(String message);

	public void updateFileStatus();
	
	public void setUploadingIntoMessage(String message);
	
}
