package org.sagebionetworks.web.test.helper.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExampleServiceStub implements ExampleService{
	
	private List<SampleDTO> samples = new ArrayList<SampleDTO>();
	private int sequence = 0;

	@Override
	public List<SampleDTO> noArgs() {
		return samples;
	}

	@Override
	public SampleDTO getSampleOverload(String name) {
		if(name == null) return null;
		// Find the sample by name
		for(SampleDTO sample: samples){
			if(sample.getName().equals(name)){
				return sample;
			}
		}
		return null;
	}

	@Override
	public SampleDTO getSampleOverload(int id) {
		// Find the sample by id
		return findSampleById(id);
	}

	/**
	 * Find a sample by id.
	 * @param id
	 * @return
	 */
	private SampleDTO findSampleById(int id) {
		for(SampleDTO sample: samples){
			if(sample.getId() == id){
				return sample;
			}
		}
		return null;
	}

	@Override
	public void voidReturn(int id) {
		// Does nothing.
	}

	@Override
	public boolean allPrimitives(long count, int id) {
		return count > -1 && id > -1;
	}

	@Override
	public List<SampleDTO> nullReturn() {
		// always return null;
		return null;
	}

	@Override
	public List<SampleDTO> withArgs(List<Integer> idList) {
		// Find all of the samples by id.
		if(idList == null) return null;
		List<SampleDTO> results = new LinkedList<SampleDTO>();
		// Find each sample
		for(Integer idToFind: idList){
			SampleDTO found = findSampleById(idToFind);
			if(found != null){
				results.add(found);
			}
		}
		return results;
	}

	@Override
	public SampleDTO getSampleOverload(Exception e) {
		return new SampleDTO(e.getMessage(), null, -1);
	}

	@Override
	public SampleDTO getSampleOverload(IOException e) {
		return new SampleDTO(e.getMessage(), null, -1);
	}

	@Override
	public int createSample(String name, String description) {
		int id = sequence;
		sequence++;
		samples.add(new SampleDTO(name, description, id));
		return id;
	}

	@Override
	public boolean deleteSample(int id) {
		// TODO Auto-generated method stub
		return samples.remove(new SampleDTO(null, null, id));
	}

	@Override
	public boolean throwsException(String message) throws IOException {
		throw new IOException(message);
	}

}
