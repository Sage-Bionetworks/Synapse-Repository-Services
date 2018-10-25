package org.sagebionetworks.search;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.output.CountingOutputStream;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;

public class CloudSearchDocumentBatchBuilder implements Closeable {

	private static final long MEBIBYTE = 1048576; // CloudSearch's Limits say MB but they really mean MiB
	private static final long DEFAULT_MAX_SINGLE_DOCUMENT_SIZE = MEBIBYTE; //1MiB
	private static final long DEFAULT_MAX_DOCUMENT_BATCH_SIZE = 5 * MEBIBYTE; //5MiB

	static final Charset CHARSET = StandardCharsets.UTF_8;
	static final byte[] PREFIX_BYTES = "[".getBytes(CHARSET);
	static final byte[] SUFFIX_BYTES = "]".getBytes(CHARSET);
	static final byte[] DELIMITER_BYTES = ",".getBytes(CHARSET);

	CloudSearchBatchBuilderResource builderResource;

	private final FileProvider fileProvider;
	//maximum bytes
	private final long maxSingleDocumentSizeInBytes;
	private final long maxDocumentBatchSizeInBytes;

	private CloudSearchDocumentBatch builtBatch;

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider) throws IOException {
		this(fileProvider, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE, DEFAULT_MAX_DOCUMENT_BATCH_SIZE);
	}

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider, long maxSingleDocumentSizeInBytes, long maxDocumentBatchSizeInBytes) throws IOException{
		if (maxSingleDocumentSizeInBytes + PREFIX_BYTES.length + SUFFIX_BYTES.length > maxDocumentBatchSizeInBytes){
			throw new IllegalArgumentException("maxSingleDocumentSizeInBytes + " + (PREFIX_BYTES.length + SUFFIX_BYTES.length) + " must be less or equal to than maxDocumentBatchSizeInBytes");
		}
		this.builtBatch = null;


		this.fileProvider = fileProvider;
		this.maxSingleDocumentSizeInBytes = maxSingleDocumentSizeInBytes;
		this.maxDocumentBatchSizeInBytes = maxDocumentBatchSizeInBytes;
	}

	boolean lazyInit() throws IOException {
		if(this.documentBatchFile != null){
			return false;
		}

		this.documentBatchFile = fileProvider.createTempFile("CloudSearchDocument", ".json");
		this.countingOutputStream = new CountingOutputStream(fileProvider.createFileOutputStream(documentBatchFile));
		this.documentIds = new HashSet<>();

		//initialize the file with prefix
		this.countingOutputStream.write(PREFIX_BYTES);
		return true;
	}

	/**
	 * Build the CloudSearchDocumentBatch
	 * @return the built documentBatch
	 * @throws IOException
	 */
	public CloudSearchDocumentBatch build() throws IOException {
		if(builtBatch != null){
			return builtBatch;
		}

		//append suffix
		countingOutputStream.write(SUFFIX_BYTES);
		countingOutputStream.flush();
		countingOutputStream.close();

		builtBatch = new CloudSearchDocumentBatchImpl(documentBatchFile, documentIds, countingOutputStream.getByteCount());
		return builtBatch;
	}

	/**
	 * Attempt to add document to the batch. Returns true if bytes could be added, false otherwise. Once false is returned,
	 * {@link #build()} should be called.
	 * @return true if bytes could be added, false otherwise
	 */
	public boolean tryAddDocument(Document document) throws IOException {
		ValidateArgument.required(document, "document");
		ValidateArgument.required(document.getId(), "document.Id");

		if (builtBatch != null){ //If it's already been built, don't add document
			throw new IllegalStateException("Build has already been called.");
		}

		byte[] documentBytes = SearchUtil.convertSearchDocumentToJSONString(document).getBytes(CHARSET);

		//if a single document exceeds the single document size limit throw exception
		if (documentBytes.length > maxSingleDocumentSizeInBytes) {
			throw new IllegalArgumentException("The document for " + document.getId() + " is " + documentBytes.length + " bytes and exceeds the maximum allowed " + maxSingleDocumentSizeInBytes + " bytes.");
		}

		//if this is not the first document to be added, we must check if it would fit in the batch
		if(!lazyInit()) {

			//determine how many bytes need to be written
			//always reserve space for the suffix because the next document being added may not fit into this document batch.
			long bytesToBeAdded =  DELIMITER_BYTES.length + documentBytes.length + SUFFIX_BYTES.length;

			// Check there is enough space to write into this batch
			if (countingOutputStream.getByteCount() + bytesToBeAdded > maxDocumentBatchSizeInBytes) {
				return false;
			}

			//add delimiter between previous document and the new document to be added.
			countingOutputStream.write(DELIMITER_BYTES);
		}

		countingOutputStream.write(documentBytes);

		documentIds.add(document.getId());
		return true;
	}

	@Override
	public void close() throws IOException {

	}




	static class CloudSearchBatchBuilderResource implements Closeable{
		File documentBatchFile;
		CountingOutputStream countingOutputStream;
		Set<String> documentIds;

		@Override
		public void close() throws IOException {
			if(countingOutputStream != null){
				countingOutputStream.close();
			}

			if(documentBatchFile != null) {
				documentBatchFile.delete();
			}
		}
	}
}
