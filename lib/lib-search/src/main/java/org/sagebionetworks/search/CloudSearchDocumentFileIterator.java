package org.sagebionetworks.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.io.output.CountingOutputStream;
import org.sagebionetworks.repo.model.search.Document;

/**
 * Iterator that lazily generates document batch files for CloudSearch.
 * This class is not thread-safe.
 */
public class CloudSearchDocumentFileIterator implements Iterator<File> {

	private static final int MEBIBYTE = 1048576; // CloudSearch's Limits say MB but they really mean MiB
	private static final int DEFAULT_MAX_SINGLE_DOCUMENT_SIZE = MEBIBYTE; //1MiB
	private static final int DEFAULT_MAX_DOCUMENT_BATCH_SIZE = 5 * MEBIBYTE; //5MiB

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	static final byte[] PREFIX_BYTES = "[".getBytes(CHARSET);
	static final byte[] SUFFIX_BYTES = "]".getBytes(CHARSET);
	static final byte[] DELIMITER_BYTES = ",".getBytes(CHARSET);

	//maximu bytes
	private final int maxSingleDocumentSizeInBytes;
	private final int maxDocumentBatchSizeInBytes;
	private final Iterator<Document> documentIterator;

	//used to carry over bytes that would not fit in the previous file into the next file
	private byte[] unwrittenDocumentBytes;

	//place holder for result that next() will consume and reset to null.
	private File currFile;

	public CloudSearchDocumentFileIterator(Iterator<Document> documentIterator, final int maxSingleDocumentSizeInBytes, final int maxDocumentBatchSizeInBytes){
		if (maxSingleDocumentSizeInBytes + PREFIX_BYTES.length + SUFFIX_BYTES.length > maxDocumentBatchSizeInBytes){
			throw new IllegalArgumentException("maxSingleDocumentSizeInBytes + " + (PREFIX_BYTES.length + SUFFIX_BYTES.length) + " must be less or equal to than maxDocumentBatchSizeInBytes");
		}

		this.documentIterator =  documentIterator;
		this.maxSingleDocumentSizeInBytes = maxSingleDocumentSizeInBytes;
		this.maxDocumentBatchSizeInBytes = maxDocumentBatchSizeInBytes;

		this.currFile = null;
		this.unwrittenDocumentBytes = null;
	}

	public CloudSearchDocumentFileIterator(Iterator<Document> documentIterator){
		this(documentIterator, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE, DEFAULT_MAX_DOCUMENT_BATCH_SIZE);
	}

	@Override
	public boolean hasNext() {
		if(currFile != null){
			return true;
		}

		try {
			File processedFile = processDocumentFile();
			if(processedFile != null){
				this.currFile = processedFile;
				return true;
			}
		} catch (IOException e){
			throw new RuntimeException(e);
		}

		return false;
	}

	@Override
	public File next() {
		if (hasNext()){
			File temp = this.currFile;
			//reset
			this.currFile = null;
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
	private File processDocumentFile() throws IOException{
		//no work to be done since the document iterator is exhausted and no left over bytes from previous
		if( !this.documentIterator.hasNext() && unwrittenDocumentBytes == null){
			return null;
		}

		File tempFile = File.createTempFile("CloudSearchDocument", ".json");

		try (CountingOutputStream countingOutputStream = new CountingOutputStream(new FileOutputStream(tempFile)) ) {
			//append prefix
			countingOutputStream.write(PREFIX_BYTES);

			//unwritten bytes from previous call to processDocumentFile(). These bytes are guaranteed to fit within the size limit.
			if(this.unwrittenDocumentBytes != null){
				countingOutputStream.write(unwrittenDocumentBytes);
				this.unwrittenDocumentBytes = null;
			}

			while (this.documentIterator.hasNext()) {
				Document doc = this.documentIterator.next();
				byte[] documentBytes = SearchUtil.convertSearchDocumentToJSONString(doc).getBytes(CHARSET);

				//if a single document exceeds the single document size limit throw exception
				if (documentBytes.length > maxSingleDocumentSizeInBytes) {
					countingOutputStream.close();
					tempFile.delete();
					throw new RuntimeException("The document for " + doc.getId() + " is " + documentBytes.length + " bytes and exceeds the maximum allowed " + maxSingleDocumentSizeInBytes + " bytes.");
				}

				//determine how many bytes need to be written
				boolean isNotFirstDocument = countingOutputStream.getByteCount() > PREFIX_BYTES.length;
				int bytesToBeAdded = (int) countingOutputStream.getByteCount() + documentBytes.length + SUFFIX_BYTES.length; //always reserve space for the suffix
				if(isNotFirstDocument) {
					bytesToBeAdded += DELIMITER_BYTES.length;
				}

				//check if there is enough space to write the bytes
				if(bytesToBeAdded <= maxDocumentBatchSizeInBytes){
					if(isNotFirstDocument){ // if not first element add delimiter
						countingOutputStream.write(DELIMITER_BYTES);
					}
					countingOutputStream.write(documentBytes);
				} else {
					this.unwrittenDocumentBytes = documentBytes;
					break;
				}
			}

			//append suffix
			countingOutputStream.write(SUFFIX_BYTES);
		}

		return tempFile;
	}

}
