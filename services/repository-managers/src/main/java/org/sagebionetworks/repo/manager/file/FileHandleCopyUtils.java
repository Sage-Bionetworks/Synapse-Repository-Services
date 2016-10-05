package org.sagebionetworks.repo.manager.file;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.util.ValidateArgument;

public class FileHandleCopyUtils {

	public static List<FileHandleAssociation> getOriginalFiles(BatchFileHandleCopyRequest batch) {
		ValidateArgument.required(batch, "batch");
		ValidateArgument.required(batch.getCopyRequests(), "BatchFileHandleCopyRequest.copyRequests");
		List<FileHandleAssociation> list = new LinkedList<FileHandleAssociation>();
		for (FileHandleCopyRequest request : batch.getCopyRequests()) {
			list.add(request.getOriginalFile());
		}
		return list;
	}

	public static Map<String, FileHandleOverwriteData> getFileHandleOverwriteData(BatchFileHandleCopyRequest batch) {
		ValidateArgument.required(batch, "batch");
		ValidateArgument.required(batch.getCopyRequests(), "BatchFileHandleCopyRequest.copyRequests");
		Map<String, FileHandleOverwriteData> map = new HashMap<String, FileHandleOverwriteData>();
		for (FileHandleCopyRequest request : batch.getCopyRequests()) {
			FileHandleOverwriteData data = new FileHandleOverwriteData();
			if (request.getNewFileName() != null) {
				data.setNewFileName(request.getNewFileName());
			}
			if (request.getNewContentType() != null) {
				data.setNewContentType(request.getNewContentType());
			}
			map.put(request.getOriginalFile().getFileHandleId(), data);
		}
		return map;
	}

	public static FileHandle createCopy(String userId, FileHandle original, FileHandleOverwriteData overwriteData, IdGenerator idGenerator) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(original, "original");
		ValidateArgument.required(overwriteData, "overrideData");
		ValidateArgument.required(idGenerator, "idGenerator");
		FileHandle newFileHandle = original;
		newFileHandle.setId(""+idGenerator.generateNewId(TYPE.FILE_IDS));
		if (overwriteData.getNewFileName() != null) {
			newFileHandle.setFileName(overwriteData.getNewFileName());
		}
		if (overwriteData.getNewContentType() != null) {
			newFileHandle.setContentType(overwriteData.getNewContentType());
		}
		newFileHandle.setEtag(UUID.randomUUID().toString());
		newFileHandle.setCreatedOn(new Date());
		newFileHandle.setCreatedBy(userId);
		return newFileHandle;
	}

	public static boolean hasDuplicates(List<FileHandleAssociation> requestedFiles) {
		ValidateArgument.required(requestedFiles, "requestedFiles");
		Set<FileHandleAssociation> seen = new HashSet<FileHandleAssociation>(requestedFiles.size());
		for (FileHandleAssociation fha : requestedFiles) {
			if (seen.contains(fha)) {
				return true;
			}
			seen.add(fha);
		}
		return false;
	}
}
