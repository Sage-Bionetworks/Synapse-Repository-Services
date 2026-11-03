package org.sagebionetworks.repo.model.dbo.file.download.v2;

import org.sagebionetworks.repo.model.download.DownloadListItem;

/**
 * A file that can be added to a download list, with the version it refers to and the size of its
 * content.
 *
 * @param item          The file, as it is added to a download list.
 * @param actualVersion The version the item refers to now: its own version, or the file's current
 *                      version when the item has none.
 * @param sizeBytes     The size of the file's content in bytes. Zero when the file's revision or file
 *                      handle does not exist.
 */
public record FileAndSize(DownloadListItem item, long actualVersion, long sizeBytes) {
}
