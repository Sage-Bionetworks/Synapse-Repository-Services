package org.sagebionetworks.repo.model.dbo.file.download.v2;

import org.sagebionetworks.repo.model.download.DownloadListItem;

/**
 * A file that can be added to a download list, with the size of its content.
 *
 * @param item      The file, as it is added to a download list.
 * @param sizeBytes The size of the file's content in bytes. Zero when the file's revision or file
 *                  handle does not exist.
 */
public record FileAndSize(DownloadListItem item, long sizeBytes) {
}
