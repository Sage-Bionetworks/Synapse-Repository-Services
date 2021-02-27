package org.sagebionetworks.repo.manager.form;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class FormFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private FormDao formDao;
	
	@Autowired
	public FormFileHandleAssociationProvider(FormDao formDao) {
		this.formDao = formDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.FormData;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String formDataId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(formDataId, "formDataId");
		String formFileHandleId = formDao.getFormDataFileHandleId(formDataId);
		if(fileHandleIds.contains(formFileHandleId)) {
			return Sets.newHashSet(formFileHandleId);
		}else {
			return Collections.emptySet();
		}
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.FORM_DATA;
	}

}
