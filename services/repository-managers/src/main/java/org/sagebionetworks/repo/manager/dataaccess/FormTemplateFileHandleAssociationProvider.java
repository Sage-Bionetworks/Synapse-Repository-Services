package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.stereotype.Service;

/**
 * A file is associated with a form template when any version of the template, including a
 * deprecated one, lists it in its attachments.
 */
@Service
public class FormTemplateFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private final FormTemplateDao formTemplateDao;

	public FormTemplateFileHandleAssociationProvider(FormTemplateDao formTemplateDao) {
		this.formTemplateDao = formTemplateDao;
	}

	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.FormTemplateAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return formTemplateDao.getAttachmentFileHandleIds(Long.parseLong(objectId), fileHandleIds);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.FORM_TEMPLATE;
	}

}
