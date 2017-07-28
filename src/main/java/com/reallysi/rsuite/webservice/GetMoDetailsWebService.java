/**
 * 
 */
package com.reallysi.rsuite.webservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.PlainTextResult;

/**
 * Takes a content assembly and generates a periodical map from it and attaches
 * the map to the content assembly.
 */
public class GetMoDetailsWebService extends DefaultRemoteApiHandler {

    /**
	 * 
	 */
	private static final String WEB_SERVICE_TITLE = "CA to Periodical Map";
	private static final long serialVersionUID = 1L;
    private static Log log = LogFactory.getLog(GetMoDetailsWebService.class);

    @Override
    public RemoteApiResult execute(
    		RemoteApiExecutionContext context, 
    		CallArgumentList args) throws RSuiteException {
		log.info("execute(): args=" + args.getValuesMap());

		StringBuilder result = new StringBuilder("<?xml version='1.0'?><moDetails>");
    	try {
    		User user = context.getSession().getUser();
            String nodeId = args.getFirstValue("rsuiteId");
            ManagedObject baseMo = context.getManagedObjectService().getManagedObject(user, nodeId);
            ManagedObject realMo = baseMo;
            if (baseMo.getTargetId() != null) {
            	realMo = context.getManagedObjectService().getManagedObject(user, baseMo.getTargetId());
            	nodeId = realMo.getId();
            }
 
            result.append("<moid>")
            .append(nodeId)
            .append("</moid>");
            
            if (!realMo.isAssemblyNode()) {
            	result.append("<motype>")
            	.append((realMo.isNonXml() ? "nonxml" : "xml"))
            	.append("</motype>");
            	if (realMo.isNonXml()) {
            		result.append("<contenttype>")
            		.append(realMo.getContentType())
            		.append("</contenttype>");
            	} else {
            		org.w3c.dom.Element elem = realMo.getElement();
            		result.append("<tagname>")
            		.append(elem.getNodeName())
            		.append("</tagname>");
            		result.append("<ditaclass>")
            		.append(elem.getAttribute("class"))
            		.append("</ditaclass>");
            	}
        		result.append("<aliases>");
            	
            	Alias[] aliases = realMo.getAliases();
            	for (Alias alias : aliases) {
            		result.append("<alias type='")
            		.append(alias.getType())
            		.append("'")
            		.append(">")
            		.append(alias.getText())
            		.append("</alias>");
            	}
        		result.append("</aliases>");
            }
            
            
    		
    	} catch (Exception e) {
    		log.error("Exception generating periodical map: " + e.getMessage(), e);
    		return new MessageDialogResult
	        (MessageType.ERROR, WEB_SERVICE_TITLE, 
	         e.getMessage());	
    	}
    	
    	result.append("</moDetails>");
    	return new PlainTextResult(result.toString());
    }


}
