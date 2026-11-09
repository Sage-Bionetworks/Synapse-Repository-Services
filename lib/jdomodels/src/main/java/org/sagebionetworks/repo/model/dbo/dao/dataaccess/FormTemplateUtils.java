package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class FormTemplateUtils {

	private static final Logger LOG = LogManager.getLogger(FormTemplateUtils.class);

	/**
	 * @param templateJson The serialized body of a template version, as stored in FORM_TEMPLATE_REVISION.TEMPLATE_JSON.
	 * @return The ids of the file handles listed in the attachments of the version. An id that is not a
	 *         number is skipped, so that a single bad version cannot fail a scan of all versions.
	 */
	public static Set<Long> extractAttachmentFileHandleIds(String templateJson) {
		Set<Long> fileHandleIds = new HashSet<>();
		FormTemplate template = JDOSecondaryPropertyUtils.createObjectFromJSON(FormTemplate.class, templateJson);
		if (template == null || template.getAttachments() == null) {
			return fileHandleIds;
		}
		for (String attachment : template.getAttachments()) {
			try {
				fileHandleIds.add(Long.valueOf(attachment));
			} catch (NumberFormatException e) {
				LOG.warn("Malformed attachment file handle id {} in form template {}", attachment, template.getId());
			}
		}
		return fileHandleIds;
	}

}
