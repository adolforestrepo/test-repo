package org.astd.rsuite.operation.result;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.operation.log.OperationLogger;
import org.astd.rsuite.utils.Transaction;
import org.astd.rsuite.visitor.HtmlFormattingOperationResultVisitor;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.ProcessInstanceSummaryInfo;
import com.rsicms.rsuite.helpers.messages.ProcessDebugMessage;
import com.rsicms.rsuite.helpers.messages.ProcessFailureMessage;
import com.rsicms.rsuite.helpers.messages.ProcessInfoMessage;
import com.rsicms.rsuite.helpers.messages.ProcessMessage;
import com.rsicms.rsuite.helpers.messages.ProcessWarningMessage;
import com.rsicms.rsuite.helpers.messages.Severity;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessDebugMessage;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessFailureMessage;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessInfoMessage;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessWarningMessage;
import com.rsicms.rsuite.helpers.messages.impl.ProcessMessageContainerImpl;

/**
 * Base class for various operations that want to get track of the operation's duration, messages,
 * and counters.
 */
public class OperationResult {

  /**
   * Name of counter: number of new MOs
   */
  private final static String COUNTER_NAME_MOS_CREATED = "mosCreated";

  /**
   * Name of counter: number of updated MOs
   */
  private final static String COUNTER_NAME_MOS_UPDATED = "mosUpdated";

  /**
   * Name of counter: number of skipped MOs
   */
  private final static String COUNTER_NAME_MOS_SKIPPED = "mosSkipped";

  /**
   * Name of counter: number of new MOs destroyed by rollback
   */
  private final static String COUNTER_NAME_NEW_MOS_ROLLED_BACK = "newMosRolledBack";

  /**
   * Name of counter: number of updated MOs reverted by rollback
   */
  private final static String COUNTER_NAME_UPDATED_MOS_ROLLED_BACK = "updatedMosRolledBack";

  /**
   * Name of counter: number of workflow process instances (jobs).
   */
  private final static String COUNTER_NAME_WORKFLOW_JOBS = "workflowJobs";

  /**
   * The result's message container. Best not for OperationResult to extend this class.
   */
  private ProcessMessageContainerImpl messageContainer;

  /**
   * The operation ID.
   */
  private String opId;

  /**
   * The moment the operation started.
   */
  private Date opStarted;

  /**
   * The moment the operation ended.
   */
  private Date opEnded;

  /**
   * A map of stop watches
   */
  private Map<String, Date> timers = new HashMap<String, Date>();

  /**
   * A map of counters. Key is the name of the counter. Value is the count.
   */
  private Map<String, Integer> counters;

  /**
   * The default object label.
   */
  private String defaultLabel;

  /**
   * All transactions associated with this operation.
   */
  private List<Transaction> transactions;

  /**
   * A list of <code>ContentAssembly</code> instances destroyed by this operation.
   */
  protected List<ContentAssembly> destroyedContentAssemblyList = new ArrayList<ContentAssembly>();

  /**
   * A list of <code>ManagedObject</code> instances destroyed by this operation.
   */
  protected List<ManagedObject> destroyedManagedObjectList = new ArrayList<ManagedObject>();

  /**
   * A list of role names processed by this operation.
   */
  protected List<String> processedRoleNamesList = new ArrayList<String>();

  /**
   * Logs all operations.
   */
  private OperationLogger opLogger;

  /**
   * Workflow process instances (jobs)
   */
  private List<ProcessInstanceSummaryInfo> workflowJobs;

  /**
   * The response or payload of the operation.
   */
  private String payload;

  /**
   * The content type of the payload
   */
  private String payloadContentType;

  /**
   * Constructor accepting default message type, object label, an instance of <code>Log</code> and
   * an Operation Id.
   * 
   * @param id The operation Id
   * @param defaultLabel
   * @param log The log to write messages to, in addition to populating the message container. OK to
   *        send null.
   */
  public OperationResult(
      String id, String defaultLabel, Log log) {
    this.messageContainer = new ProcessMessageContainerImpl();
    this.defaultLabel = defaultLabel;
    this.counters = new HashMap<String, Integer>();
    this.transactions = new ArrayList<Transaction>();
    this.opLogger = new OperationLogger(log);
    this.workflowJobs = new ArrayList<ProcessInstanceSummaryInfo>();
    this.opId = id;
    this.opLogger.setOpId(id);
  }

  public String getDefaultLabel() {
    return defaultLabel;
  }

  public void setLog(Log log) {
    this.opLogger.setLog(log);
  }

  public Log getLog() {
    return this.opLogger.getLog();
  }

  public String getOperationId() {
    return opId;
  }

  public void setOperationId(String id) {
    this.opId = id;
  }

  public void markStartOfOperation() {
    setStartOfOperation(new Date());
  }

  public void setStartOfOperation(Date start) {
    opStarted = start;
  }

  public Date getStartOfOperation() {
    return opStarted;
  }

  public void markEndOfOperation() {
    setEndOfOperation(new Date());
  }

  public void setEndOfOperation(Date start) {
    opEnded = start;
  }

  public Date getEndOfOperation() {
    return opEnded;
  }

  /**
   * Find out how long the operation took, in milliseconds.
   * 
   * @see #markStartOfOperation()
   * @see #setStartOfOperation(Date)
   * @see #markEndOfOperation()
   * @see #getOperationDurationInSeconds()
   * @return difference between start and end, in milliseconds.
   * @throws RSuiteException Thrown if the start or end of the operation was not specified.
   */
  public long getOperationDurationInMilliseconds() throws RSuiteException {
    if (opEnded == null || opStarted == null) {
      throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR,
          "error.unable.to.calculate.duration");
    }
    return opEnded.getTime() - opStarted.getTime();
  }

  /**
   * Get the operation's duration in milliseconds, suppressing any exceptions.
   * 
   * @return the operation's duration or -1 when unknown.
   */
  public long getOperationDurationInMillisecondsQuietly() {
    try {
      return getOperationDurationInMilliseconds();
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Find out how long the operation took, in seconds.
   * 
   * @see #markStartOfOperation()
   * @see #setStartOfOperation(Date)
   * @see #markEndOfOperation()
   * @see #getOperationDurationInMilliseconds()
   * @return difference between start and end, in seconds.
   * @throws RSuiteException Thrown if the start or end of the operation was not specified.
   */
  public long getOperationDurationInSeconds() throws RSuiteException {
    return getOperationDurationInMilliseconds() / 1000;
  }

  /**
   * Get the operation's duration in seconds, suppressing any exceptions.
   * 
   * @return the operation's duration or -1 when unknown.
   */
  public long getOperationDurationInSecondsQuietly() {
    try {
      return getOperationDurationInSeconds();
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Start a named timer.
   * 
   * @param name Name of timer.
   */
  public void startTimer(String name) {
    timers.put(name, new Date());
  }

  /**
   * Get the elapsed milliseconds of a named timer.
   * 
   * @param name Name of timer.
   * @return The number of elapsed milliseconds or -1 when timer wasn't started.
   */
  public long getElapsedTimeInMilliseconds(String name) {
    if (timers.containsKey(name)) {
      return new Date().getTime() - timers.get(name).getTime();
    }
    return -1;
  }

  /**
   * Get the elapsed seconds of a named timer.
   * 
   * @param name Name of timer.
   * @return The number of elapsed seconds or -1 when timer wasn't started.
   */
  public long getElapsedTimeInSeconds(String name) {
    long millis = getElapsedTimeInMilliseconds(name);
    if (millis > -1) {
      return millis / 1000;
    }
    return millis;
  }

  public void addWorkflowJob(ProcessInstanceSummaryInfo job) {
    if (job != null) {
      workflowJobs.add(job);
      incrementWorkflowJobsCount();
    }
  }

  public List<ProcessInstanceSummaryInfo> getWorkflowJobs() {
    return workflowJobs;
  }

  public void addFailure(Throwable t) {
    addFailure(defaultLabel, t);
  }

  public void addFailure(String label, Throwable t) {
    if (canUnwrapThrowable(t)) {
      t = conditionallyUnwrapThrowable(t);
    }
    String message = t.getMessage();
    opLogger.error(message, t);
    ProcessFailureMessage msg = new GenericProcessFailureMessage(Severity.FAIL.toString(), label,
        message, t);
    msg.setTimestamp();
    messageContainer.addFailureMessage(msg);
  }

  public void addWarning(Throwable t) {
    addWarning(defaultLabel, t);
  }

  public void addWarning(String label, Throwable t) {
    if (canUnwrapThrowable(t)) {
      t = conditionallyUnwrapThrowable(t);
    }
    String message = t.getMessage();
    opLogger.warn(message, t);
    ProcessWarningMessage msg = new GenericProcessWarningMessage(Severity.WARN.toString(), label,
        message, t);
    msg.setTimestamp();
    messageContainer.addWarningMessage(msg);
  }

  public void addInfoMessage(String message) {
    addInfoMessage(defaultLabel, message);
  }

  public void addInfoMessage(String label, String message) {
    addInfoMessage(label, message, null);
  }

  public void addInfoMessage(String message, Throwable t) {
    addInfoMessage(defaultLabel, message, t);
  }

  public void addInfoMessage(String label, String message, Throwable t) {
    if (canUnwrapThrowable(t)) {
      t = conditionallyUnwrapThrowable(t);
    }
    opLogger.info(message, t);
    ProcessInfoMessage msg = new GenericProcessInfoMessage(Severity.INFO.toString(), label, message,
        t);
    msg.setTimestamp();
    messageContainer.addInfoMessage(msg);
  }

  public void addDebugMessage(String message) {
    addDebugMessage(message, defaultLabel);
  }

  public void addDebugMessage(String label, String message) {
    addDebugMessage(label, message, null);
  }

  public void addDebugMessage(String message, Throwable t) {
    addDebugMessage(defaultLabel, message, t);
  }

  public void addDebugMessage(String label, String message, Throwable t) {
    if (canUnwrapThrowable(t)) {
      t = conditionallyUnwrapThrowable(t);
    }
    opLogger.debug(message, t);
    ProcessDebugMessage msg = new GenericProcessDebugMessage(Severity.DEBUG.toString(), label,
        message, t);
    msg.setTimestamp();
    messageContainer.addDebugMessage(msg);
  }

  /**
   * Find out if there are more than the specified number of failures.
   * <p>
   * This can be handy to know if a subprocess incurred failures, when the starting number of
   * failures is known.
   * 
   * @param cnt The number of failures to compare against.
   * @return True if this operation result has more failures than the provided number.
   */
  public boolean hasMoreThanFailureCount(int cnt) {
    return getFailureCount() > cnt;
  }

  /**
   * Get the total number of failures for this operation.
   * 
   * @return The total number of failures for this operation.
   */
  public int getFailureCount() {
    return getFailureMessages().size();
  }

  /**
   * Get the total number of warnings for this operation.
   * 
   * @return The total number of warnings for this operation.
   */
  public int getWarningCount() {
    return getWarningMessages().size();
  }

  /**
   * Get the total number of information messages for this operation.
   * 
   * @return The total number of information messages for this operation.
   */
  public int getInfoCount() {
    return getInfoMessages().size();
  }

  /**
   * Find out if there were any failures or warnings.
   * 
   * @return True if there are no failures or warnings
   */
  public boolean isFailureAndWarningFree() {
    return (!hasFailures() && !hasWarnings());
  }

  public boolean hasFailures() {
    return messageContainer.hasFailures();
  }

  public boolean hasWarnings() {
    return messageContainer.hasWarnings();
  }

  /**
   * When there's at least one failure, get the first one.
   * 
   * @return First failure or null.
   */
  public ProcessFailureMessage getFirstFailure() {
    if (hasFailures()) {
      return getFailureMessages().get(0);
    }
    return null;
  }

  /**
   * When there's at least one failure, get the last one.
   * 
   * @return Last failure or null.
   */
  public ProcessFailureMessage getLastFailure() {
    if (hasFailures()) {
      return getFailureMessages().get(getFailureMessages().size() - 1);
    }
    return null;
  }

  public List<ProcessFailureMessage> getFailureMessages() {
    return messageContainer.getFailureMessages();
  }

  public List<ProcessWarningMessage> getWarningMessages() {
    return messageContainer.getWarningMessages();
  }

  public List<ProcessInfoMessage> getInfoMessages() {
    return messageContainer.getInfoMessages();
  }

  public List<ProcessMessage> getAllMessages() {
    return messageContainer.getAllMessages();
  }

  private int getOrInitializeCount(String name) {
    if (!counters.containsKey(name)) {
      counters.put(name, 0);
    }
    return counters.get(name);
  }

  public int getCount(String name) {
    return getOrInitializeCount(name);
  }

  public void incrementCount(String name) {
    incrementCount(name, 1);
  }

  public void incrementCount(String name, int cnt) {
    if (StringUtils.isNotBlank(name)) {
      counters.put(name, getOrInitializeCount(name) + cnt);
    }
  }

  public List<String> getCounterNames() {
    List<String> names = new ArrayList<String>(counters.size());
    for (Map.Entry<String, Integer> entry : counters.entrySet()) {
      names.add(entry.getKey());
    }
    return names;
  }

  public void incrementManagedObjectCreatedCount() {
    incrementCount(COUNTER_NAME_MOS_CREATED);
  }

  public int getManagedObjectCreatedCount() {
    return getCount(COUNTER_NAME_MOS_CREATED);
  }

  public void incrementManagedObjectUpdatedCount() {
    incrementCount(COUNTER_NAME_MOS_UPDATED);
  }

  public int getManagedObjectUpdatedCount() {
    return getCount(COUNTER_NAME_MOS_UPDATED);
  }

  public void incrementNewManagedObjectsRolledBackCount() {
    incrementCount(COUNTER_NAME_NEW_MOS_ROLLED_BACK);
  }

  public int getNewManagedObjectsRolledBackCount() {
    return getCount(COUNTER_NAME_NEW_MOS_ROLLED_BACK);
  }

  public void incrementUpdatedManagedObjectsRolledBackCount() {
    incrementCount(COUNTER_NAME_UPDATED_MOS_ROLLED_BACK);
  }

  public int getUpdatedManagedObjectsRolledBackCount() {
    return getCount(COUNTER_NAME_UPDATED_MOS_ROLLED_BACK);
  }

  public void incrementWorkflowJobsCount() {
    incrementCount(COUNTER_NAME_WORKFLOW_JOBS);
  }

  public int getWorkflowJobsCount() {
    return getCount(COUNTER_NAME_WORKFLOW_JOBS);
  }

  public void incrementSkippedManagedObjectCount() {
    incrementCount(COUNTER_NAME_MOS_SKIPPED);
  }

  public int getSkippedManagedObjectCount() {
    return getCount(COUNTER_NAME_MOS_SKIPPED);
  }

  /**
   * Start a new transaction.
   * 
   * @return the transaction ID.
   */
  public int startTransaction() {
    Transaction t = new Transaction();
    transactions.add(t);
    return transactions.size() - 1;
  }

  /**
   * Get the current transaction, creating one if necessary.
   * 
   * @return current transaction
   */
  public Transaction getCurrentTransaction() {
    if (transactions.size() == 0) {
      startTransaction();
    }
    return transactions.get(transactions.size() - 1);
  }

  /**
   * Associate a <b>new</b> MO with the current transaction.
   * <p>
   * New versus updated is a very important distinction when it comes to rollback!
   * 
   * @param moId
   * @param assetName
   */
  public void addNewAsset(String moId, String assetName) {
    getCurrentTransaction().addAsset(moId);
    incrementManagedObjectCreatedCount();
  }

  /**
   * Associate an <b>updated</b> MO to the current transaction.
   * <p>
   * New versus updated is a very important distinction when it comes to rollback!
   * 
   * @param moId
   * @param assetName
   */
  public void addUpdatedAsset(String moId, String assetName) {
    getCurrentTransaction().addUpdatedAsset(moId, assetName);
    incrementManagedObjectUpdatedCount();
  }

  /**
   * Rollback all supported changes known by the current transaction.
   * 
   * @param context
   * @param user
   * @param result
   */
  public void rollbackCurrentTransaction(ExecutionContext context, User user,
      OperationResult result) {
    getCurrentTransaction().rollback(context, user, result);
  }

  /**
   * Get all transactions associated with this operation. Tranactions provide lists of new and
   * updated MOs, as well as provide the ability to rollback those edits.
   * 
   * @return Zero or more transactions.
   */
  public List<Transaction> getTransactions() {
    return transactions;
  }

  /**
   * @return A list of <code>ContentAssembly</code> instances destroyed by this operation.
   */
  public List<ContentAssembly> getDestroyedContentAssemblies() {
    return destroyedContentAssemblyList;
  }

  /**
   * @param destroyedContentAssemblyList List of <code>ContentAssembly</code> instances destroyed by
   *        this operation.
   */
  public void setDestroyedContentAssemblies(List<ContentAssembly> destroyedContentAssemblyList) {
    this.destroyedContentAssemblyList = destroyedContentAssemblyList;
  }

  /**
   * @return A list of <code>ManagedObject</code> instances destroyed by this operation.
   */
  public List<ManagedObject> getDestroyedManagedObjects() {
    return destroyedManagedObjectList;
  }

  /**
   * @param destroyedManagedObjectList List of <code>ManagedObject</code> instances destroyed by
   *        this operation.
   */
  public void setDestroyedManagedObjects(List<ManagedObject> destroyedManagedObjectList) {
    this.destroyedManagedObjectList = destroyedManagedObjectList;
  }

  /**
   * @return A list of role names processed in this operation.
   */
  public List<String> getProcessedRoleNamesList() {
    return processedRoleNamesList;
  }

  /**
   * @param delimiter Delimiter to use to separate list members if there is more than one
   * @return A string representation of the members of the processedRoleNamesList list object.
   */
  public String getProcessedRoleNamesList(String delimiter) {
    return StringUtils.join(processedRoleNamesList, delimiter);
  }

  /**
   * @param processedRoleNamesList List of role names processed by this operation.
   */
  public void setProcessedRoleNamesList(List<String> processedRoleNamesList) {
    this.processedRoleNamesList = processedRoleNamesList;
  }

  /**
   * Set the operation's payload, and content type thereof.
   * 
   * @param payload
   * @param contentType
   */
  public void setPayload(String payload, String contentType) {
    this.payload = payload;
    this.payloadContentType = contentType;
  }

  /**
   * @return the operation's payload, if it has one.
   */
  public String getPayload() {
    return payload;
  }

  /**
   * @return True if this operation provided a payload.
   */
  public boolean hasPayload() {
    return StringUtils.isNotBlank(payload);
  }

  /**
   * @return The payload's content type.
   */
  public String getPayloadContentType() {
    return payloadContentType;
  }

  /**
   * Copies some of the sub result over to this one.
   * 
   * @param subResult The sub-result to add to this result.
   */
  public void addSubResult(OperationResult subResult) {
    if (subResult != null) {
      // All messages
      this.messageContainer.addAll(subResult.messageContainer);

      // Destroyed objects
      this.destroyedContentAssemblyList.addAll(subResult.getDestroyedContentAssemblies());
      this.destroyedManagedObjectList.addAll(subResult.getDestroyedManagedObjects());
    }
  }

  /**
   * @return An "executive summary" of the operation which was introduced as part of email subjects.
   */
  public String getExecutiveSummary() {
    if (hasFailures()) {
      return new StringBuilder("Error! (").append(getFailureCount()).append(")").toString();
    } else if (hasWarnings()) {
      return new StringBuilder("Warning (").append(getWarningCount()).append(")").toString();
    } else {
      return "Successful";
    }
  }

  /**
   * @return The messages as an HTML-formatted report.
   */
  public String getHtmlFormattedMessages() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    HtmlFormattingOperationResultVisitor visitor = new HtmlFormattingOperationResultVisitor(writer);
    try {
      visitor.visit(this);
    } catch (Exception e) {
      writer.println("Unexpected exception formatting message report: " + e.getMessage());
      e.printStackTrace(writer);
    }
    return stringWriter.toString();
  }

  /**
   * Find out if {@link #conditionallyUnwrapThrowable(Throwable)} can unwrap the provided throwable.
   * 
   * @param t
   * @return True if the throwable can/should be unwrapped.
   */
  public boolean canUnwrapThrowable(Throwable t) {
    return t instanceof InvocationTargetException;
  }

  /**
   * Unwrap the given throwable when it is needlessly wrapped. The DeltaXML Merge integration wraps
   * various exceptions with InvocationTargetException. This method is able to get to the underlying
   * and insightful exception.
   * 
   * @param t
   * @return Either the provided throwable, or one that it wraps.
   */
  public Throwable conditionallyUnwrapThrowable(Throwable t) {
    while (t instanceof InvocationTargetException) {
      t = ((InvocationTargetException) t).getTargetException();
    }
    return t;
  }

  /**
   * When the given throwable isn't an RSuiteException, wrap it in one.
   * <p>
   * TODO: Revisit if this practice is necessary.
   * 
   * @param t
   * @param wrapperCode
   * @param wrapperMessageKey
   * @param wrapperMessageArgs
   * @return An RSuiteException
   */
  public RSuiteException conditionallyWrapThrowable(Throwable t, int wrapperCode,
      String wrapperMessageKey, Object... wrapperMessageArgs) {

    t = conditionallyUnwrapThrowable(t);

    RSuiteException re;
    if (t instanceof RSuiteException) {
      re = (RSuiteException) t;
    } else {
      re = new RSuiteException(wrapperCode, ProjectMessageResource.getMessageText(wrapperMessageKey,
          wrapperMessageArgs), t);
    }
    return re;
  }

}
