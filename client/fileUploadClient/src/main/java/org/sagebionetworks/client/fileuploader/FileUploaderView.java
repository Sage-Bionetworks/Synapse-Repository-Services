package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.util.List;

import org.apache.pivot.io.FileList;

public interface FileUploaderView {

	public interface Presenter {
		
		void uploadFiles();
		
		UploadStatus getFileUplaodStatus(File file);

		void addFilesForUpload(List<File> files);
	}

	public void showStagedFiles(List<File> files);
	
	public void setPresenter(Presenter presenter);
	
	public void alert(String message);

	public void updateFileStatus();
	
	public void setUploadingIntoMessage(String message);
	
}
