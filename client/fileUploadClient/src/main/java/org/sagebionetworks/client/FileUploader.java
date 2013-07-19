package org.sagebionetworks.client;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sf.jmimemagic.Magic;

import org.apache.pivot.wtk.Window;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileUploader implements FileUploaderView.Presenter {
	private static int fileIdSequence = 0;

	private FileUploaderView view;
    private Synapse synapseClient;
    private String parentId;
    private final ExecutorService uploadPool = Executors.newFixedThreadPool(2);
	private Map<Integer,File> idToFile = new HashMap<Integer, File>();
	private Map<Future<FileEntity>,Integer> futureToId = new HashMap<Future<FileEntity>, Integer>();
	private Map<File, UploadStatus> fileStatus = new HashMap<File, UploadStatus>();
	
	
	public FileUploader(FileUploaderView view) {
		this.view = view;
		view.setPresenter(this);
	}
	
	public Window asWidget() {
		return (Window)view;
	}

	public void configure(Synapse synapseClient, String parentId) {
		this.synapseClient = synapseClient;
		this.parentId = parentId;
		
		try {
			synapseClient.getUserSessionData();
		} catch (SynapseException e) {
			if(e instanceof SynapseUnauthorizedException) {
				view.alert("Your Synapse session has expired. Please reload.");				
			} else {
				view.alert("An Error Occured. Please reload.");
			}
		}

		
	}

	@Override
	public void uploadFiles(List<File> files) {		
		for (int i = 0; i < files.size(); i++) {
			final File file = files.get(i);
			if(fileStatus.containsKey(file) && 
					(fileStatus.get(file) == UploadStatus.WAITING_FOR_UPLOAD 
					|| fileStatus.get(file) == UploadStatus.UPLOADING 
					|| fileStatus.get(file) == UploadStatus.UPLOADED)) continue; // don't reupload files in the list
			
			setFileStatus(file, UploadStatus.WAITING_FOR_UPLOAD);
			final String mimeType = getMimeType(file);
				// upload file
				Future<FileEntity> uploadedFuture = uploadPool.submit(new Callable<FileEntity>() {
					@Override
					public FileEntity call() throws Exception {
						// create filehandle via multipart upload (blocking)
						setFileStatus(file, UploadStatus.UPLOADING);
						S3FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType);

						// create child File entity under parent
						final FileEntity entity = new FileEntity();
						entity.setParentId(parentId);
						entity.setDataFileHandleId(fileHandle.getId());													
						return synapseClient.createEntity(entity);
					}
				}); 
				int id = ++fileIdSequence;
				idToFile.put(id, file);
				futureToId.put(uploadedFuture, id);
		}
		
		// check for completed jobs
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {			
			@Override
			public void run() {
				for(Future<FileEntity> future : futureToId.keySet()) {
					if(future.isDone()) {
						File file = idToFile.get(futureToId.get(future));
						try {
							future.get();
							setFileStatus(file, UploadStatus.UPLOADED);
						} catch (Exception e) {
							setFileStatus(file, UploadStatus.FAILED);
						}
						timer.cancel();
					}
				}
			}
		}, 0, 500L); // update every 1/2 second		
	}

	/*
	 * Private Methods
	 */
	private String getMimeType(File file) {
		String mimeType = null;
		try {
			mimeType = Magic.getMagicMatch(file, false).getMimeType();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(mimeType == null) mimeType = "text/plain";
		return mimeType;
	}

	private void setFileStatus(File file, UploadStatus status) {
		fileStatus.put(file, status);
	}

	
}
