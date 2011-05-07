package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.shared.users.UserData;

public interface UserListener {
	
	public void userChanged(UserData newUser);

}
