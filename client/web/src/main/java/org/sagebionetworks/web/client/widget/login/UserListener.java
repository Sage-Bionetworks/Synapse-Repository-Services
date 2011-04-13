package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.client.security.user.UserData;

public interface UserListener {
	
	public void userChanged(UserData newUser);

}
