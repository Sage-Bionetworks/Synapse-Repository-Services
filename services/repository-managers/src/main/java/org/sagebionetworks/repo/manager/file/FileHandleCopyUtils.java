package org.sagebionetworks.repo.manager.file;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRecord;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.util.ValidateArgument;

public class FileHandleCopyUtils {

	/**
	 * Extract the list of FileHandleAssociation in the given BatchFileHandleCopyRequest
	 * 
	 * @param batch
	 * @return
	 */
	public static List<FileHandleAssociation> getOriginalFiles(BatchFileHandleCopyRequest batch) {
		ValidateArgument.required(batch, "batch");
		ValidateArgument.required(batch.getCopyRequests(), "BatchFileHandleCopyRequest.copyRequests");
		List<FileHandleAssociation> list = new LinkedList<FileHandleAssociation>();
		for (FileHandleCopyRequest request : batch.getCopyRequests()) {
			list.add(request.getOriginalFile());
		}
		return list;
	}

	/**
	 * Create and return a map from an original FileHandle ID to the request
	 * 
	 * @require no duplicate ID in the given batch
	 * @param batch
	 * @return
	 */
	public static Map<String, FileHandleCopyRequest> getRequestMap(BatchFileHandleCopyRequest batch) {
		ValidateArgument.required(batch, "batch");
		ValidateArgument.required(batch.getCopyRequests(), "BatchFileHandleCopyRequest.copyRequests");
		Map<String, FileHandleCopyRequest> map = new HashMap<String, FileHandleCopyRequest>();
		for (FileHandleCopyRequest request : batch.getCopyRequests()) {
			map.put(request.getOriginalFile().getFileHandleId(), request);
		}
		return map;
	}

	/**
	 * Create a new FileHandle object with a given FileHandle object.
	 * Overwrite etag, id, createdOn, and createdBy fields.
	 * Overwrite fileName and contentType fields if the new ones are set.
	 * 
	 * @param userId
	 * @param original
	 * @param overwriteData
	 * @param newId
	 * @return
	 */
	public static FileHandle createCopy(String userId, FileHandle original, FileHandleCopyRequest overwriteData, String newId) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(original, "original");
		ValidateArgument.required(overwriteData, "overrideData");
		ValidateArgument.required(newId, "newId");
		FileHandle newFileHandle = original;
		newFileHandle.setId(newId);
		if (overwriteData.getNewFileName() != null) {
			newFileHandle.setFileName(overwriteData.getNewFileName());
		}
		if (overwriteData.getNewContentType() != null) {
			newFileHandle.setContentType(overwriteData.getNewContentType());
		}
		newFileHandle.setEtag(UUID.randomUUID().toString());
		newFileHandle.setCreatedOn(new Date());
		newFileHandle.setCreatedBy(userId);
		if (newFileHandle instanceof CloudProviderFileHandleInterface) {
			((CloudProviderFileHandleInterface) newFileHandle).setPreviewId(null);
		}
		return newFileHandle;
	}

	/**
	 * Returns true if there is at least one duplicate FilehandleAssociation object found;
	 * false otherwise.
	 * 
	 * @param requestedFiles
	 * @return
	 */
	public static boolean hasDuplicates(List<FileHandleAssociation> requestedFiles) {
		ValidateArgument.required(requestedFiles, "requestedFiles");
		Set<String> seen = new HashSet<String>(requestedFiles.size());
		for (FileHandleAssociation fha : requestedFiles) {
			if (seen.contains(fha.getFileHandleId())) {
				return true;
			}
			seen.add(fha.getFileHandleId());
		}
		return false;
	}

	/**
	 * Create a record that captures info about FileHandle copy operation.
	 * 
	 * @param userId
	 * @param newFileHandleId
	 * @param originalFile
	 * @return
	 */
	public static FileHandleCopyRecord createCopyRecord(String userId, String newFileHandleId, FileHandleAssociation originalFile) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(newFileHandleId, "newFileHandleId");
		ValidateArgument.required(originalFile, "originalFile");
		FileHandleCopyRecord record = new FileHandleCopyRecord();
		record.setUserId(userId);
		record.setOriginalFileHandle(originalFile);
		record.setNewFileHandleId(newFileHandleId);
		return record;
	}
}
