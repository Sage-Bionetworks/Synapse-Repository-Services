package org.sagebionetworks.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

public class CloudSearchDocumentBatchImpl implements CloudSearchDocumentBatch{

	File documentBatchFile;
	long byteSize;
	Set<String> documentIds;


	public CloudSearchDocumentBatchImpl(File documentBatchFile, Set<String> documentIds, long byteSize){
		this.documentBatchFile = documentBatchFile;
		this.documentIds = Collections.unmodifiableSet(documentIds);
		this.byteSize = byteSize;
	}

	@Override
	public long size() {
		return byteSize;
	}

	@Override
	public InputStream getNewInputStream() {
		try{
			return new FileInputStream(documentBatchFile);
		} catch (FileNotFoundException e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getDocumentIds() {
		return documentIds;
	}

	@Override
	public void close(){
		documentBatchFile.delete();
	}
}
