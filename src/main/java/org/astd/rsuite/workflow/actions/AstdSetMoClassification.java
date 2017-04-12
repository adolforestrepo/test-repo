package org.astd.rsuite.workflow.actions;

import java.util.List;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import org.astd.reallysi.rsuite.api.workflow.StringUtil;

/**
 * Set taxonomy classification for an MO.
 * <p>The MO to set classification data for is either specified
 * by the <tt>moId</tt> or <tt>moAlias</tt> parameter.  If neither
 * parameter is specified, than context variable is used to determine
 * the ID of the MO to retrieve classification data from.
 * </p>
 * <p>The classification data is specified by the <tt>data</tt>
 * parameter.  Normally, this should be a variable reference
 * since the data should be in XML format as defined by
 * ASTD DTDs.
 * </p>
 */
public class AstdSetMoClassification extends AstdActionHandlerBase {
    private static final long serialVersionUID = 1L;
    
    /**
     * (Optional) Alias to MO.
     */
    public static final String MO_ALIAS_PARAM = "moAlias";

    /**
     * (Optional) Alias to MO.
     */
    public static final String MO_ID_PARAM = "moId";

    /**
     * Name of the variable containing the classification xml.
     */
    public static final String DATA_PARAM = "data";

    @Override
    public void execute(
            WorkflowExecutionContext context
    ) throws Exception {
        Log wfLog = context.getWorkflowLog();
        User user = getSystemUser();

        String data = resolveVariables(context,
                getParameter(DATA_PARAM));
        if (StringUtil.isNullOrEmptyOrSpace(data)) {
        	wfLog.info("No classification data provided, nothing to do");
            return;
        }

        String moId = resolveVariables(context,
                getParameter(MO_ID_PARAM));
        if (StringUtil.isNullOrEmptyOrSpace(moId)) {
        	String moAlias = resolveVariables(context,
                    getParameter(MO_ALIAS_PARAM));
        	if (!StringUtil.isNullOrEmptyOrSpace(moAlias)) {
        	 	ManagedObject mo = context.getManagedObjectService()
                                          .getObjectByAlias(user, moAlias);
        	 	if (mo != null) {
        	 		moId = mo.getId();
        	 	} else {
                    MoListWorkflowObject moList =
                    	context.getMoListWorkflowObject();
                    if (moList == null || moList.isEmpty()) {
                        reportAndThrowRSuiteException(context,
                            "MO ID not specified to set classification");
                    }
                    moId = moList.getMoList().get(0).getMoid();
        	 	}
        	}
        }
       	wfLog.info("Classification data to set for "+moId+": "+data);
       	try {
       		AstdActionUtils.setClassificationXmlForMo(
       				context, user, wfLog, moId, data);
       	} catch (RSuiteException rse) {
            reportAndThrowRSuiteException(context,
            		"Error setting classification for"+moId+": "+
            		rse.getLocalizedMessage(), rse);
       	}
    }

    public void setData(String s) {
        setParameter(DATA_PARAM, s);
    }

    public void setMoId(String s) {
        setParameter(MO_ID_PARAM, s);
    }

    public void setMoAlias(String s) {
        setParameter(MO_ALIAS_PARAM, s);
    }
}
