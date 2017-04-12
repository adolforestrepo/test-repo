package org.astd.rsuite.workflow.actions;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContextWrapper;
import com.reallysi.rsuite.system.workflow.ProcessInstanceSummaryInfoImpl;
import com.reallysi.rsuite.api.expressions.ParameterReplacer;
import com.reallysi.service.workflow.action.ContextInstanceParameterResolver;
import com.reallysi.service.workflow.ingestion.bo.VariableBo;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.astd.rsuite.workflow.actions.nonleaving.BaseNonLeavingActionHandler;







public abstract class AbstractBaseActionHandler
  extends BaseNonLeavingActionHandler
{
   static Log log = LogFactory.getLog(AbstractBaseActionHandler.class);
  
  public static class LocalExecutionContext
    extends ExecutionContextWrapper
    implements WorkflowExecutionContext
  {
     private Map<String, Object> attributes = null;
    private ContextInstance contextInstance;
    private org.jbpm.graph.exe.ExecutionContext executionContext;
    private KeyedVariableMap variables;
     private Log workflowLog = null;
    

    private String processInstanceId;
    

    private ProcessInstanceSummaryInfo processInstanceSummaryInfo;
    


    public LocalExecutionContext(Log workflowLog, com.reallysi.rsuite.api.extensions.ExecutionContext delegate, String processInstanceId, org.jbpm.graph.exe.ExecutionContext executionContext, ContextInstance contextInstance, KeyedVariableMap variables)
    {
       super();
       this.workflowLog = workflowLog;
       this.processInstanceId = processInstanceId;
       this.executionContext = executionContext;
       this.contextInstance = contextInstance;
       this.variables = variables;
       attributes = new HashMap();
      
       processInstanceSummaryInfo = new ProcessInstanceSummaryInfoImpl(contextInstance.getProcessInstance().getProcessDefinition().getName(), String.valueOf(contextInstance.getProcessInstance().getProcessDefinition().getId()), String.valueOf(contextInstance.getProcessInstance().getProcessDefinition().getVersion()), processInstanceId, contextInstance.getProcessInstance().getStart(), contextInstance.getProcessInstance().getEnd());
    }
    










    public Log getWorkflowLog()
    {
       return workflowLog;
    }
    
    public String getProcessInstanceId()
    {
       return processInstanceId;
    }
    
    public ProcessInstanceSummaryInfo getProcessInstanceSummaryInfo()
    {
       return processInstanceSummaryInfo;
    }
    

    public ContextInstance getContextInstance()
    {
       return contextInstance;
    }
    
    public void setAttribute(String name, Object value)
    {
       attributes.put(name, value);
    }
    
    public Object getAttribute(String name)
    {
       return attributes.get(name);
    }
    
    public Map<String, Object> getAttributes()
    {
       return attributes;
    }
    
    public Token getCurrentToken()
    {
       return executionContext.getToken();
    }
    


    public void setLocalVariable(String name, Object value)
    {
       if (AbstractBaseActionHandler.log.isDebugEnabled())
      {
         AbstractBaseActionHandler.log.debug("setLocalVariable: enter, name=" + name);
         AbstractBaseActionHandler.log.debug("setLocalVariable: enter, value type=" + value.getClass().getName());
      }
       ContextInstance ci = executionContext.getContextInstance();
       Token currentToken = getCurrentToken();
      
       if (ci.getLocalVariable(name, currentToken) == null) {
         ci.createVariable(name, value, currentToken);
      } else {
         executionContext.setVariable(name, value);
      }
    }
    

    public void setGlobalVariable(String name, Object value)
    {
       if (AbstractBaseActionHandler.log.isDebugEnabled())
      {
         AbstractBaseActionHandler.log.debug("setGlobalVariable: enter, name=" + name);
         AbstractBaseActionHandler.log.debug("setGlobalVariable: enter, value type=" + value.getClass().getName());
      }
       ContextInstance ci = executionContext.getContextInstance();
       ProcessInstance pi = ci.getProcessInstance();
       Token rootToken = pi.getRootToken();
      
       if (ci.getLocalVariable(name, rootToken) == null) {
         ci.createVariable(name, value, rootToken);
      } else {
         ci.setVariable(name, value, rootToken);
      }
    }
    



    public void setGlobalVariable(Class<? extends Object> clazz, Object value)
      throws RSuiteException
    {
       String varName = clazz.getName();
       setGlobalVariable(varName, value);
    }
    
    public Map<String, Object> getWorkflowVariables()
    {
       return contextInstance.getVariables();
    }
    
    public KeyedVariableMap getVariables()
    {
       return variables;
    }
    
    public FileWorkflowObject getFileWorkflowObject()
    {
       FileWorkflowObject filebo = (FileWorkflowObject)FileWorkflowObject.take(contextInstance, FileWorkflowObject.class.getName());
       return filebo;
    }
    



    public MoListWorkflowObject getMoListWorkflowObject()
    {
       MoListWorkflowObject moWFO = (MoListWorkflowObject)executionContext.getVariable(MoListWorkflowObject.class.getName());
      
       if (moWFO == null)
      {
         moWFO = new MoListWorkflowObject();
         moWFO.build((String)executionContext.getVariable("rsuite contents"));
      }
       return moWFO;
    }
    
    public void setMoListWorkflowObject(MoListWorkflowObject o)
    {
       setLocalVariable("rsuite contents", o.toString());
       setLocalVariable(MoListWorkflowObject.class.getName(), o);
    }
    


    public void setFileWorkflowObject(ProcessedWorkflowObject o)
    {
       o.put(contextInstance);
    }
    


    public String getVariable(String varName)
      throws RSuiteException
    {
       String value = variables.get(null, varName);
       if (value != null)
         return value;
       Object valueObj = getVariableAsObject(varName);
       return valueObj == null ? null : valueObj.toString();
    }
    
    public void setVariable(String varName, Object value)
      throws RSuiteException
    {
       setLocalVariable(varName, value);
    }
    

    public String getVariable(String parent, String varName)
      throws RSuiteException
    {
       if (parent == null) {
         return getVariable(varName);
      }
       return variables.get(parent, varName);
    }
    

    public WorkflowJobContext getWorkflowJobContext()
    {
       WorkflowJobContext wjc = (WorkflowJobContext)WorkflowJobContext.take(contextInstance, WorkflowJobContext.class);
       return wjc;
    }
    






    public String resolveVariables(String rsuiteVariableKey, String text)
      throws RSuiteException
    {
       KeyedVariableMap variables = getVariables();
      
       return resolveVariables(variables, rsuiteVariableKey, text);
    }
    











    public String resolveVariables(KeyedVariableMap variables, String rsuiteVariableKey, String text)
      throws RSuiteException
    {
       if ((variables != null) && (rsuiteVariableKey != null))
      {
         text = variables.resolve(text, rsuiteVariableKey);
      }
      


       ParameterReplacer parser = new ParameterReplacer();
      
       return parser.replaceParams(new ContextInstanceParameterResolver(contextInstance, delegate.getConfigurationProperties()), text);
    }
    





    public Object getVariableAsObject(Class<? extends Object> clazz)
      throws RSuiteException
    {
       String varName = clazz.getName();
       return getVariableAsObject(varName);
    }
    

    public Object getVariableAsObject(String varName)
      throws RSuiteException
    {
       Object value = executionContext.getVariable(varName);
       return value;
    }
    



    public void setVariable(Class<? extends Object> clazz, Object value)
      throws RSuiteException
    {
       String varName = clazz.getName();
       setVariable(varName, value);
    }
    



    public void setVariable(String mapKey, String varName, String value)
      throws RSuiteException
    {
       if (variables == null) {
         variables = new VariableBo();
      }
       variables.put(mapKey, null, varName, value);
    }
    



    public void setVariable(String mapKey, String parent, String varName, String value)
      throws RSuiteException
    {
       variables.put(mapKey, parent, varName, value);
    }
    



    public TaskInstance getTaskInstance()
    {
       TaskInstance ti = (TaskInstance)getAttribute(TaskInstance.class.getName());
       return ti;
    }
    


    public ProcessInstance getProcessInstance()
    {
       return contextInstance.getProcessInstance();
    }
    

    public WorkflowErrors getWorkflowErrors()
      throws RSuiteException
    {
       WorkflowErrors wfErrors = (WorkflowErrors)getVariableAsObject(WorkflowErrors.class);
       if (wfErrors == null) {
         wfErrors = new WorkflowErrors();
         setVariable(WorkflowErrors.class, wfErrors);
      }
       return wfErrors;
    }
  }
  









  public void execute(org.jbpm.graph.exe.ExecutionContext executionContext)
    throws Exception
  {
     setLeaveNodeOnCompletion(true);
     super.execute(executionContext);
  }
}

