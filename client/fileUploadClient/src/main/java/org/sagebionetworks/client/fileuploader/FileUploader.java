package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import net.sf.jmimemagic.Magic;

import org.apache.log4j.Logger;
import org.apache.pivot.wtk.Window;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserSessionData;

public class FileUploader implements FileUploaderView.Presenter {
	private static final Logger log = Logger.getLogger(FileUploader.class); 
	
	private static final int MAX_NAME_CHARS = 40;

	private static int fileIdSequence = 0;

	private FileUploaderView view;
	UploadFuturesFactory uploadFuturesFactory;
    private Synapse synapseClient;
    private String targetEntityId;
    private final ExecutorService uploadPool = Executors.newFixedThreadPool(2);
	private Map<Integer,File> idToFile = new HashMap<Integer, File>();
	private Map<Future<Entity>,Integer> futureToId = new HashMap<Future<Entity>, Integer>();
	private Map<File, UploadStatus> fileStatus = new HashMap<File, UploadStatus>();
	private Set<Future<Entity>> unfinished = new HashSet<Future<Entity>>();
	UserSessionData userSessionData;
	final Set<File> filesStagedForUpload = new HashSet<File>();
	private boolean singleFileMode;
	private boolean enabled;
	private Entity targetEntity;
	
	public FileUploader(FileUploaderView view, UploadFuturesFactory uploadFuturesFactory) {
		this.view = view;
		this.uploadFuturesFactory = uploadFuturesFactory;
		this.singleFileMode = true; // safest default
		this.enabled = true;
		view.setPresenter(this);		
	}
	
	public Window asWidget() {
		return (Window)view;
	}

	/**
	 * Configure Uploader with either a container entity or an entity to update (single file mode)
	 * @param synapseClient
	 * @param targetEntityId
	 */
	public void configure(Synapse synapseClient, String targetEntityId) {
		this.synapseClient = synapseClient;
		this.targetEntityId = targetEntityId;
		
		// get user profile
		if(!getUserSessionData(synapseClient)) return;		
		getTargetEntity(synapseClient, targetEntityId);		
	}

	/**
	 * Mainly for testing
	 * @return
	 */
	public Set<File> getStagedFilesForUpload() {
		return filesStagedForUpload;
	}
	
	/**
	 * Mainly for testing
	 * @return
	 */
	public Map<File, UploadStatus> getFileStatus() {
		return fileStatus;
	}
	
	/**
	 * Retrieve the upload status of a file
	 */
	@Override
	public UploadStatus getFileUplaodStatus(File file) {
		if(!fileStatus.containsKey(file)) return UploadStatus.NOT_UPLOADED;
		else return fileStatus.get(file);
	}

	/**
	 * Add files to be staged for uploading
	 */
	@Override
	public void addFilesForUpload(List<File> files) {				
		// flatten the hierarchy of files/folders given into a list
		final Pattern hiddenFile = Pattern.compile("^\\..*");
		SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {		
			@Override
		    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {		    	
		    	// ignore hidden files
		    	if(hiddenFile.matcher(path.getFileName().toString()).matches()) return FileVisitResult.CONTINUE;		    			    	
		    	if(!filesStagedForUpload.contains(path.toFile())) {
		    		filesStagedForUpload.add(path.toFile());
		    	}		    	
		        return FileVisitResult.CONTINUE;
		    }

		    @Override
		    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		    	//failedToVisit.add(file.toFile());
		        return FileVisitResult.CONTINUE;
		    }
		}; 
		
		// add user selected files to private list and add all files recursively to display list
		for (File file : files) {
		    try {
				Files.walkFileTree(file.toPath(), fileVisitor);
			} catch (IOException e) {
				log.error(e);
				view.alert("An error occurred. Please try reloading this applicaiton.");
			}		    
		}
		view.showStagedFiles(new ArrayList<File>(filesStagedForUpload));
	}

	/**
	 * Remove files from those staged to be uploaded
	 */
	@Override
	public void removeFilesFromUpload(List<File> files) {
		for(File file : files) {
			filesStagedForUpload.remove(file);
			fileStatus.remove(file);
		}
	}
	
	/**
	 * Upload staged files
	 */
	@Override
	public void uploadFiles() {
		// preconditions
		if(!enabled) return; // just in case
		if(singleFileMode && filesStagedForUpload.size() > 1) {
			view.alert("The Synapse File Uploader is in Single File Mode. Please reduce the number of files to 1.");
			return;
		}
				
		for (final File file : filesStagedForUpload) {									
			if(fileStatus.containsKey(file) && 
					(fileStatus.get(file) == UploadStatus.WAITING_TO_UPLOAD 
					|| fileStatus.get(file) == UploadStatus.UPLOADING 
					|| fileStatus.get(file) == UploadStatus.UPLOADED)) continue; // don't reupload files in the list
			
			setFileStatus(file, UploadStatus.WAITING_TO_UPLOAD);
			final String mimeType = getMimeType(file);
			// upload file
			StatusCallback statusCallback = new StatusCallback() {				
				@Override
				public void setStatus(UploadStatus status) {
					setFileStatus(file, status);
				}
			};
			
			Future<Entity> uploadedFuture;
			if(singleFileMode) {
				uploadedFuture = uploadFuturesFactory.createNewVersionFileEntityFuture(file, mimeType, uploadPool, synapseClient, (FileEntity)targetEntity, statusCallback);
			} else {				
				uploadedFuture = uploadFuturesFactory.createChildFileEntityFuture(file, mimeType, uploadPool, synapseClient, targetEntityId, statusCallback);  
			}
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
				for(Future<Entity> future : futureToId.keySet()) {
					if(future.isDone()) {
						unfinished.remove(future);
						File file = idToFile.get(futureToId.get(future));
						try {
							Entity entity = future.get();
							if(singleFileMode) targetEntity = entity;
							setFileStatus(file, UploadStatus.UPLOADED);
						} catch (Exception e) {
							log.error(e);
							if(e != null && e.getCause() != null && e.getCause() instanceof SynapseUserException && e.getMessage().contains("(409)")) {
								setFileStatus(file, UploadStatus.ALREADY_EXISTS);
							} else {
								setFileStatus(file, UploadStatus.FAILED);
							}
						}
					}
				}
				if(unfinished.isEmpty()) timer.cancel();
			}
		}, 0, 500L); // update every 1/2 second		
	}
	
	
	/*
	 * Private Methods
	 */
	private boolean getUserSessionData(Synapse synapseClient) {
		try {
			userSessionData = synapseClient.getUserSessionData();
		} catch (SynapseException e) {
			log.error(e);
			if(e instanceof SynapseUnauthorizedException) {
				view.alert("Your Synapse session has expired. Please reload.");				
			} else {
				view.alert("An Error Occured. Please reload.");
			}
			setEnabled(false);
			return false;
		}
		return true;
	}
	
	private void getTargetEntity(Synapse synapseClient, String targetEntityId) {
		try {
			targetEntity = synapseClient.getEntityById(targetEntityId);
			if(targetEntity != null) {
				// check if container
				if(targetEntity instanceof Project || targetEntity instanceof Folder) {
					singleFileMode = false;				
				}
				view.setSingleFileMode(singleFileMode);
				
				// Don't support old entity types that require repo upload proxy
				if(singleFileMode && !(targetEntity instanceof FileEntity)) {
					setEnabled(false);
					String entityType = targetEntity.getConcreteType().replaceAll(".+\\.", "");					
					view.alert("The File Uploader does not support upload to deprecated Entity type: "
							+ entityType
							+ ". Please recreate your entity as a FileEntity.");
				}
				
				// show entity in view
				String message = targetEntity.getName();
				if(message.length() > MAX_NAME_CHARS) message = message.substring(0, MAX_NAME_CHARS) + "...";
				message += " ("+ targetEntityId +")";
				view.setUploadingIntoMessage(message);
			} else {
				view.alert("Upload to is null. Please reload.");
			}
		} catch (SynapseException e) {
			log.error(e);
			if(e instanceof SynapseNotFoundException) {
				view.alert("Upload target Not Found: " + targetEntityId);				
			} else if(e instanceof SynapseForbiddenException) {
				String userName = "(undefined)";
				if(userSessionData != null && userSessionData.getProfile() != null) 
					userName = userSessionData.getProfile().getDisplayName();
				view.alert("Access Denied to " + targetEntityId + "for user " + userName + ". Please gain access and then reload.");
			} else {
				view.alert("An Error Occured. Please reload.");
			}
			setEnabled(false);
		}
	}
	
	private void setEnabled(boolean enabled) {
		this.enabled = enabled;
		view.setEnabled(enabled);
	}

	private String getMimeType(File file) {
		String mimeType = null;
		try {
			mimeType = Magic.getMagicMatch(file, false).getMimeType();
		} catch (Exception e) {
			log.error(e);
		}

		if(mimeType == null) mimeType = "text/plain";
		return mimeType;
	}

	private void setFileStatus(File file, UploadStatus status) {
		fileStatus.put(file, status);
		view.updateFileStatus();
	}
	
}
