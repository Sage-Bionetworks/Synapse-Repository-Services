package org.sagebionetworks.search;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.sagebionetworks.util.FileProvider;

public class CloudSearchDocumentBatchBuilder implements Closeable {

	private static final int MEBIBYTE = 1048576; // CloudSearch's Limits say MB but they really mean MiB
	private static final int DEFAULT_MAX_SINGLE_DOCUMENT_SIZE = MEBIBYTE; //1MiB
	private static final int DEFAULT_MAX_DOCUMENT_BATCH_SIZE = 5 * MEBIBYTE; //5MiB

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	static final byte[] PREFIX_BYTES = "[".getBytes(CHARSET);
	static final byte[] SUFFIX_BYTES = "]".getBytes(CHARSET);
	static final byte[] DELIMITER_BYTES = ",".getBytes(CHARSET);

	FileProvider fileProvider;

	File documentBatchFile;

	Set<String> documentIds;


	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider){
		this(fileProvider, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE);
	}

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider, long maxSingleDocumentSizeInBytes, long maxDocumentBatchSizeInBytes){
		if (maxSingleDocumentSizeInBytes + PREFIX_BYTES.length + SUFFIX_BYTES.length > maxDocumentBatchSizeInBytes){
			throw new IllegalArgumentException("maxSingleDocumentSizeInBytes + " + (PREFIX_BYTES.length + SUFFIX_BYTES.length) + " must be less or equal to than maxDocumentBatchSizeInBytes");
		}
	}

	public CloudSearchDocumentBatch build(){

	}

	public boolean tryAddDocumentBytes(){

	}

	@Override
	public void close() throws IOException {
		this.documentBatchFile.delete();
	}
}
