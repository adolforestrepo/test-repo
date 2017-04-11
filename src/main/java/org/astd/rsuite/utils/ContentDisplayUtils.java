package org.astd.rsuite.utils;


import static org.astd.rsuite.constants.AstdConstants.UI_PARAM_RSUITE_REFRESH_MANAGED_OBJECTS;

import static org.astd.rsuite.constants.AstdConstants.UI_PROPERTY_OBJECTS;

import static org.astd.rsuite.constants.AstdConstants.UI_PROPERTY_CHILDREN;


import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;


public class ContentDisplayUtils {

	public static MessageDialogResult getResultWithLabelRefreshing (
			MessageType messageType, String dialogTitle, String message, String dialogWith, String ...moIds) {
		MessageDialogResult result = new MessageDialogResult(messageType, dialogTitle, message, dialogWith);
		UserInterfaceAction uia = new UserInterfaceAction(UI_PARAM_RSUITE_REFRESH_MANAGED_OBJECTS.getName());
		if (moIds != null){
			for (String moId : moIds) {
				uia.addProperty(UI_PROPERTY_OBJECTS.getName(), moId);
			}
			
		}
		
		uia.addProperty(UI_PROPERTY_CHILDREN.getName(), true);		
		result.addAction(uia);			
		return result;		
	}
}
