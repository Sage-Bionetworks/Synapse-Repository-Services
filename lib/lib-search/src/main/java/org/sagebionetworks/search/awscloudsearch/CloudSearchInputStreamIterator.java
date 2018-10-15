package org.sagebionetworks.search.awscloudsearch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.search.SearchUtil;

public class CloudSearchInputStreamIterator implements Iterator<InputStream> {

	public static final int MEGABYTE = (int) Math.pow(10, 6); //use base-10 value since it is smaller than base-2
	public static final int MAX_SINGLE_DOCUMENT_SIZE = MEGABYTE; //1MB
	public static final int MAX_DOCUMENT_BATCH_SIZE = 5 * MEGABYTE; //5MB

	private static final byte[] PREFIX_BYTES = "[".getBytes(StandardCharsets.UTF_8);
	private static final byte[] SUFFIX_BYTES = "]".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DELIMITER_BYTES = ",".getBytes(StandardCharsets.UTF_8);

	//this is set by the inner class enumberation
	private byte[] unreadBytes;


	Iterator<Document> documentIterator;


	//used to store documents which, by themselves, would exceed CloudSearch's limit
	List<Document> exceedSingleDocumentSizeLimit;

	SequenceInputStream currSeqInputStream;

	public CloudSearchInputStreamIterator(Iterator<Document> documentIterator){
		this.documentIterator =  documentIterator;

		this.exceedSingleDocumentSizeLimit = new LinkedList<>();

		this.currSeqInputStream = null;

		this.unreadBytes = null;
	}

	@Override
	public boolean hasNext() {
		return nextInputStream();
	}

	@Override
	public InputStream next() {
		if (hasNext()){
			return this.currSeqInputStream;
		} else {
			throw new NoSuchElementException();
		}
	}

	void nextInputStream(){
		this.currSeqInputStream = new SequenceInputStream(new Enumeration<InputStream>() {
			int availableBytes = MAX_DOCUMENT_BATCH_SIZE - (PREFIX_BYTES.length + SUFFIX_BYTES.length); //reserve some byte space for prefix and suffix
			byte[] nextByteArray = null;
			boolean prefixBytesAdded = false;

			@Override
			public boolean hasMoreElements() {//TODO: refactor boolean zen
				if(nextByteArray != null){//the next byte array has already been processed
					return true;
				}
				if(!documentIterator.hasNext()){ // nothing else to get from the document iterator
					return false;
				}

				byte[] documentBytes = SearchUtil.(documentIterator.next());


			}

			@Override
			public InputStream nextElement() {
				if (hasMoreElements()){
					InputStream inputStream = new ByteArrayInputStream(nextByteArray);
					nextByteArray = null;
					return inputStream;
				} else {
					throw new NoSuchElementException();
				}
			}


		});
	}

}
