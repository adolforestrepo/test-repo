package com.reallysi.rsuite.webservice;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectLoader;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiDefinition;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.service.ManagedObjectService;

public class ImportDitaInterchangePackageWebService
  implements RemoteApiHandler
{
  /**
	 *
	 */
	public class ZipNodeTree {

	private ZipFile zipFile;
	private List<ZipEntry> roots = new ArrayList<ZipEntry>();

	/**
	 * @param zipFile
	 */
	public ZipNodeTree(ZipFile zipFile) {
		this.zipFile = zipFile;
	}

	/**
	 * @param entry
	 */
	public void addRoot(ZipEntry entry) {
		this.roots .add(entry);		
	}

	}

private static Log log = LogFactory.getLog(ImportDitaInterchangePackageWebService.class);
  
  private RemoteApiDefinition def = null;

  /**
   * List of extensions to treat as XML in addition
   * to RSuite system default (xml, nxml, etc.).
   */
public String treatAsXml = "";

/**
 * List of extensions to never treat as XML in addition
 * to Rsuite system default.
 */
public String neverTreatAsXml = ""; // Use system default.

private static final FileFilter visibleOnlyFileFilter;

static {
	  String[] ignoredPrefixes = {".", "__", "~"};
	  String[] ignoredSuffixes = {".bak", ".scc"};

	  IOFileFilter filter = getCvsSvnAwareFileFilter();
	  filter = FileFilterUtils.andFileFilter(filter, HiddenFileFilter.VISIBLE);
	  // Ignore __MACOS files and similar:
	  filter = FileFilterUtils.andFileFilter(filter, 
			  FileFilterUtils.notFileFilter(
					  new PrefixFileFilter(ignoredPrefixes)));
	  // Ignore VSS control files:
	  filter = FileFilterUtils.andFileFilter(filter, 
			  FileFilterUtils.notFileFilter(
					  new SuffixFileFilter(ignoredSuffixes)));
	  visibleOnlyFileFilter = (FileFilter)filter;
}
  
  
  /**
   * 
   */
  public void initialize(
    RemoteApiDefinition def)
  {
    log.info("initalize: enter");
    
    this.def = def;
    String value;
    value = (String)this.def.getPropertyValue("treatAsXml");
    this.treatAsXml = (value != null?value:"");
    value = (String)this.def.getPropertyValue("neverTreatAsXml");
    this.neverTreatAsXml = (value != null?value:"");
    
    // TODO: Add properties for configuring file filters.
    
  }
  

  /**
   * 
   */
  public RemoteApiResult execute(
    RemoteApiExecutionContext context,
    CallArgumentList args)
      throws RSuiteException
  {
    
    CallArgument fileArg = args.getFirst("zipFile");
    if (fileArg == null)
      return new MessageDialogResult("Load from DITA Package", "Error: no file supplied");
    
    FileItem zipFileItem = fileArg.getValueAsFileItem();
    if (zipFileItem == null)
      return new MessageDialogResult("Load from DITA Package", "Error: file argument was not a file");
    
    String moid = args.getFirstValue("rsuiteId");
    
    if (moid == null)
      return new MessageDialogResult("Load from DITA Package", "Error: no managed object was selected");
    
    return doLoad(context, moid, zipFileItem);
  }
  
  
  /**
   * 
   */
  protected RemoteApiResult doLoad
    (RemoteApiExecutionContext context,
      String caId,
      FileItem inZipFile)
  {
    log.info("doLoad(): starting at object "+caId+", processing zip file: "+inZipFile.getName());
    
    // Now load the upzipped files into the repository:
    
	User user = context.getSession().getUser();

    ContentAssembly ca;
	try {
		ca = context.getContentAssemblyService().getContentAssembly(user, caId);
	} catch (RSuiteException e) {
	      return logAndReportErrror(e, "unable to get content assembly with ID [" + caId + "]");
	}

	ZipFile zipFile = null;
	// FIXME: Have to understand how to deal with file item in this context.
//	try {
//		zipFile = new ZipFile();
//	} catch (FileNotFoundException e) {
//	    return logAndReportErrror(e, "Input zip file not found: " + e.getMessage());
//	} catch (ZipException e) {
//	    return logAndReportErrror(e, "ZipException: " + e.getMessage());
//	} catch (IOException e) {
//	    return logAndReportErrror(e, "IOException: " + e.getMessage());
//	}
	
//	try {
//		ZipEntry entry = getDxpPackageRootMap(zipFile);
//	} catch (DitaDxpException e) {
//	    return logAndReportErrror(e, "DITA DXP Exception processing Zip file: " + e.getMessage());
//	}

	
   
    StringBuilder msg = new StringBuilder();
    msg.append("Load from Zip: Success: Loaded following files from the zip file:<br/>\n");
    
    return new MessageDialogResult
                 ("Load from Zip", 
                  msg.toString(), "500");
  }





protected void reportFileList(List<File> loadedFiles, StringBuilder msg) {
	if (loadedFiles.size() < 10) {
		for (File file : loadedFiles) {
	    	msg.append("<br/>\n")
	    	.append(file.getName())
	    	;
	    }
	} else {
		msg.append("<br/>\nLoaded ")
		.append(loadedFiles.size())
		.append(" files.");
	}
}


protected RemoteApiResult logAndReportErrror(Exception e, String msg) {
	log.warn(msg, e);
	  
	  return new MessageDialogResult("Load from Zip", "Error, " + msg, "500");
}
  
  
  
  /**
 * @param context
 * @param rootDir
 * @param loadedFiles 
 * @throws RSuiteException 
 */
private void loadDirectoryToCa(RemoteApiExecutionContext context,
		File rootDir, ContentAssemblyNodeContainer ca, List<File> loadedFiles) throws RSuiteException {
	  User user = context.getSession().getUser();
	  
	  ManagedObjectLoader loader = context.getManagedObjectService().constructManagedObjectLoader(user);
	  
	  ObjectAttachOptions options = new ObjectAttachOptions();
	  ContentAssembly caContext = context.getContentAssemblyService().getContentAssembly(user, ca.getId());
	  loader.setContentAssemblyContext(caContext, options);

	  ContentAssemblyCreateOptions createOptions = new ContentAssemblyCreateOptions();
	  createOptions.setSilentIfExists(true);
	  
	  File[] files = rootDir.listFiles((FileFilter)visibleOnlyFileFilter);
	  List<ManagedObject> updatedMos = new ArrayList<ManagedObject>();
	  for (File file : files) {
		  if (file.isDirectory()) {
			  ContentAssemblyNodeContainer newCa = context.getContentAssemblyService().
			    createCANode(user, ca.getId(), file.getName(), createOptions);
			  loadDirectoryToCa(context, file, newCa, loadedFiles);			  
		  } else {
			  try {
				loadFileToCa(context, loader, updatedMos, ca, file, loadedFiles);
			} catch (Exception e) {
				throw new RSuiteException(0, e.getClass().getSimpleName() + 
						" Exception loading file \"" + file.getName() + "\" to CA [" + ca.getId() + "] " + ca.getDisplayName(), e);
			}
		  }
	  }
	  
	  // Attach the updated MOs:
	  log.info("loadDirectoryToCaAttaching updated MOs...");
	  context.getContentAssemblyService().attach(user, ca.getId(), updatedMos, options);

	  // Commit the new MOs to the repository.
	  log.info("Committing new MOs...");
	  loader.commit();
}


/**
 * @param context
 * @param file
 * @param ca
 * @param loadedFiles
 * @throws IOException 
 * @throws RSuiteException 
 */
private ManagedObject loadFileToCa(RemoteApiExecutionContext context, 
		ManagedObjectLoader loader, 
		List<ManagedObject> updatedMos, 
		ContentAssemblyNodeContainer ca, 
		File file, 
		List<File> loadedFiles) throws Exception {
	  User user = context.getSession().getUser();
	  ManagedObject mo = getExistingMoForFile(context, user, file);
	  ManagedObjectService moService = context.getManagedObjectService();
	  
	  ObjectSource src = null;
	  if (mo != null) {
		  if (mo.isCheckedoutButNotByUser(user)) {
			  log.warn("loadFileToCa(): Managed object [" + mo.getId() + "] " + mo.getDisplayName() + " is checked out by another user. Cannot update it.");				
		  } else {
			  boolean inCA = ca.hasChild(mo);
			  ObjectUpdateOptions options = new ObjectUpdateOptions();
			  options.setExternalFileName(file.getName());
			  options.setDisplayName(file.getName());
			  moService.checkOut(user, mo.getId());
			  src = new NonXmlObjectSource(file);			 
			  log.info("loadFileToCa: Updating MO [" + mo.getId() + "] with file \"" + file.getName() + "\"...");
			  moService.update(user, mo.getId(), src, options);
			  moService.checkIn(user, mo.getId(), VersionType.MINOR, "Updated via Zip load", false);			  
			  if (!inCA)
				  updatedMos.add(mo);
			  	  log.info("loadFileToCa: MO updated.");			 
				  loadedFiles.add(file);
		  }
	  } else {
		  if (treatAsXml(context, file.getName())) {
			  log.info("loadFileToCa(): Ignoring XML file \"" + file.getName() + "\"");
		  } else {
			  src = new NonXmlObjectSource(file);
		  }
		  String[] aliases = {file.getName()};
		  String[] collections = {};
		  ObjectInsertOptions options = new ObjectInsertOptions(file.getName(), aliases, collections, false);
		  // NOTE: This setting of the display name is a workaround for a bug (COR-1380). The 
		  // external filename should be used as the display name if display name is not specified.
		  options.setDisplayName(file.getName());
		  log.info("loadFileToCa: Loading file \"" + file.getName() + "\" as a new MO...");
		  loader.load(FileUtils.readFileToByteArray(file), options);
		  log.info("loadFileToCa: File loaded");
		  loadedFiles.add(file);
	  }
	  return mo;
}


/**
 * Looks for an MO with an alias that matches the filename of the
 * incoming file. Note that this effectively requires that
 * all MOs have unique aliases.
 * @param context
 * @param user
 * @param ca CA to look in for matches (anticipates future functionality
 * where scope of alias uniqueness can be limited to a subtree of the
 * browse tree).
 * @param file
 * @return
 * @throws RSuiteException 
 */
private ManagedObject getExistingMoForFile(RemoteApiExecutionContext context,
		User user, File file) throws RSuiteException {
	ManagedObject mo = null;
	String alias = file.getName();
	ManagedObjectService moSvc = context.getManagedObjectService();
	List<ManagedObject> mosWithAlias = moSvc.getObjectsByAlias(user, alias);
	if (mosWithAlias.size() > 1) {
		String idList = "";
		for (ManagedObject item : mosWithAlias) {
			idList += " " + item.getId() + ", ";
		}
		log.warn("getExistingMoForFile(): Found " + mosWithAlias.size() + " MOs with alias \"" + alias + "\": " + idList + ". Picking first in list.");
		mo = mosWithAlias.get(0);
	}
	if (mosWithAlias.size() > 0) {
		log.info("getExistingMoForFile(): Found existing MO with alias \"" + alias + "\". ");
		mo = mosWithAlias.get(0);
	} else {
		log.info("getExistingMoForFile(): No MO with alias \"" + alias + "\" fail.");
	}
	return mo; 
}


protected static IOFileFilter getCvsSvnAwareFileFilter() {
	IOFileFilter filter = FileFilterUtils.trueFileFilter();
	  filter = FileFilterUtils.
	    andFileFilter(
	    		FileFilterUtils.makeCVSAware(filter),
	    		FileFilterUtils.makeSVNAware(filter));
	return filter;
}

/**
 * Given a filename, determines if it should be treated as XML or not.
 * <p>FIXME: Move this to a public utility class in the public API.
 * @param context
 * @param fileName Filename to check.
 * @return
 */
protected boolean treatAsXml(ExecutionContext context, String fileName) {
	  String inExtension = FilenameUtils.getExtension(fileName);
	  
    String xml_file_ext = context.getRSuiteServerConfiguration().getTreatAsXmlFileExtensionsAsCommaDelimitedString();
    xml_file_ext += this.treatAsXml;
      	      
	  String alwaysTreatAsXml = xml_file_ext;
	  String neverTreatAsXml = this.neverTreatAsXml;
	  
	  // Never treat takes precedence over always treat.
	  String[] never = neverTreatAsXml.split(",\\s*");
	  for (String candExt : never) {
		  if (candExt.startsWith("."))
			  candExt = candExt.substring(1);
		  if (candExt.equals(inExtension))
			  return false;
	  }
	  
	  String[] always = alwaysTreatAsXml.split(",\\s*");
	  for (String candExt : always) {
		  if (candExt.startsWith("."))
			  candExt = candExt.substring(1);
		  if (candExt.equals(inExtension))
			  return true;
	  }
	  
    return false;
  }




}
