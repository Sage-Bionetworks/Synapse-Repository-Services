package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.ListListener;
import org.apache.pivot.collections.Map;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.io.FileList;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.ComponentKeyListener;
import org.apache.pivot.wtk.DropAction;
import org.apache.pivot.wtk.DropTarget;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.FileBrowserSheet.Mode;
import org.apache.pivot.wtk.Keyboard;
import org.apache.pivot.wtk.Manifest;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.apache.pivot.wtk.Span;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.Window;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileUploader extends Window implements Bindable {
    @BXML private TableView fileTableView;
    @BXML private PushButton uploadButton;
    @BXML private PushButton browseButton;

    private FileList fileList = null;
    private Synapse synapseClient;
    
    @Override
    public void initialize(Map<String, Object> namespace, URL location, Resources resources) {    		
    	fileList = new FileList();
        fileTableView.setTableData(fileList);

        browseButton.setEnabled(true);
        
        fileList.getListListeners().add(new ListListener.Adapter<File>() {
            @Override
            public void itemInserted(List<File> list, int index) {
                uploadButton.setEnabled(list.getLength() > 0);                
            }

            @Override
            public void itemsRemoved(List<File> list, int index, Sequence<File> files) {
                uploadButton.setEnabled(list.getLength() > 0);

                if (fileTableView.isFocused()
                    && index < list.getLength()) {
                    fileTableView.setSelectedIndex(index);
                }
            }
        });

        fileTableView.getComponentKeyListeners().add(new ComponentKeyListener.Adapter() {
            @Override
            public boolean keyPressed(Component component, int keyCode, Keyboard.KeyLocation keyLocation) {
                if (keyCode == Keyboard.KeyCode.DELETE
                    || keyCode == Keyboard.KeyCode.BACKSPACE) {
                    Sequence<Span> selectedRanges = fileTableView.getSelectedRanges();

                    for (int i = selectedRanges.getLength() - 1; i >= 0; i--) {
                        Span range = selectedRanges.get(i);
                        int index = range.start;
                        int count = range.end - index + 1;
                        fileList.remove(index, count);
                    }
                }

                return false;
            }
        });

        fileTableView.setDropTarget(new DropTarget() {
            @Override
            public DropAction dragEnter(Component component, Manifest dragContent,
                int supportedDropActions, DropAction userDropAction) {
                DropAction dropAction = null;

                if (dragContent.containsFileList()
                    && DropAction.COPY.isSelected(supportedDropActions)) {
                    dropAction = DropAction.COPY;
                }

                return dropAction;
            }

            @Override
            public void dragExit(Component component) {
                // empty block
            }

            @Override
            public DropAction dragMove(Component component, Manifest dragContent,
                int supportedDropActions, int x, int y, DropAction userDropAction) {
                return (dragContent.containsFileList() ? DropAction.COPY : null);
            }

            @Override
            public DropAction userDropActionChange(Component component, Manifest dragContent,
                int supportedDropActions, int x, int y, DropAction userDropAction) {
                return (dragContent.containsFileList() ? DropAction.COPY : null);
            }

            @Override
            public DropAction drop(Component component, Manifest dragContent,
                int supportedDropActions, int x, int y, DropAction userDropAction) {
                DropAction dropAction = null;

                if (dragContent.containsFileList()) {
                    try {
                        FileList tableData = (FileList)fileTableView.getTableData();
                        FileList fileListLocal = dragContent.getFileList();
                        for (File file : fileListLocal) {
                            if (file.isDirectory()) {
                                // TODO Expand recursively
                            }

                            tableData.add(file);
                        }

                        dropAction = DropAction.COPY;
                    } catch(IOException exception) {
                        System.err.println(exception);
                    }
                }

                dragExit(component);

                return dropAction;
            }
        });

        browseButton.getButtonPressListeners().add(new ButtonPressListener() {			
        	@Override
        	public void buttonPressed(Button arg0) {
        		final FileBrowserSheet fileBrowserSheet = new FileBrowserSheet();
        		  
                fileBrowserSheet.setMode(Mode.OPEN_MULTIPLE);
                fileBrowserSheet.open(FileUploader.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            Sequence<File> selectedFiles = fileBrowserSheet.getSelectedFiles();
                            FileList tableData = (FileList)fileTableView.getTableData(); 
                            for(int i=0; i<selectedFiles.getLength(); i++) {                            	
                            	tableData.add(selectedFiles.get(i));
                            }
                        } 
                    }
                });
        	}
        });

        uploadButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {                
                uploadFiles();
            }
        });
        
    }

	public void setSynapseClient(Synapse synapseClient) {
		this.synapseClient = synapseClient;
		try {
			synapseClient.getUserSessionData();
		} catch (SynapseException e) {
			if(e instanceof SynapseUnauthorizedException) {
				Alert.alert("Your Synapse session has expired. Please reload.", this);				
			}
		}
		
	}
	
	/*
	 * Private Methods
	 */
	private void uploadFiles() {
		FileList tableData = (FileList)fileTableView.getTableData();
		String x = "";
		for(File file : tableData.getList()) {
		//	S3FileHandle fileHandle = synapseClient.createFileHandle(file, Files.probeContentType());
		}
		
		
	}
	
	
	

}
