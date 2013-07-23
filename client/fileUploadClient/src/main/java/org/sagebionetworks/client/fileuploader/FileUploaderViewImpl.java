package org.sagebionetworks.client.fileuploader;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @BXML private Label parentMessage;
        
    private static Presenter presenter;
    private FileList fileList = null;    
    
    @Override
    public void setPresenter(Presenter presenter) {
    	this.presenter = presenter;
    }
    
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

        setupKeyListeners();
        setupDragAndDrop();
        setupBrowseButton();

        uploadButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {                
                uploadFiles();
            }
        });
                
    }

	private void setupBrowseButton() {
		browseButton.getButtonPressListeners().add(new ButtonPressListener() {			
        	@Override
        	public void buttonPressed(Button arg0) {
        		final FileBrowserSheet fileBrowserSheet = new FileBrowserSheet();
        		  
                fileBrowserSheet.setMode(Mode.OPEN_MULTIPLE);
                fileBrowserSheet.open(FileUploaderViewImpl.this, new SheetCloseListener() {
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
	}
	
	@Override
	public void alert(String message) {
		Alert.alert(message, this);
	}

	@Override
	public void updateFileStatus() {
		fileTableView.repaint();
	}	

	@Override
	public void setUploadingIntoMessage(String message) {
		parentMessage.setText(message);
	}
	
	/*
	 * Private Methods
	 */
	private void uploadFiles() {
		FileList tableData = (FileList)fileTableView.getTableData();		
		presenter.uploadFiles(tableData.getList());		
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
                        final FileList tableData = (FileList)fileTableView.getTableData();
                        FileList fileListLocal = dragContent.getFileList();
                        final Pattern hiddenFile = Pattern.compile("^\\..*");
                        SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            	// ignore hidden files
                            	if(!hiddenFile.matcher(file.getFileName().toString()).matches()) {
                            		tableData.add(file.toFile());                            		
                            	}
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                return FileVisitResult.CONTINUE;
                            }
                        }; 
                        for (File file : fileListLocal) {                        
                            Files.walkFileTree(file.toPath(), fileVisitor);
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
                        fileList.remove(index, count);
                    }
                }

                return false;
            }
        });
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
				default:
					statusDisplay = "Unknown";
				}
				setText(statusDisplay);
				getStyles().put("color", color);
			}
		}
		
		
	}

}
