package org.astd.rsuite.workflow.actions;

import org.astd.reallysi.rsuite.api.workflow.StringUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkflowJobContext
  extends BaseWorkflowObject
{
  private static final long serialVersionUID = 1L;
  private static final String FAILED = "failed";
  private static final String WORK = "work";
  private static final String TEMP = "temp";
   private static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
  


   private String baseFolderPath = null;
   private String module = null;
   private String sourceFilePath = "";
   private String hotFolderPath = "";
   private String workFolderPath = "";
   private String tempFolderPath = "";
   private String failedFolderPath = "";
  
  private String timestamp;
  
   private String failedFile = "";
  





  public WorkflowJobContext(String baseFolderPath)
  {
     this.baseFolderPath = baseFolderPath;
  }
  




  private void init()
  {
     timestamp = timestampFormat.format(new Date());
     tempFolderPath = generateFolderPathRelativeToBase("temp");
     failedFolderPath = generateFolderPathRelativeToBase("failed");
     workFolderPath = generateFolderPathRelativeToBase("work");
  }
  




  public String toString()
  {
     StringBuilder buf = new StringBuilder();
     buf.append(getClass().getSimpleName());
     buf.append("[module=");
     buf.append(module);
     buf.append(",work=");
     buf.append(workFolderPath);
     buf.append(",failed=");
     buf.append(failedFolderPath);
     buf.append("]");
     return buf.toString();
  }
  





  public String getStartTimestamp()
  {
     return timestamp;
  }
  







  public String getModule()
  {
     return module;
  }
  







  public void setModule(String module)
  {
     this.module = module;
  }
  






  public String getSourceFilePath()
  {
     return sourceFilePath;
  }
  


  /**
   * @deprecated
   */
  public String getSourceFile()
  {
     return getSourceFilePath();
  }
  




  public void setSourceFilePath(String sourceFilePath)
  {
     this.sourceFilePath = sourceFilePath;
  }
  






  public String getHotFolderPath()
  {
     return hotFolderPath;
  }
  





  public void setHotFolderPath(String hotFolderPath)
  {
     this.hotFolderPath = hotFolderPath;
  }
  





  public String getWorkFolderPath()
  {
     if (StringUtil.isNullOrEmptyOrSpace(workFolderPath))
       init();
     return workFolderPath;
  }
  

  /**
   * @deprecated
   */
  public String getWorkfolder()
  {
     return getWorkFolderPath();
  }
  





  public String getFailedFolderPath()
  {
     if (StringUtil.isNullOrEmptyOrSpace(failedFolderPath))
       init();
     return failedFolderPath;
  }
  


  /**
   * @deprecated
   */
  public String getFailedfolder()
  {
     return getFailedFolderPath();
  }
  


  public String getFailedFile()
  {
     return failedFile;
  }
  
  public void setFailedFile(String failedFile)
  {
     this.failedFile = failedFile;
  }
  
  public void setFailedFile(File failedFile)
  {
     this.failedFile = failedFile.getAbsolutePath();
  }
  





  public String getTempFolderPath()
  {
     if (StringUtil.isNullOrEmptyOrSpace(tempFolderPath))
       init();
     return tempFolderPath;
  }
  


  /**
   * @deprecated
   */
  public String getTemp()
  {
     return getTempFolderPath();
  }
  






  private String generateFolderPathRelativeToBase(String pathComponent)
  {
     return baseFolderPath + File.separator + timestamp + File.separator + (getModule() == null ? "" : new StringBuilder().append(getModule()).append(File.separator).toString()) + pathComponent;
  }
  
  public void destroy() {}
}

