package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.List;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.ProcessInstanceInfo;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.ProcessInstanceService;
import com.reallysi.tools.StringUtil;

/**
 * Reassign an article.
 */
public class AstdReassignArticleActionHandler
             extends AstdActionHandlerBase  {

    private static final long serialVersionUID = 1L;

    public static final String VAR_PUB_CODE = "form.pubcode";
    public static final String VAR_TYPE     = "form.type";
    public static final String VAR_VOLUME   = "form.volume";
    public static final String VAR_ISSUE    = "form.issue";
    public static final String VAR_SEQUENCE = "form.sequence";
    public static final String VAR_AUTHOR   = "form.author";

    @Override
    public void execute(
            WorkflowExecutionContext context
    ) throws Exception {
        Log wfLog = context.getWorkflowLog();
        wfLog.info("Attempting to reassign article...");

        try {
            User user = getSystemUser();
            String moid = context.getVariable("rsuite contents");
            wfLog.info("MOID (us): "+moid);
            ManagedObjectService mosvc = context.getManagedObjectService();
            ManagedObject mo = mosvc.getManagedObject(user, moid);
            if (!mo.isAssemblyNode()) {
                reportAndThrowRSuiteException(context,
                    "MO is not a content assembly");
            }

            AstdArticleFilename curName = null;
            try {
                curName = new AstdArticleFilename(mo.getDisplayName());
            } catch (Exception e) {
                reportAndThrowRSuiteException(context,
                    "MO does not have a valid article name: " +
                    e.getLocalizedMessage());
            }

            ContentAssembly us =
                context.getContentAssemblyService().getContentAssemblyById(
                                user, moid);
            if (AstdActionUtils.isArticleContentsLocked(us)) {
            	reportAndThrowRSuiteException(context,
                        "Article "+curName+" has locked content, cannot reassign");
            }

            // Get form values: If a form value not set, we default to
            // value from current name.
            String pubCode = context.getVariable(VAR_PUB_CODE);
            if (StringUtil.isNullOrEmptyOrSpace(pubCode)) {
                pubCode = curName.pubCode;
            }
            String type = context.getVariable(VAR_TYPE);
            if (StringUtil.isNullOrEmptyOrSpace(type)) {
                type = curName.type;
            }
            String volume = context.getVariable(VAR_VOLUME);
            if (StringUtil.isNullOrEmptyOrSpace(volume)) {
                volume = curName.volume;
            }
            String issue = context.getVariable(VAR_ISSUE);
            if (StringUtil.isNullOrEmptyOrSpace(issue)) {
                issue = curName.issue;
            }
            String sequence = context.getVariable(VAR_SEQUENCE);
            if (StringUtil.isNullOrEmptyOrSpace(sequence)) {
                sequence = curName.sequence;
            }
            String author = context.getVariable(VAR_AUTHOR);
            if (StringUtil.isNullOrEmptyOrSpace(author)) {
                author = curName.author;
            }
            
            boolean sameIssue = curName.pubCode.equals(pubCode) &&
                                curName.issue.equals(issue) &&
                                curName.volume.equals(volume);
            String newName = pubCode + type + volume + issue + sequence + author;
            wfLog.info("Current CA name: "+curName);
            wfLog.info("New CA name: " + newName);

            if (!sameIssue) {
            	boolean isUnassigned = "99".equals(volume);
            	String folderRoot = "/" + AstdWorkflowConstants.FOLDER_MAGAZINE
            	+ "/" + ArticlePubCode.getPubDesc(pubCode);
            	String folder = null;
            	if (isUnassigned) {
            		folder = folderRoot;
            	} else {
            		folder = folderRoot + "/" + "Volume " + volume;
            	}
            	wfLog.info("New folder location: " + folder);
            	try {
            		folder = AstdActionUtils.createFolder(
            				user, folder, context);
            	} catch (Exception e) {
            		reportAndThrowRSuiteException(context,
            				"Unable to create folder: " + e.getLocalizedMessage());
            	}

            	String caContainer = null;
            	if (isUnassigned) {
            		caContainer = "Unassigned";
            	} else {
            		caContainer = "Issue " + issue;
            	}
            	wfLog.info("New container CA: " + caContainer);
            	String caId = null;
            	try {
            		caId = AstdActionUtils.createCA(
            				getRepositoryService(), user, caContainer, folder, context);
            	} catch (Exception e) {
            		reportAndThrowRSuiteException(context,
            				"Unable to create CA: " + e.getLocalizedMessage());
            	}
            	wfLog.info("New container CA ID: "+caId);

            	// Attempt to move article CA to new location
            	ContentAssembly newParent =
            		context.getContentAssemblyService().getContentAssemblyById(
            				user, caId);
            	try {
            		wfLog.info("Moving CA");
            		context.getContentAssemblyService().moveTo(
            				user, newParent.getId(), us.getId());
            	} catch (Exception e) {
            		reportAndThrowRSuiteException(context,
            				"Unable to move CA: " + e.getLocalizedMessage());
            	}
            }

            // Attempt to rename article CA
            try {
            	wfLog.info("Renaming CA "+moid+" to "+newName);
                context.getContentAssemblyService().renameCANode(
                                user, moid, newName, null);
            } catch (Exception e) {
                reportAndThrowRSuiteException(context,
                    "Unable to rename CA: " + e.getLocalizedMessage());
            }

            // Re-fetch CA instance since previous operations change CA and we
            // need to make sure we are in-sync with cache.
            us = context.getContentAssemblyService().getContentAssemblyById(user, moid);

            // Rename non-XML child nodes that follow article naming convention
    		List<? extends ContentAssemblyItem> items =
    			us.getChildrenObjects();
    		wfLog.info("CA items = "+items);
    		if (items != null && items.size() > 0) {
    			for (ContentAssemblyItem item : items) {
    				wfLog.info("Check if need to rename "+item.getDisplayName());
    				if (item instanceof ManagedObjectReference) {
    					ManagedObjectReference mor = (ManagedObjectReference)item;
    					@SuppressWarnings("deprecation")
						String sid = mor.getSourceId();
    					if (!AstdActionUtils.isNonXml(mor)) {
    						// For xml objects, we need to set alias since
    						// transform/export operations may depend on it.
    						wfLog.info("Setting new alias for MO "+sid+": "+
    								newName+".xml");
    						mosvc.setAlias(user, sid, newName+".xml");
    						continue;
    					}
    					wfLog.info("Attempting to rename "+item.getDisplayName()+
    							" to "+newName);
    					String newNonXmlName =
    						AstdActionUtils.renameArticleNonXmlMo(
    							user, mosvc, wfLog, sid, newName);
    					
    					// XXX: Bug in Rsuite 3.3.1 (and earlier) where alias
    					// is not updated in previous utility method, therefore,
    					// we explicitly set it here (again)
    					if (newNonXmlName != null) {
    						wfLog.info("Setting alias for "+sid+" to \""+
    								newNonXmlName+"\"");
    						mosvc.setAlias(user, sid, newNonXmlName);
    					}
    				}
    			}
    		}
            
    		// Update process variables
    		String pid = mo.getLayeredMetadataValue(
    				AstdWorkflowConstants.ASTD_ARTICLE_PID_LMD_FIELD);
    		if (!StringUtil.isNullOrEmptyOrSpace(pid)) {
    			wfLog.info("Attempting to update variables for process "+pid);
    			ProcessInstanceService psvc =
    				context.getProcessInstanceService(); 
    			ProcessInstanceInfo pi = psvc.getProcessInstance(user,pid); 
    			if (pi == null) {
    				wfLog.info("No process with id "+pid+" found, clearing LMD field");
    				AstdActionUtils.clearArticlePidLmdField(context, mo, user);
    			} else {
    				// FIXME: When API is made available to change variables, update
    				//        block.  The following code has no effect.
    				/*
    				List pVars = pi.getVariables();
    				for (Object obj : pVars) {
    					VariableInfo var = (VariableInfo)obj;
    					String varName = var.getName();
    					wfLog.info("\t(old) "+varName+"="+var.getValue());
    					if (varName.equals(AstdWorkflowConstants.ASTD_VAR_PUB_CODE)) {
    						var.setValue(pubCode);
    					} else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_FULL_FILENAME)) {
                            var.setValue(newName+".docx");
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_SOURCE_FILENAME)) {
                            var.setValue(newName);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_ARTICLE_TYPE)) {
                            var.setValue(type);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_VOLUME_NUMBER)) {
                            var.setValue(volume);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_ISSUE)) {
                            var.setValue(issue);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_MONTH)) {
                            var.setValue(issue);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_SEQUENCE)) {
                            var.setValue(sequence);
                        } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_FILENAME_AUTHOR)) {
                            var.setValue(author);
                        }
    					wfLog.info("\t(new) "+varName+"="+var.getValue());
    				}
    				*/
    			}
    		}

        } catch (Exception e) {
          context.setVariable("REASSIGN_MSGS", e.getLocalizedMessage());
          throw e;
        }
    }
    
}
