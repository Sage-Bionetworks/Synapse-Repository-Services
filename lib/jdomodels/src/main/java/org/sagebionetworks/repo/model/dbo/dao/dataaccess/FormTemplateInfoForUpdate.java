package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

/**
 * The mutable state of a form template needed to validate and apply an update.
 *
 * @param id                   The id of the template.
 * @param etag                 The current etag, used for optimistic concurrency control.
 * @param currentVersionNumber The number of the latest version of the template.
 */
public record FormTemplateInfoForUpdate(Long id, String etag, Long currentVersionNumber) {

}
