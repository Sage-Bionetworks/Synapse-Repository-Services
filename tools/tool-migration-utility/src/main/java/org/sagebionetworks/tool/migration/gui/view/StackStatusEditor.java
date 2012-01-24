package org.sagebionetworks.tool.migration.gui.view;

import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;

/**
 * Editor for changing the status of a stack.
 * @author John
 *
 */
public class StackStatusEditor extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JLabel enumLabel = new JLabel("Status:");
	private JComboBox comboBox = new JComboBox();
	private JLabel currentMessageLabel = new JLabel("Current Message:");
	private JTextField currentMessage = new JTextField("Currently we are...");
	private JLabel pendingMessageLabel = new JLabel("Pending Message:");
	private JTextField pendingMessage = new JTextField("Pending...");
	
	public StackStatusEditor(){
		initComponents();
	}
	
	private void initComponents() {
		// Set the status enum values
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		for(StatusEnum enumValue: StatusEnum.values()){
			model.addElement(enumValue.name());
		}
		comboBox.setModel(model);
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		// Use default gaps
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		ParallelGroup h1 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
		h1.addComponent(enumLabel);
		h1.addComponent(currentMessageLabel);
		h1.addComponent(pendingMessageLabel);
		ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h2.addComponent(comboBox);
		h2.addComponent(currentMessage);
		h2.addComponent(pendingMessage);
		SequentialGroup hSequence = layout.createSequentialGroup();
		hSequence.addGroup(h1);
		hSequence.addGroup(h2);
		
		ParallelGroup hOuter = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		hOuter.addGroup(hSequence);
		
		// Vertical
		ParallelGroup v1 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v1.addComponent(enumLabel);
		v1.addComponent(comboBox);
		ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v2.addComponent(currentMessageLabel);
		v2.addComponent(currentMessage);
		ParallelGroup v3 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v3.addComponent(pendingMessageLabel);
		v3.addComponent(pendingMessage);
	
		SequentialGroup vSequence = layout.createSequentialGroup();
		vSequence.addGroup(v1);
		vSequence.addGroup(v2);
		vSequence.addGroup(v3);
		
		layout.setHorizontalGroup(hOuter);
		layout.setVerticalGroup(vSequence);
		
		setPreferredSize(new Dimension(600, 100));
	}
	
	/**
	 * Preview this component.
	 * @throws InvocationTargetException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
		// Show the dialog
		final StackStatusEditor editor = new StackStatusEditor();
		final StackStatus current = new StackStatus();
		current.setStatus(StatusEnum.READ_WRITE);
		current.setCurrentMessage("No currentMessage");
		current.setPendingMaintenanceMessage("No pending");
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				StackStatus result = editor.showStatusDialog(null, current);
				System.out.println(result);
			}
		});
		
	}
	
	/**
	 * Show the blocking status dialog.
	 * @param parent
	 * @param current
	 * @return
	 */
	public StackStatus showStatusDialog(Component parent, StackStatus current){
		this.comboBox.setSelectedItem(current.getStatus().name());
		this.currentMessage.setText(current.getCurrentMessage());
		this.pendingMessage.setText(current.getPendingMaintenanceMessage());
		Object[] options = {"Apply",
        "Cancel"};
		int selected = JOptionPane.showOptionDialog(parent, this, "Set Stack Status", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
		// Zero indicates that the user selected apply
		if(selected == 0){
			// Pull the values from the dialog.
			StackStatus newStatus = new StackStatus();
			newStatus.setStatus(StatusEnum.valueOf((String)comboBox.getSelectedItem()));
			newStatus.setCurrentMessage(currentMessage.getText());
			newStatus.setPendingMaintenanceMessage(pendingMessage.getText());
			return newStatus;
		}else{
			return null;
		}
	}


}
