package org.sagebionetworks.client.fileuploader;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.ListListener;
import org.apache.pivot.collections.Map;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.io.FileList;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.ApplicationContext;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.ComponentKeyListener;
import org.apache.pivot.wtk.DropAction;
import org.apache.pivot.wtk.DropTarget;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.FileBrowserSheet.Mode;
import org.apache.pivot.wtk.Keyboard;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.Manifest;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.apache.pivot.wtk.Span;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.content.TableViewCellRenderer;

public class FileUploaderViewImpl extends Window implements Bindable, FileUploaderView {

	protected static final Logger log = Logger.getLogger(FileUploaderViewImpl.class.getName());
	
	@BXML private TableView fileTableView;
    @BXML private PushButton uploadButton;
    @BXML private PushButton browseButton;
    @BXML private Label actionMessage;
    @BXML private Label entityMessage;
        
    private static Presenter presenter;
    private FileList fileList = null;
    private boolean enabled = true;
    private boolean singleFileMode = true;
    private FileBrowserSheet fileBrowserSheet;

    @Override
    public void setPresenter(Presenter presenter) {
    	this.presenter = presenter;
    }
    
    @Override
    public void initialize(Map<String, Object> namespace, URL location, Resources resources) {    		
    	fileList = new FileList();
        fileTableView.setTableData(fileList);              
        browseButton.setEnabled(true);
        
        setupKeyListeners();
        setupDragAndDrop();
        setupBrowseButton();
        setupUploadButton();      
    }
    
	@Override
	public void alert(String message) {
		Alert.alert(message, this);
	}	
	
	@Override
	public void updateFileStatus() {
		ApplicationContext.queueCallback(new Runnable() {
			@Override
			public void run() {				
				fileTableView.repaint();
			}
		});
	}	

	@Override
	public void setUploadingIntoMessage(String message) {
		if(singleFileMode) actionMessage.setText("update file:");
		else actionMessage.setText("upload to:");
		entityMessage.setText(message);
	}
	
	@Override
	public void showStagedFiles(java.util.List<File> files) {
		final FileList tableData = (FileList)fileTableView.getTableData();				
		for (File file : files) {
	    	tableData.add(file);                            		
		}		
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		applyEnabled();
	}
	
	@Override
	public void setSingleFileMode(boolean singleFileMode) {
		this.singleFileMode = singleFileMode;
		
		if(fileBrowserSheet != null) 
			setBrowserSheetMode();		
	}

	/*
	 * Private Methods
	 */
	private void setupBrowseButton() {
		browseButton.getButtonPressListeners().add(new ButtonPressListener() {			
        	@Override
        	public void buttonPressed(Button arg0) {
        		fileBrowserSheet = new FileBrowserSheet();        		  
                setBrowserSheetMode();
                fileBrowserSheet.open(FileUploaderViewImpl.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            Sequence<File> selectedFiles = fileBrowserSheet.getSelectedFiles();
                            java.util.List<File> files = new ArrayList<File>();
                            for(int i=0; i<selectedFiles.getLength(); i++) 
                            	files.add(selectedFiles.get(i));                            
                            presenter.addFilesForUpload(files);
                        } 
                    }
                });
        	}

        });
	}

	private void setBrowserSheetMode() {
		if(singleFileMode)
        	fileBrowserSheet.setMode(Mode.OPEN);
        else 
        	fileBrowserSheet.setMode(Mode.OPEN_MULTIPLE);
	}

	private void setupDragAndDrop() {
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
                    	// don't allow illegal drops in single file mode
						if (singleFileMode) {
							if((!fileList.isEmpty() && dragContent.getFileList().getList().size() > 0)	// already a file in the list?
									|| (dragContent.getFileList().getList().size() > 1) // dropping too many files?
									|| (dragContent.getFileList().getList().size() == 1 && dragContent.getFileList().getList().get(0).isDirectory()) // dropping a directory?
									) {
	                    		alert("You may only have one file when uploading a new version of a File");
	                    		dragExit(component);
	                    		return null;
	                    	}							
						}
                        presenter.addFilesForUpload(dragContent.getFileList().getList());
                        dropAction = DropAction.COPY;
                    } catch(IOException exception) {
                        System.err.println(exception);
                    }
                }

                dragExit(component);

                return dropAction;
            }

        });
	}
	
	private void setupKeyListeners() {
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
                        
                        // retrieve files to be removed and send to presenter
                        java.util.List<File> filesToRemove = new ArrayList<File>();
                        for(int num=range.start; num<=range.end; num++) {
                            filesToRemove.add(fileList.get(num));
                        }
                        presenter.removeFilesFromUpload(filesToRemove);

                        // remove from view
                        fileList.remove(index, count);                                                	
                    }
                }

                return false;
            }
        });
	}

	private void setupUploadButton() {
		uploadButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {                
            	presenter.uploadFiles();
            }
        });
        
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
	}

	private void applyEnabled() {		
		if(uploadButton != null) {
			uploadButton.setEnabled(enabled);
		}
		if(browseButton != null) {
			browseButton.setEnabled(enabled);
		}
		if(fileTableView != null) {
			fileTableView.setEnabled(enabled);
		}
	}
	
	
	/*
	 * CellRenderer
	 */
	public static class TestCellRenderer extends TableViewCellRenderer {
		Color DEFAULT = new Color(0,140,180);
		Color ERROR = new Color(180,0,0);
		Color PENDING = new Color(93,124,0);
		Color SUCCESS = new Color(27,191,0);
		
		@Override
		public void render(Object row, int rowIndex, int columnIndex,
				TableView tableView, String columnName, boolean selected,
				boolean highlighted, boolean disabled) {
			if (row != null) {
				@SuppressWarnings("unchecked")
				List<File> files = (List<File>) tableView.getTableData();				
				UploadStatus status = presenter.getFileUplaodStatus(files.get(rowIndex));
				String statusDisplay;
				Color color = DEFAULT;
				switch(status) {
				case NOT_UPLOADED:
					statusDisplay = "Not Uploaded";				
					break;
				case WAITING_TO_UPLOAD:
					statusDisplay = "Waiting to Upload";
					color = PENDING;
					break;
				case UPLOADING:
					statusDisplay = "Uploading...";
					color = PENDING;
					break;
				case UPLOADED:
					statusDisplay = "Uploaded";
					color = SUCCESS;
					break;
				case FAILED:
					statusDisplay = "Failed";
					color = ERROR;
					break;
				case ALREADY_EXISTS:
					statusDisplay = "Already Exists";
					color = ERROR;
					break;					
				default:
					statusDisplay = "Unknown";
				}
				setText(statusDisplay);
				getStyles().put("color", color);
			}
		}
		
		
	}

}
