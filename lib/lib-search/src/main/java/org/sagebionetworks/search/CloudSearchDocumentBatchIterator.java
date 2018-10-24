package org.sagebionetworks.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.sagebionetworks.repo.model.search.Document;

/**
 * Iterator that lazily generates document batch files for CloudSearch.
 * This class is not thread-safe.
 */
public class CloudSearchDocumentBatchIterator implements Iterator<CloudSearchDocumentBatch> { //TODO rename to DocumentBatchIterator

	private final CloudSearchDocumentBuilderProvider builderProvider;

	private final Iterator<Document> documentIterator;

	//used to carry over documents that would not fit in the previous file into the next file
	private Document unwrittenDocument;

	//place holder for result that next() will consume and reset to null.
	private CloudSearchDocumentBatch currentBatch;


	public CloudSearchDocumentBatchIterator(Iterator<Document> documentIterator, CloudSearchDocumentBuilderProvider builderProvider){
		this.documentIterator =  documentIterator;
		this.builderProvider = builderProvider;

		this.currentBatch = null;
		this.unwrittenDocument = null;
	}


	@Override
	public boolean hasNext() {
		if(currentBatch != null){
			return true;
		}

		try {
			CloudSearchDocumentBatch processedFile = processDocumentFile();
			if(processedFile != null){
				this.currentBatch = processedFile;
				return true;
			}
		} catch (IOException e){
			throw new RuntimeException(e);
		}

		return false;
	}

	@Override
	public CloudSearchDocumentBatch next() {
		if (hasNext()){
			CloudSearchDocumentBatch temp = this.currentBatch;
			//reset
			this.currentBatch = null;
			return temp;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Generates the batch document file by using the documentIterator
	 * @return a File containing batch documents, null if no more documents can be written.
	 * @throws IOException
	 */
	CloudSearchDocumentBatch processDocumentFile() throws IOException{
		//no work to be done since the document iterator is exhausted and no left over bytes from previous
		if( !this.documentIterator.hasNext() && unwrittenDocument == null){
			return null;
		}


		try ( CloudSearchDocumentBatchBuilder builder = builderProvider.getBuilder()) {
			//unwritten bytes from previous call to processDocumentFile(). These bytes are guaranteed to fit within the size limit.
			if(this.unwrittenDocument != null){
				builder.tryAddDocument(unwrittenDocument);
				this.unwrittenDocument = null;
			}

			while (this.documentIterator.hasNext()) {
				Document doc = this.documentIterator.next();
				//try to add the document. If it would not fit, return the current batch
				if(!builder.tryAddDocument(doc)){
					this.unwrittenDocument = doc;
					break;
				}
			}
			return builder.build();
		}
	}

}
