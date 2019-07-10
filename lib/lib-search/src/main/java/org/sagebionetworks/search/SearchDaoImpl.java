package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String QUERY_BY_ID_AND_ETAG = "(and "+FIELD_ID+":'%1$s' "+FIELD_ETAG+":'%2$s')";

	private static final String QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE = "matchall";

	static private Logger log = LogManager.getLogger(SearchDaoImpl.class);

	@Autowired
	CloudSearchClientProvider cloudSearchClientProvider;


	@Override
	public void deleteDocuments(Set<String> docIdsToDelete) {
		ValidateArgument.required(docIdsToDelete,"docIdsToDelete");

		if(docIdsToDelete.isEmpty()){ //no work needs to be done
			return;
		}

		DateTime now = DateTime.now();
		// Note that we cannot use a JSONEntity here because the format is
		// just a JSON array
		List<Document> documentBatch = new ArrayList<>(docIdsToDelete.size());
		for (String entityId : docIdsToDelete) {
			Document document = new Document();
			document.setType(DocumentTypeNames.delete);
			document.setId(entityId);
			documentBatch.add(document);
		}
		// Delete the batch.
		sendDocuments(documentBatch.iterator());
	}

	@Override
	public void createOrUpdateSearchDocument(Document document){
		ValidateArgument.required(document, "document");
		cloudSearchClientProvider.getCloudSearchClient().sendDocument(document);
	}

	@Override
	public void sendDocuments(Iterator<Document> documentIterator){
		cloudSearchClientProvider.getCloudSearchClient().sendDocuments(documentIterator);
	}

	@Override
	public SearchResult executeSearch(SearchRequest search){
		return cloudSearchClientProvider.getCloudSearchClient().rawSearch(search);
	}

	@Override
	public boolean doesDocumentExistInSearchIndex(String id, String etag){
 		ValidateArgument.required(id, "id");

		// Search for the document
		String query = String.format(QUERY_BY_ID_AND_ETAG, id, etag);
		try {
			SearchResult results = executeSearch(new SearchRequest().withQuery(query).withQueryParser(QueryParser.Structured));
			return results.getHits().getFound() > 0;
		}catch (IllegalArgumentException e){
			Throwable cause = e.getCause();
			if (cause instanceof SearchException && StringUtils.contains(cause.getMessage(), "Syntax error in query: field")){
				//This exception is very likely caused by a race condition on the search index's schema. It should be resolved later
				throw new TemporarilyUnavailableException(e);
			}
			throw e;
		}
	}

	/**
	 * List all documents in the search index.
	 *
	 * @param limit
	 * @param offset
	 * @return
	 */
	SearchResult listSearchDocuments(long limit, long offset){
		return executeSearch(new SearchRequest().withQuery(QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE)
				.withQueryParser(QueryParser.Structured)
				.withSize(limit).withStart(offset));
	}

	@Override
	public void deleteAllDocuments() throws InterruptedException {
		// Keep deleting as long as there are documents
		SearchResult sr = null;
		do{
			sr = listSearchDocuments(1000, 0);
			HashSet<String> idSet = new HashSet<String>();
			for(Hit hit: sr.getHits().getHit()){
				idSet.add(hit.getId());
			}
			// Delete the whole set
			if(!idSet.isEmpty()){
				log.warn("Deleting the following documents from the search index:"+idSet.toString());
				deleteDocuments(idSet);
				Thread.sleep(5000);
			}
		}while(sr.getHits().getFound() > 0);
	}
}
