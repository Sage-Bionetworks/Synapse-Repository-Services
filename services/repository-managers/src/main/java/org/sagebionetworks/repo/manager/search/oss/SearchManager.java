package org.sagebionetworks.repo.manager.search.oss;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import java.io.IOException;
import java.util.List;


public interface SearchManager {

    /**
     * Creates/deletes a document based on Entity or Wiki changes that occurred in Synapse. Used by SearchIndexWorker.
     * @param changeMessages a batch of ChangeMessages representing changes in Synapse.
     */
    void documentChangeMessages(List<ChangeMessage> changeMessages);

    /**
     * Returns whether a document exists for a given Synapse id and etag
     * @param id id of the Synapse entity.
     * @param etag etag od Synapse entity.
     * @return true if a document exists, false otherwise.
     */
    boolean doesDocumentExist(String id, String etag) throws IOException;

}
