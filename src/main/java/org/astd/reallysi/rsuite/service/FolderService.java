package org.astd.reallysi.rsuite.service;

import com.reallysi.rsuite.api.Folder;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.service.SystemComponent;

public abstract interface FolderService
  extends SystemComponent
{
  public abstract Folder createFolder(User paramUser, String paramString1, String paramString2, boolean paramBoolean, String paramString3)
    throws RSuiteException;
  
  public abstract Folder createFolder(User paramUser, String paramString1, String paramString2, boolean paramBoolean1, String paramString3, boolean paramBoolean2)
    throws RSuiteException;
  
  public abstract Folder createFolder(User paramUser, String paramString1, String paramString2, String paramString3)
    throws RSuiteException;
  
  public abstract boolean deleteFolder(User paramUser, String paramString, boolean paramBoolean)
    throws RSuiteException;
  
  public abstract String folderHasDescendants(User paramUser, String paramString)
    throws RSuiteException;
  
  public abstract Folder getFolder(User paramUser, String paramString)
    throws RSuiteException;
  
  public abstract boolean hasFolder(User paramUser, String paramString)
    throws RSuiteException;
  
  public abstract Folder getRootFolder(User paramUser)
    throws RSuiteException;
  
  public abstract boolean hasFolder(String paramString)
    throws RSuiteException;
  
  public abstract boolean isEmptyFolder(String paramString)
    throws RSuiteException;
  
  public abstract void renameFolder(User paramUser, String paramString1, String paramString2)
    throws RSuiteException;
}