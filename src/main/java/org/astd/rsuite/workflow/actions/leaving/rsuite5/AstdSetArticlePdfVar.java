//package com.astd.rsuite.actions;
package org.astd.rsuite.workflow.actions.leaving.rsuite5;
import java.util.List;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.User;
//import com.reallysi.rsuite.api.workflow.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
//import com.reallysi.tools.StringUtil;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.ArticlePubCode;

/**
 * Set named variable with the article PDF MO ID.
 * <p>The article CA is either specified by the current managed object
 * of the workflow context or thru the <tt>articleCaMoId</tt>
 * parameter.  The workflow variable to set is designated by
 * the <tt>targetVariableName</tt> parameter.
 * </p>
 * <p><b>ASSUMPTION</b>: The PDF for the article must have the
 * same basename as the article display name.  If no PDF object
 * is found for the article, or the PDF does not have the same
 * basename as the article (CA), then an exception is thrown.
 * </p>
 */
public class AstdSetArticlePdfVar extends BaseWorkflowAction implements TempWorkflowConstants {
    private static final long serialVersionUID = 1L;
    
    protected Expression targetVariableName;
    protected Expression EXCEPTION_OCCUR;
    protected Expression articleCaMoId;
    
    protected Expression articlePdfMoId;
    /**
     * (Optional) MO ID of article CA.
     */
    public static final String ARTICLE_MO_ID_PARAM = "articleCaMoId";

    /**
     * Name of the variable to hold the PDF MO ID.
     */
    public static final String TARGET_VAR_PARAM = "targetVariableName";
    
    
    /**
     * MO ID of PDF version of article.
     */
    public static final String ASTD_VAR_ARTICLE_PDF = "articlePdfMoId";


    @Override
    
    public void execute(WorkflowContext context) throws Exception {
        Log wfLog = context.getWorkflowLog();
        wfLog.info("Attempting to pdf...");
        
        
        
        
        User user =context.getAuthorizationService().getSystemUser();
        
    /*    String varName = resolveVariables(
                getParameter(TARGET_VAR_PARAM));
        if (StringUtils.isEmpty(varName)) {
            reportAndThrowRSuiteException(
                    "Target variable name not specified");
        }*/

        
        
        
        
        String varName = resolveExpression(targetVariableName);
        if (StringUtils.isBlank(varName)) {
          reportAndThrowRSuiteException("Target variable name not specified");
        }
        
        
        
        
        
        String caId = resolveVariables(
                getParameter(ARTICLE_MO_ID_PARAM));
        if (StringUtils.isEmpty(caId)) {
            MoListWorkflowObject moList = context.getMoListWorkflowObject();
            
             if (moList == null || moList.isEmpty()) {
                reportAndThrowRSuiteException(
                    "Article content assembly not specified");
            }
            caId = moList.getMoList().get(0).getMoid();
        }
        ManagedObject caMo = context.getManagedObjectService().
            getManagedObject(user, caId);
        String type = caMo.getContentType();
    	String articleName = caMo.getDisplayName();
    	wfLog.info("MO name = "+articleName);

    	boolean foundPdf = false;
        if (type != null && "PDF".equals(type.toUpperCase())) {
        	// PDF is context MO
        	wfLog.info("PDF is context MO: "+ articleName);
        	context.setVariable(varName, caId);
        	foundPdf = true;
        
        } else {
        	ContentAssembly us =
        		context.getContentAssemblyService().getContentAssemblyById(
        				user, caId);
        	List<? extends ContentAssemblyItem> items = us.getChildrenObjects();
        	wfLog.info("CA items = "+items);
        	if (items != null && items.size() > 0) {
        		for (ContentAssemblyItem item : items) {
        			String itemName = item.getDisplayName();
        			wfLog.info("Checking "+itemName+"...");
        			if (item instanceof ManagedObjectReference) {
        				ManagedObjectReference mor = (ManagedObjectReference)item;
        				@SuppressWarnings("deprecation")
						String sid = mor.getSourceId();
        				wfLog.info(itemName+" is a MO reference: "+sid);

        				if (AstdActionUtils.isNonXml(mor)) {
        					wfLog.info(itemName+" is non-Xml");
        					type = mor.getContentType();
        					wfLog.info(itemName+" type is "+type);
        					if (type != null && "PDF".equals(type.toUpperCase())) {
        						int dot = itemName.lastIndexOf('.');
        						if (dot < 0) continue;
        						if (articleName.equals(
        								itemName.substring(0, dot))) {
        							wfLog.info("FOUND: "+sid);
        							context.setVariable(varName, sid);
        							foundPdf = true;
        							break;
        						}
        					}
        				}

        			} else {
        				wfLog.info(item.getDisplayName()+" is NOT a MOR");
        			}
        		}
        	}
        }
        if (!foundPdf) {
            reportAndThrowRSuiteException(
                    "No article PDF for "+articleName);
        }
    }

    public void setTargetVariableName(String s) {
        setParameter(TARGET_VAR_PARAM, s);
    }

    public void setArticleCaMoId(String s) {
        setParameter(ARTICLE_MO_ID_PARAM, s);
    }
}
