package org.sagebionetworks.web.test.helper.examples;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This is an example Presenter class that is used to drive the asynchronous service and capture the results.
 * 
 * @author jmhill
 *
 */
public class ExamplePresenter {
	
	private ExampleServiceAsync asyncService = null;
	// No args
	private List<SampleDTO> noArgSuccess = null;
	private Throwable noArgFailure = null;
	// with args
	private List<SampleDTO> withArgSuccess = null;
	private Throwable withArgFailure = null;
	// null return
	private List<SampleDTO> nullReturnSuccess = null;
	private Throwable nullReturnFailure = null;
	// void return
	private boolean voidReturnSuccess = false;
	private Throwable voidReturnFailure = null;
	// createSampel
	private Integer createSucces = null;
	private Throwable createFailure = null;
	
	public ExamplePresenter(ExampleServiceAsync asyncService){
		if(asyncService == null) throw new IllegalArgumentException("ExampleServiceAsync cannot be null");
		this.asyncService = asyncService;
	}
	
	public void doNoArgs(){
		// Make the call
		asyncService.noArgs(new AsyncCallback<List<SampleDTO>>() {
			
			@Override
			public void onSuccess(List<SampleDTO> result) {
				setNoArgSuccess(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setNoArgFailure(caught);
			}
		});
		// Note, if the asycnh service calls either onSuccess() or onFailure()
		// before returning than the data will be lost.
		setNoArgFailure(null);
		setNoArgSuccess(null);
	}
	
	public void doWithArgs(final List<Integer> idList){
		asyncService.withArgs(idList,new AsyncCallback<List<SampleDTO>>() {
			
			@Override
			public void onSuccess(List<SampleDTO> result) {
				setWithArgSuccess(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setWithArgFailure(caught);
			}
		});
		// Note, if the asycnh service calls either onSuccess() or onFailure()
		// before returning than the data will be lost.
		setWithArgFailure(null);
		setWithArgSuccess(null);
	}
	
	public void doNullReturn(){
		asyncService.nullReturn(new AsyncCallback<List<SampleDTO>>() {
			
			@Override
			public void onSuccess(List<SampleDTO> result) {
				setNullReturnSuccess(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setNullReturnFailure(caught);
			}
		});
		// Note, if the asycnh service calls either onSuccess() or onFailure()
		// before returning than the data will be lost.
		setNullReturnFailure(null);
		// Since onSuccess will set this to null, start with a non-null list
		setNullReturnSuccess(new LinkedList<SampleDTO>());
	}
	
	public void doVoidReturn(int id) {
		asyncService.voidReturn(id, new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				setVoidReturnSuccess(true);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setVoidReturnFailure(caught);
				
			}
		});
		// Note, if the asycnh service calls either onSuccess() or onFailure()
		// before returning than the data will be lost.
		setVoidReturnSuccess(false);
		setVoidReturnFailure(null);
	}
	
	public void doCreateSample(String name, String description){
		asyncService.createSample(name, description, new AsyncCallback<Integer>() {
			
			@Override
			public void onSuccess(Integer result) {
				setCreateSucces(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setCreateFailure(caught);
			}
		});
		
		// Note, if the asycnh service calls either onSuccess() or onFailure()
		// before returning than the data will be lost.
		setCreateSucces(null);
		setCreateFailure(null);
	}
	
	

	public Integer getCreateSucces() {
		return createSucces;
	}

	public void setCreateSucces(Integer createSucces) {
		this.createSucces = createSucces;
	}

	public Throwable getCreateFailure() {
		return createFailure;
	}

	public void setCreateFailure(Throwable createFailure) {
		this.createFailure = createFailure;
	}

	public List<SampleDTO> getWithArgSuccess() {
		return withArgSuccess;
	}

	public void setWithArgSuccess(List<SampleDTO> withArgSuccess) {
		this.withArgSuccess = withArgSuccess;
	}

	public Throwable getWithArgFailure() {
		return withArgFailure;
	}

	public void setWithArgFailure(Throwable withArgFailure) {
		this.withArgFailure = withArgFailure;
	}

	public List<SampleDTO> getNoArgSuccess() {
		return noArgSuccess;
	}

	public void setNoArgSuccess(List<SampleDTO> noArgSuccess) {
		this.noArgSuccess = noArgSuccess;
	}

	public Throwable getNoArgFailure() {
		return noArgFailure;
	}

	public void setNoArgFailure(Throwable noArgFailure) {
		this.noArgFailure = noArgFailure;
	}

	public List<SampleDTO> getNullReturnSuccess() {
		return nullReturnSuccess;
	}

	public void setNullReturnSuccess(List<SampleDTO> nullReturnSuccess) {
		this.nullReturnSuccess = nullReturnSuccess;
	}

	public Throwable getNullReturnFailure() {
		return nullReturnFailure;
	}

	public void setNullReturnFailure(Throwable nullReturnFailure) {
		this.nullReturnFailure = nullReturnFailure;
	}

	public boolean isVoidReturnSuccess() {
		return voidReturnSuccess;
	}

	public void setVoidReturnSuccess(boolean voidReturnSuccess) {
		this.voidReturnSuccess = voidReturnSuccess;
	}

	public Throwable getVoidReturnFailure() {
		return voidReturnFailure;
	}

	public void setVoidReturnFailure(Throwable voidReturnFailure) {
		this.voidReturnFailure = voidReturnFailure;
	}

}
