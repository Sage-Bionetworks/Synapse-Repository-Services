package org.sagebionetworks.client.fileuploader;

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
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileUploader implements FileUploaderView.Presenter {
	private static final int MAX_NAME_CHARS = 40;

	private static int fileIdSequence = 0;

	private FileUploaderView view;
    private Synapse synapseClient;
    private String parentId;
    private final ExecutorService uploadPool = Executors.newFixedThreadPool(2);
	private Map<Integer,File> idToFile = new HashMap<Integer, File>();
	private Map<Future<FileEntity>,Integer> futureToId = new HashMap<Future<FileEntity>, Integer>();
	private Map<File, UploadStatus> fileStatus = new HashMap<File, UploadStatus>();
	private Set<Future<FileEntity>> unfinished = new HashSet<Future<FileEntity>>();
	UserSessionData userSessionData;
	
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
			userSessionData = synapseClient.getUserSessionData();
		} catch (SynapseException e) {
			if(e instanceof SynapseUnauthorizedException) {
				view.alert("Your Synapse session has expired. Please reload.");				
			} else {
				view.alert("An Error Occured. Please reload.");
			}
			return;
		}
		
		try {
			Entity parent = synapseClient.getEntityById(parentId);
			if(parent != null) {
				String message = parent.getName();
				if(message.length() > MAX_NAME_CHARS) message = message.substring(0, MAX_NAME_CHARS) + "...";
				message += " ("+ parentId +")";
				view.setUploadingIntoMessage(message);
			} else {
				view.alert("Upload to is null. Please reload.");
			}
		} catch (SynapseException e) {
			if(e instanceof SynapseNotFoundException) {
				view.alert("Upload target Not Found: " + parentId);				
			} else if(e instanceof SynapseForbiddenException) {
				String userName = "(undefined)";
				if(userSessionData != null && userSessionData.getProfile() != null) 
					userName = userSessionData.getProfile().getDisplayName();
				view.alert("Access Denied to " + parentId + "for user " + userName);
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
					(fileStatus.get(file) == UploadStatus.WAITING_TO_UPLOAD 
					|| fileStatus.get(file) == UploadStatus.UPLOADING 
					|| fileStatus.get(file) == UploadStatus.UPLOADED)) continue; // don't reupload files in the list
			
			setFileStatus(file, UploadStatus.WAITING_TO_UPLOAD);
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
			unfinished.add(uploadedFuture);
		}
		
		// check for completed jobs
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {			
			@Override
			public void run() {
				for(Future<FileEntity> future : futureToId.keySet()) {
					if(future.isDone()) {
						unfinished.remove(future);
						File file = idToFile.get(futureToId.get(future));
						try {
							future.get();
							setFileStatus(file, UploadStatus.UPLOADED);
						} catch (Exception e) {
							setFileStatus(file, UploadStatus.FAILED);
						}
					}
				}
				if(unfinished.isEmpty()) timer.cancel();
			}
		}, 0, 500L); // update every 1/2 second		
	}
	
	@Override
	public UploadStatus getFileUplaodStatus(File file) {
		if(!fileStatus.containsKey(file)) return UploadStatus.NOT_UPLOADED;
		else return fileStatus.get(file);
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
		view.updateFileStatus();
	}
	
}
