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

	private final FileProvider fileProvider;
	//maximum bytes
	private final long maxSingleDocumentSizeInBytes;
	private final long maxDocumentBatchSizeInBytes;

	private boolean builtBatch;
	private CloudSearchBatchBuilderResource builderResources;


	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider) throws IOException {
		this(fileProvider, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE, DEFAULT_MAX_DOCUMENT_BATCH_SIZE);
	}

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider, long maxSingleDocumentSizeInBytes, long maxDocumentBatchSizeInBytes) throws IOException {
		if (maxSingleDocumentSizeInBytes + PREFIX_BYTES.length + SUFFIX_BYTES.length > maxDocumentBatchSizeInBytes) {
			throw new IllegalArgumentException("maxSingleDocumentSizeInBytes + " + (PREFIX_BYTES.length + SUFFIX_BYTES.length) + " must be less or equal to than maxDocumentBatchSizeInBytes");
		}
		this.builtBatch = false;
		this.builderResources = null;


		this.fileProvider = fileProvider;
		this.maxSingleDocumentSizeInBytes = maxSingleDocumentSizeInBytes;
		this.maxDocumentBatchSizeInBytes = maxDocumentBatchSizeInBytes;
	}

	/**
	 * Initializes builder's resources the if necessary
	 * @return true if the resources needed to be initialized, false otherwise.
	 * @throws IOException
	 */
	boolean initIfNecessary() throws IOException {
		if(this.builderResources != null){
			return false;
		}

		builderResources = new CloudSearchBatchBuilderResource(fileProvider);

		//initialize the file with prefix
		builderResources.getCountingOutputStream().write(PREFIX_BYTES);
		return true;
	}

	/**
	 * Build the CloudSearchDocumentBatch
	 * @return the built documentBatch
	 * @throws IOException
	 */
	public CloudSearchDocumentBatch build() throws IOException {
		if (builtBatch){ // If it's already been built, don't add document
			throw new IllegalStateException("build() can only be called once");
		}
		if(builderResources == null){
			throw new IllegalStateException("at least one document must be added for a valid batch");
		}

		try {
			CountingOutputStream countingOutputStream = builderResources.getCountingOutputStream();
			//append suffix
			countingOutputStream.write(SUFFIX_BYTES);
			countingOutputStream.flush();
			countingOutputStream.close();

			builtBatch = true;
			return new CloudSearchDocumentBatchImpl(builderResources.getDocumentBatchFile(), builderResources.getDocumentIds(), countingOutputStream.getByteCount());
		} finally {
			builderResources = null;
		}
	}

	/**
	 * Attempt to add document to the batch. Returns true if bytes could be added, false otherwise. Once false is returned,
	 * {@link #build()} should be called.
	 * @return true if bytes could be added, false otherwise
	 */
	public boolean tryAddDocument(Document document) throws IOException {
		ValidateArgument.required(document, "document");
		ValidateArgument.required(document.getId(), "document.Id");

		if (builtBatch){ // If it's already been built, don't add document
			throw new IllegalStateException("build() has already been called.");
		}

		byte[] documentBytes = SearchUtil.convertSearchDocumentToJSONString(document).getBytes(CHARSET);

		//if a single document exceeds the single document size limit pretend like we can add it, but don't actually add it
		//we effectively skip adding large documents
		if (documentBytes.length > maxSingleDocumentSizeInBytes) {
			return true;
			//throw new IllegalArgumentException("The document for " + document.getId() + " is " + documentBytes.length + " bytes and exceeds the maximum allowed " + maxSingleDocumentSizeInBytes + " bytes.");
		}

		if(!willDocumentBytesFit(documentBytes.length)){
			return false;
		}

		//if this is not the first document to be added, we must check if it would fit in the batch
		if(!initIfNecessary()) {
			//add delimiter between previous document and the new document to be added.
			builderResources.getCountingOutputStream().write(DELIMITER_BYTES);
		}

		builderResources.getCountingOutputStream().write(documentBytes);

		builderResources.getDocumentIds().add(document.getId());
		return true;
	}

	private boolean willDocumentBytesFit(long documentByteLength){
		if(builderResources == null){
			//not initialized yet so it should fit since maxSingleDocumentSizeInBytes has already been checked
			return true;
		}

		//determine how many bytes need to be written
		//always reserve space for the suffix because the next document being added may not fit into this document batch.
		long bytesToBeAdded =  DELIMITER_BYTES.length + documentByteLength + SUFFIX_BYTES.length;

		// Check there is enough space to write into this batch
		return builderResources.getCountingOutputStream().getByteCount() + bytesToBeAdded <= maxDocumentBatchSizeInBytes;
	}

	@Override
	public void close() throws IOException {
		if (builderResources != null){
			builderResources.close();
		}
	}


	/**
	 * Class used to encapsulate the resources needed by the builder.
	 * The builder needs these fields to be either all null, or all not null.
	 */
	static class CloudSearchBatchBuilderResource implements Closeable{
		private File documentBatchFile;
		private CountingOutputStream countingOutputStream;
		private Set<String> documentIds;

		public CloudSearchBatchBuilderResource(FileProvider fileProvider) throws IOException {
			this.documentBatchFile = fileProvider.createTempFile("CloudSearchDocument", ".json");
			this.countingOutputStream = new CountingOutputStream(fileProvider.createFileOutputStream(documentBatchFile));
			this.documentIds = new HashSet<>();
		}

		@Override
		public void close() throws IOException {
			countingOutputStream.close();
			documentBatchFile.delete();
		}

		public CountingOutputStream getCountingOutputStream() {
			return countingOutputStream;
		}

		public Set<String> getDocumentIds() {
			return documentIds;
		}

		public File getDocumentBatchFile() {
			return documentBatchFile;
		}
	}
}
