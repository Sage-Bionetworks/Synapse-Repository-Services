package org.sagebionetworks.repo.manager.form;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class FormFileHandleAssociationProvider implements FileHandleAssociationProvider {

	@Autowired
	FormDao formDao;

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
