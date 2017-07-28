package com.reallysi.rsuite.webservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.content.ContentDisplayObject;
import com.reallysi.rsuite.api.content.ContentObjectPath;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.NotificationResult;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.WorkflowInstanceService;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;

public class StartDitaOTWorkflowWebService extends RemoteApiHandlerBase {	

	private static Log log = LogFactory.getLog(StartDitaOTWorkflowWebService.class);

	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.remoteapi.RemoteApiHandler#execute(com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext, com.reallysi.rsuite.api.remoteapi.CallArgumentList)
	 */
	public RemoteApiResult execute(RemoteApiExecutionContext context,
				                   CallArgumentList args) throws RSuiteException {
		log.info("execute(): args=" + args.getValuesMap());

		String rsuiteId = args.getFirstValue( "rsuiteId");
		String processDefinitionName = args.getFirstValue( "processDefinitionName");
		String outputPath = args.getFirstValue(DitaOtOptions.OUTPUT_PATH_PARAM);
		String zipOutput = args.getFirstValue(DitaOtOptions.ZIP_OUTPUT_PARAM);

		User user = getUser(context);

		ManagedObject parentMo = getManagedObjectParent( context, user, args.getContentObjectPaths( user));
		
		try {
			Map<String,Object> instArgs = new HashMap<String,Object>();
			instArgs.put( "rsuite contents", rsuiteId);
			instArgs.put( "parentId", parentMo.getId());
			instArgs.put( "parentMoId", parentMo.getId());
			instArgs.put( "rsuiteUserId", user.getUserId());
			instArgs.put(DitaOtOptions.OUTPUT_PATH_PARAM, outputPath);
			instArgs.put(DitaOtOptions.ZIP_OUTPUT_PARAM, zipOutput);
			
			for ( CallArgument arg : args.getAll()) {
				instArgs.put( arg.getName(), args.getFirstString( arg.getName()));
			}
			
			WorkflowInstanceService piSvc = context.getWorkflowInstanceService();
			WorkflowInstance wfInstance = piSvc.startWorkflow(processDefinitionName, instArgs);
			log.info( "Started workflow \"" + processDefinitionName + "\": " + wfInstance.getId());
      
			NotificationResult result = new NotificationResult( "Start Workflow", processDefinitionName +" started.");
			return result;

		} catch (Exception e) {
			log.error( "StartWorkflow: error: " + e.getMessage(), e);
			MessageDialogResult result = new MessageDialogResult( MessageType.ERROR, "StartWorkflow", e.getMessage());
		    return result;
		}
	}

    public static ManagedObject getManagedObjectParent( ExecutionContext context, User user, 
    		                                            List<ContentObjectPath> contentObjectPaths) throws RSuiteException {                        
    	ManagedObjectService moService = context.getManagedObjectService();
    	ContentObjectPath firstPath = contentObjectPaths.get(0);                                        
        ManagedObject parentMo = null;                                                                  
        if (firstPath.getSize() > 1) {                                                                  
            ContentDisplayObject parentObject = firstPath.getPathObjects()                              
                    .get(firstPath.getSize() - 2);                                                      
            parentMo = parentObject.getManagedObject();                                                 
            String targetId = parentMo.getTargetId();                                                   
            if (targetId != null) {                                                                     
                parentMo = moService.getManagedObject(user, targetId);                                  
            }                                                                                           
        }                                                                                               
        if (parentMo == null) {                                                                         
            // No parent                                                                                
            throw new RSuiteException(RSuiteException.ERROR_OBJECT_NOT_FOUND,                           
                    "Parent CA not found");                                                             
        }                                                                                               
        return parentMo;                                                                                
    }  	
}
