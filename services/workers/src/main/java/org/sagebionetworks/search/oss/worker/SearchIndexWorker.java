package org.sagebionetworks.search.oss.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.search.oss.SearchManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This worker updates the OpenSearch index based on messages received
 *
 * @author Sandhra
 */
@Service
public class SearchIndexWorker implements BatchChangeMessageDrivenRunner {

    static private Logger log = LogManager.getLogger(SearchIndexWorker.class);

    @Autowired
    private SearchManager searchManager;


    @Override
    public void run(ProgressCallback progressCallback, List<ChangeMessage> changes) throws RecoverableMessageException {
        try {
            searchManager.documentChangeMessages(changes);
        } catch (RecoverableMessageException e) {
            throw e;
        } catch (Throwable e) {
            log.error("SearchIndexWorker Failed", e);
            throw e;
        }
    }
}
