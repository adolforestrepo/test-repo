package org.astd.reallysi.rsuite.api.workflow;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContextAware;
import com.reallysi.rsuite.api.i18n.MessageResources;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.IDGenerator;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.MetaDataService;
import com.reallysi.rsuite.service.ProcessDefinitionService;
import com.reallysi.rsuite.service.ProcessInstanceService;
import com.reallysi.rsuite.service.RepositoryService;
import com.reallysi.rsuite.service.SchemaService;
import com.reallysi.rsuite.service.SearchService;
import com.reallysi.rsuite.service.XmlApiManager;
import com.reallysi.rsuite.tools.ExceptionUtils;
import com.reallysi.rsuite.api.expressions.ParameterReplacer;
import com.reallysi.service.workflow.Constants;
import com.reallysi.service.workflow.action.ContextInstanceParameterResolver;
import org.astd.reallysi.rsuite.api.workflow.InnerException;

import com.reallysi.rsuite.api.workflow.WorkflowActionHandler;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.SystemException;
import com.reallysi.service.workflow.ingestion.bo.VariableBo;
import com.reallysi.rsuite.api.expressions.ExpressionUtils;
import com.reallysi.tools.BooleanUtils;
import com.reallysi.tools.DateFormatUtils;
import com.reallysi.tools.FileUtils;
import org.astd.reallysi.rsuite.api.workflow.StringUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;


















public abstract class AbstractBaseNonLeavingActionHandler
  implements WorkflowActionHandler, ActionHandler, ExecutionContextAware
{
  public static final String DEFAULT_COMMENT_STREAM_MESSAGE_LABEL = "RSuite";
  public static final String EXCEPTION_MESSAGE_VAR = "exceptionMessage";
  public static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss";
/*   78 */   public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
  


  public static final String DEFAULT_WORKFLOW_COMMENT_STREAM_NAME = "rsuite.commentStream.global";
  


  private static final long serialVersionUID = 1L;
  


/*   90 */   static Log log = LogFactory.getLog(AbstractBaseNonLeavingActionHandler.class);
  


  public static final String RSUITE_COMMENT_STREAM_GLOBAL = "rsuite.commentStream.global";
  

  public static final String EXCEPTION_HANDLING_BEHAVIOR_CATCH = "catch";
  

  public static final String EXCEPTION_HANDLING_BEHAVIOR_THROW = "throw";
  

  public static final String RSUITE_WORKFLOW_DEFAULT_EXCEPTION_HANDLING_BEHAVIOR_VARNAME = "rsuiteWorkflowDefaultExceptionHandlingBehavior";
  

  public static final String EXCEPTION_HANDLING_BEHAVIOR_PARAM = "exceptionHandlingBehavior";
  

  public static final String FAIL_DETAIL_PARAM = "failDetail";
  

  public static final String LEAVE_NODE_ON_COMPLETION = "leaveNodeOnCompletion";
  

  public static final String ALWAYS_TREAT_AS_XML_PARAM = "alwaysTreatAsXml";
  

  public static final String ALWAYS_TREAT_AS_XML_PARAM_DEFAULT = "xml, nxml, dita, ditamap";
  

  public static final String NEVER_TREAT_AS_XML_PARAM = "neverTreatAsXml";
  

  public static final String NEVER_TREAT_AS_XML_PARAM_DEFAULT = "incx, inx";
  

  public static final String COMMENT_STREAM_NAME_PARAM = "commentStreamName";
  

  public static final String COMMENT_STREAM_MESSAGE_PARAM = "commentStreamMessage";
  

  public static final String COMMENT_STREAM_MESSAGE_LABEL_PARAM = "commentStreamMessageLabel";
  


  public String getAlwaysTreatAsXml()
  {
     return getParameter("alwaysTreatAsXml");
  }
  
  public String getNeverTreatAsXml() {
     return getParameter("neverTreatAsXml");
  }
  
  public void setAlwaysTreatAsXml(String value) {
     setParameter("alwaysTreatAsXml", value);
  }
  
  public void setNeverTreatAsXml(String value) {
     setParameter("neverTreatAsXml", value);
  }
  
  public void setExceptionHandlingBehavior(String exceptionHandlingBehavior) {
     setParameter("exceptionHandlingBehavior", exceptionHandlingBehavior);
  }
  
  public void setCommentStreamMessage(String commentStreamMessage) {
     setParameter("commentStreamMessage", commentStreamMessage);
  }
  
  public void setCommentStreamMessageLabel(String commentStreamMessageLabel) {
     setParameter("commentStreamMessageLabel", commentStreamMessageLabel);
  }
  
  public void setLeaveNodeOnCompletion(String leaveNodeOnCompletion) {
     setParameter("leaveNodeOnCompletion", leaveNodeOnCompletion);
  }
  





























   static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
   protected DecimalFormat numberFormat = new DecimalFormat("000000");
  



   protected Map<String, String> params = new HashMap();
  






  protected com.reallysi.rsuite.api.extensions.ExecutionContext context;
  





   protected boolean shouldLeaveNodeOnCompletion = false;
  







  public boolean ifLeaveNodeOnCompletion()
  {
     if (context.isShutdownInitiated()) {
       return false;
    }
     String lnocParam = getParameter("leaveNodeOnCompletion");
     if (!StringUtil.isNullOrEmptyOrSpace(lnocParam)) {
      try {
         boolean lnoc = Boolean.parseBoolean(lnocParam);
         shouldLeaveNodeOnCompletion = lnoc;
      }
      catch (Throwable e) {}
    }
    
     return shouldLeaveNodeOnCompletion;
  }
  




  public void setLeaveNodeOnCompletion(boolean leaveNodeOnCompletion)
  {
     shouldLeaveNodeOnCompletion = leaveNodeOnCompletion;
  }
  



  public abstract void execute(WorkflowExecutionContext paramWorkflowExecutionContext)
    throws Exception;
  



  public void setExecutionContext(com.reallysi.rsuite.api.extensions.ExecutionContext context)
  {
     this.context = context;
  }
  


  public AuthorizationService getAuthorizationService()
  {
     return context.getAuthorizationService();
  }
  


  public ProcessDefinitionService getProcessDefinitionService()
  {
     return context.getProcessDefinitionService();
  }
  


  public ProcessInstanceService getProcessInstanceService()
  {
     return context.getProcessInstanceService();
  }
  
  public MessageResources getMessageResources() {
     return context.getMessageResources();
  }
  


  public User getSystemUser()
  {
     return getAuthorizationService().getSystemUser();
  }
  


  public ContentAssemblyService getContentAssemblyService()
  {
     return context.getContentAssemblyService();
  }
  


  public ManagedObjectService getManagedObjectService()
  {
     return context.getManagedObjectService();
  }
  


  public SearchService getSearchService()
  {
     return context.getSearchService();
  }
  


  public XmlApiManager getXmlApiManager()
  {
     return context.getXmlApiManager();
  }
  

  public MetaDataService getMetaDataService()
  {
     return context.getMetaDataService();
  }
  


  public RepositoryService getRepositoryService()
  {
     return context.getRepositoryService();
  }
  


  public SchemaService getSchemaService()
  {
     return context.getSchemaService();
  }
  


  public IDGenerator getIDGenerator()
  {
     return context.getIDGenerator();
  }
  




  public ExpressionUtils getExpressionEvaluator()
  {
     return new ExpressionUtils(getRepositoryService());
  }
  
  public void execute(org.jbpm.graph.exe.ExecutionContext executionContext) throws Exception {
     long start = System.currentTimeMillis();
     ContextInstance contextInstance = executionContext.getContextInstance();
    
     String msgHeader = Constants.constructLoggerHeader(contextInstance);
    
     log.info(msgHeader + getClass().getSimpleName() + " starts...");
    try
    {
       formatVariable(executionContext);
      
       VariableBo variableBo = (VariableBo)VariableBo.take(contextInstance, VariableBo.class);
       if (variableBo == null) {
         variableBo = new VariableBo();
         variableBo.put(contextInstance);
      }
      
       Node node = executionContext.getProcessInstance().getRootToken().getNode();
      
       Log log = constructWorkflowLog(context.getProcessInstanceService().getWorkflowLogManager(), contextInstance);
      


       ProcessInstance pi = executionContext.getProcessInstance();
      
       WorkflowExecutionContext localContext = new AbstractBaseActionHandler.LocalExecutionContext(log, context, String.valueOf(pi.getId()), executionContext, contextInstance, variableBo);
      







       localContext.setAttribute(org.jbpm.graph.exe.ExecutionContext.class.getName(), executionContext);
       localContext.setAttribute(ContextInstance.class.getName(), contextInstance);
       localContext.setAttribute(ProcessInstance.class.getName(), pi);
       localContext.setAttribute(TaskInstance.class.getName(), executionContext.getTaskInstance());
      
       logActionHandlerExecution(log, executionContext);
      
       execute(localContext);
      
       handleCommentStreamMessageParam(localContext);

    }
    catch (InnerException e)
    {
       handleException(executionContext, contextInstance, msgHeader, "EditorTaskException", e, "EXCEPTION_TYPE_INNER");



    }
    catch (BusinessRuleException e)
    {


       handleException(executionContext, contextInstance, msgHeader, "BusinessRuleException", e, "EXCEPTION_TYPE_BUSINESSRULE");



    }
    catch (Exception e)
    {


       handleException(executionContext, contextInstance, msgHeader, "Exception", e, "EXCEPTION_TYPE_SYSTEM");




    }
    catch (Throwable e)
    {



       handleException(executionContext, contextInstance, msgHeader, "Exception", new Exception(e), "EXCEPTION_TYPE_SYSTEM");
    }
    





     log.info(msgHeader + getClass().getSimpleName() + " ends in " + (System.currentTimeMillis() - start) + " ms.");
    
    try
    {
       if (ifShouldLeaveNode(executionContext)) {
         executionContext.leaveNode();
      }
    }
    catch (Exception e) {
       log.warn("Exception leaving node: " + getClass().getSimpleName(), e);
    }
  }
  












  protected boolean ifShouldLeaveNode(org.jbpm.graph.exe.ExecutionContext executionContext)
  {
     Object rsuiteProcessSingleStepMode = executionContext.getVariable("rsuiteProcessSingleStepMode");
    
     if (rsuiteProcessSingleStepMode == null) {
       return ifLeaveNodeOnCompletion();
    }
     boolean isSingleStepping = BooleanUtils.isTrue(rsuiteProcessSingleStepMode.toString(), false);
    
     return (!isSingleStepping) && (ifLeaveNodeOnCompletion());
  }
  
  protected void logActionHandlerExecution(Log log, org.jbpm.graph.exe.ExecutionContext executionContext) {
     ProcessInstance pi = executionContext.getProcessInstance();
     StringBuilder buf = new StringBuilder();
     buf.append("Process ");
     buf.append(pi.getId());
     buf.append(" (Token ");
     buf.append(executionContext.getToken().getId());
     buf.append(") executing action handler: ");
     buf.append(getClass().getSimpleName());
     log.info(buf.toString());
  }
  














  protected void handleException(org.jbpm.graph.exe.ExecutionContext executionContext, ContextInstance contextInstance, String msgHeader, String exceptionName, Exception e, String exceptionType)
    throws Exception
  {
     String defaultExceptionHandlingBehavior = (String)executionContext.getVariable("rsuiteWorkflowDefaultExceptionHandlingBehavior");
    
     String behavior = getParameter("exceptionHandlingBehavior");
     if ((behavior == null) || ("".equals(behavior)))
    {
       behavior = defaultExceptionHandlingBehavior == null ? "catch" : defaultExceptionHandlingBehavior;
    }
    
     if ("throw".equals(behavior))
    {
       throwException(e, executionContext, msgHeader);
    }
     else if ("catch".equals(behavior))
    {
       logExceptionsAndSetExceptionOccur(executionContext, contextInstance, msgHeader, e, exceptionType);

    }
    else
    {
       log.error("Unrecognized value \"" + behavior + "\" for effective value of " + "exceptionHandlingBehavior" + "\" parameter. " + "Variable " + "rsuiteWorkflowDefaultExceptionHandlingBehavior" + " has value \"" + defaultExceptionHandlingBehavior + "\"");
      



       logExceptionsAndSetExceptionOccur(executionContext, contextInstance, msgHeader, e, exceptionType);
    }
  }
  





  protected void throwException(Exception e, org.jbpm.graph.exe.ExecutionContext executionContext, String msgHeader)
    throws Exception
  {
     logException(e, executionContext, msgHeader);
     throw e;
  }
  
  protected void logExceptionsAndSetExceptionOccur(org.jbpm.graph.exe.ExecutionContext executionContext, ContextInstance contextInstance, String msgHeader, Throwable e, String exceptionType)
    throws RSuiteException
  {
     Log wfLog = context.getProcessInstanceService().getWorkflowLogManager().getOrCreateWorkflowLog(Long.toString(executionContext.getProcessDefinition().getId()), executionContext.getProcessDefinition().getName(), Long.toString(executionContext.getProcessInstance().getId()));
    


     StringBuilder buff = new StringBuilder(exceptionType);
     buff.append(" is caught in AbstractBaseActionHandler");
    




     boolean includeStackTrace = true;
     if ((e instanceof BusinessRuleException))
       includeStackTrace = false;
     if (((e instanceof RSuiteException)) && (((RSuiteException)e).isNoStackTraceSuggested())) {
       includeStackTrace = false;
    }
     if (includeStackTrace) {
       ExceptionUtils.formatExceptionToBuffer(e, buff);
       wfLog.warn(buff.toString());
    }
    else
    {
       log.info("Business rule exception in workflow: " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    
     VariableBo variableBo = (VariableBo)VariableBo.take(contextInstance, VariableBo.class);
     variableBo.addSystemValue("EXCEPTION_OCCUR", "true", contextInstance);
     variableBo.addSystemValue("EXCEPTION_TYPE", exceptionType, contextInstance);
     logExceptionToWorkflowLog(e, executionContext);
     logException(e, executionContext, msgHeader);
  }
  







  protected void logExceptionToWorkflowLog(Throwable e, org.jbpm.graph.exe.ExecutionContext executionContext)
    throws RSuiteException
  {
     Log wfLog = context.getProcessInstanceService().getWorkflowLogManager().getOrCreateWorkflowLog(Long.toString(executionContext.getProcessDefinition().getId()), executionContext.getProcessDefinition().getName(), Long.toString(executionContext.getProcessInstance().getId()));
    



     String msg = "";
     if ((e instanceof BusinessRuleException)) {
       msg = ((BusinessRuleException)e).getFailureDetail();
    }
     if (((e instanceof RSuiteException)) && (((RSuiteException)e).isNoStackTraceSuggested())) {
       wfLog.error(e.getMessage());
    } else {
       wfLog.error("- Exception: " + msg, e);
    }
  }
  
  protected void logException(Throwable e, org.jbpm.graph.exe.ExecutionContext executionContext, String msgHeader) {
     if ((e instanceof BusinessRuleException)) {
       processIngestionBusinessRule((WorkflowObjectListingException)e, executionContext, msgHeader);
    } else {
       processJavaException(e, executionContext, msgHeader);
    }
  }
  







  protected void processJavaException(Throwable e, org.jbpm.graph.exe.ExecutionContext executionContext, String msgHeader)
  {
     List<ProcessedWorkflowObject> errorFiles = new ArrayList();
    





     WorkflowJobContext fbo = (WorkflowJobContext)WorkflowJobContext.take(executionContext.getContextInstance(), WorkflowJobContext.class);
    String srcFile;
    String srcFile;
     if (fbo == null) {
       srcFile = "NO ERROR FILE";
    } else {
       srcFile = fbo.getSourceFilePath();
    }
     if (StringUtil.isNullOrEmptyOrSpace(srcFile)) {
       srcFile = "NO ERROR FILE";
    }
     errorFiles.add(new FileWorkflowObject(new File(srcFile)));
     String bruleHandler = getClass().getName();
     String detail = e.toString();
     Date errorDate = DateFormatUtils.getNow();
     BusinessRuleException bre = new SystemException(errorFiles);
     bre.setFailureDetail(detail);
     bre.setSourceName(bruleHandler);
     bre.setErrorDate(errorDate);
    


     if (bre.getErrorSourceNodeName() == null)
    {

       Node node = executionContext.getNode();
       if (node != null) {
         bre.setErrorSourceNodeName(node.getName());
      }
    }
     WorkflowErrors workflowErrors = (WorkflowErrors)WorkflowErrors.take(executionContext.getContextInstance(), WorkflowErrors.class);
    
     workflowErrors = workflowErrors == null ? new WorkflowErrors() : workflowErrors;
    
     workflowErrors.setTaskname("System Error");
     workflowErrors.addException(bre);
    




     workflowErrors.addSystemException(e);
    
     workflowErrors.put(executionContext.getContextInstance());
  }
  











  protected void processIngestionBusinessRule(WorkflowObjectListingException e, org.jbpm.graph.exe.ExecutionContext executionContext, String msgHeader)
  {
     BusinessRuleException bre = (BusinessRuleException)e;
    


     if (bre.getErrorSourceNodeName() == null)
    {

       Node node = executionContext.getNode();
       if (node != null) {
         bre.setErrorSourceNodeName(node.getName());
      }
    }
    
     if (bre.getFailureDetail() == null)
    {



       bre.setFailureDetail(getFailDetail());
    }
    


     List<ProcessedWorkflowObject> errorWorkflowObjects = bre.getFailedObjectsList();
     String bruleHandler = bre.getSourceName();
    

     Date errorDate = bre.getErrorDate();
    
     WorkflowJobContext folderPathBo = (WorkflowJobContext)WorkflowErrors.take(executionContext.getContextInstance(), WorkflowJobContext.class);
    
     String workflowname = executionContext.getProcessDefinition().getName();
     String objectName = "";
     if (folderPathBo != null)
    {
       String sourceFile = folderPathBo.getSourceFile();
       if (sourceFile != null) {
         objectName = FileUtils.getFileNameFromPath(sourceFile);
      } else {
         objectName = "{No source file for folderPathBo}";
      }
    }
    else {
       objectName = "Failure";
    }
    
     WorkflowErrors bo = (WorkflowErrors)WorkflowErrors.take(executionContext.getContextInstance(), WorkflowErrors.class);
    
     bo = bo == null ? new WorkflowErrors() : bo;
     bo.setTaskname(workflowname + ": " + objectName);
     bo.addException(bre);
     bo.put(executionContext.getContextInstance());
  }
  

  protected String[] formatAll(ContextInstance ci, List<ProcessedWorkflowObject> errorFiles, String failDetail)
  {
     String[] result = new String[errorFiles.size()];
    
     for (int i = 0; i < errorFiles.size(); i++) {
       VariableBo vbo = (VariableBo)VariableBo.take(ci, VariableBo.class);
       String tmpStr = ((ProcessedWorkflowObject)errorFiles.get(i)).getName();
       if (failDetail != null) {
         tmpStr = failDetail;
      }
       else if (vbo != null)
         tmpStr = vbo.resolve(failDetail, ((ProcessedWorkflowObject)errorFiles.get(i)).getName());
      try {
         tmpStr = evaluteExpressions(context, tmpStr);
      } catch (RSuiteException e) {
         log.error("Exception evaluating expressions: " + e.getMessage(), e);
      }
       if (tmpStr != null) {
         result[i] = tmpStr.replaceAll("'", "&apos;");
      } else
         result[i] = "{No failure detail}";
    }
     return result;
  }
  
  protected void formatVariable(org.jbpm.graph.exe.ExecutionContext executionContext)
    throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
  {
     VariableBo vbo = (VariableBo)VariableBo.take(executionContext.getContextInstance(), VariableBo.class);
    
     if (vbo != null) {
       Class clazz = getClass();
       Field[] fields = clazz.getDeclaredFields();
       Method[] methods = clazz.getMethods();
       for (Field field : fields) {
         boolean hasGet = false;
         boolean hasSet = false;
         Method getMethod = null;
         Method setMethod = null;
         if (field.getType().getName().equalsIgnoreCase("java.lang.String"))
        {
           for (Method method : methods) {
             String methodName = method.getName();
             if ((methodName.equalsIgnoreCase("get" + field.getName())) && (method.getModifiers() == 1))
            {
               hasGet = true;
               getMethod = method;
            }
             else if ((methodName.equalsIgnoreCase("set" + field.getName())) && (method.getModifiers() == 1))
            {
               hasSet = true;
               setMethod = method;
            }
          }
          


           if ((hasGet & hasSet)) {
             String value = (String)getMethod.invoke(this, new Object[0]);
             String newValue = vbo.resolve(value, null);
             setMethod.invoke(this, new Object[] { newValue });
          }
        }
      }
    }
  }
  





  protected String resolveVariables(WorkflowExecutionContext executionContext, String text)
    throws RSuiteException
  {
     return resolveVariables(executionContext, null, null, text);
  }
  




  protected String resolveVariables(WorkflowExecutionContext executionContext, String rsuiteVariableKey, String text)
    throws RSuiteException
  {
     VariableBo vbo = (VariableBo)VariableBo.take(executionContext.getContextInstance(), VariableBo.class);
     return resolveVariables(executionContext, vbo, rsuiteVariableKey, text);
  }
  








  protected String resolveVariables(WorkflowExecutionContext executionContext, VariableBo rsuiteVariables, String rsuiteVariableKey, String text)
    throws RSuiteException
  {
     if ((rsuiteVariables != null) && (rsuiteVariableKey != null))
    {
       text = rsuiteVariables.resolve(text, rsuiteVariableKey);
    }
    


     ParameterReplacer parser = new ParameterReplacer();
    
     return parser.replaceParams(new ContextInstanceParameterResolver(executionContext.getContextInstance(), executionContext.getConfigurationProperties()), text);
  }
  







  protected ArrayList<MoWorkflowObject> getMOsToProcess(WorkflowExecutionContext context)
  {
     MoListWorkflowObject moWFO = context.getMoListWorkflowObject();
     if ((moWFO == null) || (moWFO.getMoList() == null)) return new ArrayList();
     ArrayList<MoWorkflowObject> arrayList = new ArrayList(moWFO.getMoList());
     return arrayList;
  }
  








  protected ArrayList<File> getFilesToProcess(String initialFilePath)
  {
     ArrayList<File> fileList = new ArrayList();
     File initialFile = new File(initialFilePath);
     if (!initialFile.exists())
       return fileList;
     if (initialFile.isFile())
    {
       fileList.add(initialFile);
    }
    else
    {
       addFilesToList(fileList, initialFile.listFiles());
    }
    
     return fileList;
  }
  








  protected void addFilesToList(ArrayList<File> fileList, File[] files)
  {
     for (File f : files)
    {
       if (f.isFile())
      {
         fileList.add(f);
      }
      else
      {
         addFilesToList(fileList, f.listFiles());
      }
    }
  }
  





  protected void setParameter(String name, String value)
  {
     if (log.isDebugEnabled())
       log.debug("Setting parameter [" + name + "] to \"" + value + "\"");
     params.put(name, value);
  }
  



  protected String getParameter(String name)
  {
     if (params.containsKey(name)) return (String)params.get(name);
     return null;
  }
  
  protected Map<String, String> getParameters() {
     return params;
  }
  
  public void setFailDetail(String failDetail) {
     setParameter("failDetail", failDetail);
  }
  
  public String getFailDetail() {
     return (String)params.get("failDetail");
  }
  




  protected String getParameterWithDefault(String name, String defaultValue)
  {
     String value = getParameter(name);
     if (StringUtil.isNullOrEmpty(value)) return defaultValue;
     return value;
  }
  



  protected void checkParamsNotEmptyOrNull(String... params)
    throws RSuiteException
  {
     boolean paramFound = false;
     for (String param : params) {
       paramFound = (paramFound) || (!StringUtil.isNullOrEmptyOrSpace(getParameter(param)));
    }
     if (!paramFound) {
       String parms = params[0];
       for (int i = 1; i < params.length; i++) {
         parms = parms + ", " + params[i];
      }
       throw new RSuiteException("At least one of the parameters [" + parms + "] must be specified");
    }
  }
  
  protected Log constructWorkflowLog(WorkflowLogManager workflowLogManager, ContextInstance contextInstance) throws RSuiteException
  {
     ProcessInstance pi = contextInstance.getProcessInstance();
     ProcessDefinition pd = pi.getProcessDefinition();
    
     Log log = workflowLogManager.getOrCreateWorkflowLog(String.valueOf(pd.getId()), pd.getName(), String.valueOf(pi.getId()));
    


     return log;
  }
  












  protected String evaluteExpressions(org.jbpm.graph.exe.ExecutionContext context, String textWithVariablesResolved)
    throws RSuiteException
  {
     ExpressionUtils evaluator = getExpressionEvaluator();
     List<String> evaluationResult = evaluator.resolveExpressions(textWithVariablesResolved);
     String result = StringUtil.join(evaluationResult);
     return result;
  }
  





  protected String evaluteExpressions(com.reallysi.rsuite.api.extensions.ExecutionContext context, String textWithVariablesResolved)
    throws RSuiteException
  {
     if (textWithVariablesResolved == null) return null;
     ExpressionUtils evaluator = getExpressionEvaluator();
     List<String> evaluationResult = evaluator.resolveExpressions(textWithVariablesResolved);
     String result = StringUtil.join(evaluationResult);
     return result;
  }
  




  protected String resolveVariablesAndExpressions(WorkflowExecutionContext executionContext, String text)
  {
     return resolveVariablesAndExpressions(executionContext, text, null);
  }
  






  protected String resolveVariablesAndExpressions(WorkflowExecutionContext executionContext, String text, String mapKey)
  {
     String result = text;
    try {
       result = resolveVariables(executionContext, mapKey, result);
       result = evaluteExpressions(executionContext, result);
    } catch (RSuiteException e) {
       log.error("Exception from resolveVariables or evaluateExpressions", e);
    }
     return result;
  }
  


  protected void logActionHandlerParameters(Log log)
  {
     Map<String, String> parms = getParameters();
     log.info("  Parameters:");
     for (Map.Entry<String, String> entry : parms.entrySet()) {
       log.info("   [" + (String)entry.getKey() + "] = \"" + (String)entry.getValue() + "\"");
    }
  }
  





  protected boolean treatAsXml(WorkflowExecutionContext context, String fileName)
  {
     String inExtension = FileUtils.getFileSuffix(fileName, "");
    
     String xml_file_ext = context.getRSuiteServerConfiguration().getTreatAsXmlFileExtensionsAsCommaDelimitedString();
    
     String alwaysTreatAsXml = getParameterWithDefault("alwaysTreatAsXml", "xml, nxml, dita, ditamap," + xml_file_ext);
     alwaysTreatAsXml = resolveVariablesAndExpressions(context, alwaysTreatAsXml, fileName);
     String neverTreatAsXml = getParameterWithDefault("neverTreatAsXml", "incx, inx");
     neverTreatAsXml = resolveVariablesAndExpressions(context, neverTreatAsXml, fileName);
    

     String[] never = neverTreatAsXml.split(",\\s*");
     for (String candExt : never) {
       if (candExt.startsWith("."))
         candExt = candExt.substring(1);
       if (candExt.equals(inExtension)) {
         return false;
      }
    }
     String[] always = alwaysTreatAsXml.split(",\\s*");
     for (String candExt : always) {
       if (candExt.startsWith("."))
         candExt = candExt.substring(1);
       if (candExt.equals(inExtension)) {
         return true;
      }
    }
     return false;
  }
  


  protected void removeMoFromMOsToProcess(WorkflowExecutionContext context, MoWorkflowObject moObj)
  {
     removeMoFromMOsToProcess(context, moObj, null);
  }
  



  protected void removeMoFromMOsToProcess(WorkflowExecutionContext context, MoWorkflowObject moObj, Log log)
  {
     MoListWorkflowObject moList = context.getMoListWorkflowObject();
     if (log != null)
       log.info("removeMoFromMOsToProcess(): Removing MO " + moObj.getMoid() + ", moList.size()=" + moList.getMoList().size());
     for (MoWorkflowObject cand : moList.getMoList()) {
       if (cand.getMoid().equals(moObj.getMoid())) {
         moList.removeMo(cand);
         break;
      }
    }
     if (log != null)
       log.info("removeMoFromMOsToProcess(): After removing MO " + moObj.getMoid() + ", moList.size()=" + moList.getMoList().size());
     context.setMoListWorkflowObject(moList);
  }
  
  public void setCommentStreamName(String value)
  {
     setParameter("commentStreamName", value);
  }
  








  protected void handleCommentStreamMessageParam(WorkflowExecutionContext context)
    throws RSuiteException
  {
     String message = getParameter("commentStreamMessage");
     message = resolveVariablesAndExpressions(context, message);
     if (!StringUtil.isNullOrEmptyOrSpace(message)) {
       String commentLabel = getParameterWithDefault("commentStreamMessageLabel", "RSuite");
       commentLabel = resolveVariablesAndExpressions(context, commentLabel);
      
       String streamName = getParameter("commentStreamName");
       streamName = resolveVariablesAndExpressions(context, streamName);
      

       if ((null == streamName) || ("".equals(streamName))) {
         streamName = "rsuite.commentStream.global";
      }
       createWorkflowComment(context, commentLabel, streamName, message);
    }
  }
  






  protected void createWorkflowComment(WorkflowExecutionContext context, String message)
    throws RSuiteException
  {
     createWorkflowComment(context, "rsuite.commentStream.global", getSystemUser().getUserId(), message);
  }
  








  protected void createWorkflowComment(WorkflowExecutionContext context, String commentLabel, String message)
    throws RSuiteException
  {
     createWorkflowComment(context, "rsuite.commentStream.global", commentLabel, message);
  }
  









  protected void createWorkflowComment(WorkflowExecutionContext context, String commentLabel, String streamName, String message)
    throws RSuiteException
  {
     Log wfLog = context.getWorkflowLog();
    
     CommentStream cs = (CommentStream)context.getVariableAsObject(streamName);
     if (cs == null) {
       wfLog.info("Creating new comment stream named \"" + streamName + "\"");
       cs = new CommentStream(streamName, null);
    }
    
     wfLog.info("Adding comment\"" + message + "\" to comment stream \"" + streamName + "\"");
     Comment comment = new Comment(commentLabel, Calendar.getInstance().getTime(), message);
     cs.addComment(comment);
     context.getContextInstance().setVariable(streamName, cs);
  }
  
  protected void reportAndThrowRSuiteException(WorkflowExecutionContext context, String msg) throws RSuiteException
  {
     context.setVariable("exceptionMessage", msg);
     createWorkflowComment(context, "RSuite", "rsuite.commentStream.global", "Error: " + msg);
     throw new RSuiteException(0, msg);
  }
  





  protected void reportAndThrowRSuiteException(WorkflowExecutionContext context, String msg, Exception e)
    throws RSuiteException
  {
     context.setVariable("exceptionMessage", msg);
     createWorkflowComment(context, "RSuite", "rsuite.commentStream.global", "Error: " + msg);
     throw new RSuiteException(0, msg, e);
  }
  
  public static String getNowString()
  {
     String timeStr = DATE_FORMAT.format(new Date());
     return timeStr;
  }
  






  public File getWorkingDir(WorkflowExecutionContext context, boolean deleteOnExit)
    throws Exception
  {
     WorkflowJobContext wjc = context.getWorkflowJobContext();
    
     File workDir = null;
     if (wjc == null) {
       workDir = getTempDir("workflowProcess_" + context.getProcessInstanceId() + "_at_", deleteOnExit);
    } else {
       workDir = new File(wjc.getWorkFolderPath());
       if (!workDir.exists())
         workDir.mkdirs();
    }
     return workDir;
  }
  




  protected static File getTempDir(String prefix, boolean deleteOnExit)
    throws Exception
  {
     File tempFile = File.createTempFile(prefix, "trash");
     File tempDir = new File(tempFile.getAbsolutePath() + "_dir");
     tempDir.mkdirs();
     tempFile.delete();
     if (deleteOnExit) tempDir.deleteOnExit();
     return tempDir;
  }
}

/* Location:           C:\Users\ibrahim\git\astd_source\java\lib\rsuite-api.jar
 * Qualified Name:     com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.7.1
 */