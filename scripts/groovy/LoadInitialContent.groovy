/**
 * Script to import a directory tree into RSuite, recreating the directory 
 * structure in RSuite using CAs.
 *
 * Top-level directories are skipped if they already exist in RSuite.
 *
 * Use the "excludeTiers" property in the RSuite node type files to 
 * conditionally include by tier.  The value of this property may be one or
 * more tier names, such as "prod" or "qa prod".  Separate multiple values 
 * with a space.  Pass the tier name into this script, using the "tier"
 * parameter.  The scripts defaults to "prod", to be safe.
 *
 * rsuite-admin.bat run -U ? -P ? -s LoadInitialContent.groovy -Dtier=qa
 *
 */

import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

def projectDir = new File(scriptFile.absolutePath).parentFile.parentFile.parentFile
def sampleDataDir = new File(projectDir, 'sample_data')
def initialBrowseTreeDir = new File(sampleDataDir, 'initial_browse_tree')


ignoredDirectoriesRegex = ~/(^\.svn)|(^CVS)/
ignoredFilesRegex = ~/^_rsuiteNodeType|^\.|^__MAC|^~/

xmlExtensions = [".xml", ".nxml", ".dita", ".ditamap"] // Modify this list as needed.

/**
 * Returns true if the file is an XML file, otherwise false
 * 
 * 
 */
def fileIsXml(file) {
	 def isXml = false
	 xmlExtensions.each {
		 if (file.getName().endsWith(it))
		 	isXml = true
	 }
	 return isXml
}

def getDirectoryNodeType(dirFile) {
  def nodeType = "root" // Assume initial folder is root, meaning it should not itself be reflected in the browse tree.
  
  def nodeTypeFile = getRSuiteNodeTypeFile(dirFile)
    
  if (nodeTypeFile != null) {
	  matcher = ( nodeTypeFile.name =~ /_rsuiteNodeType_([^\.]+)/)
	  nodeType = matcher[0][1]
	  println " + [INFO] Found node type [" + nodeType + "]"
  } else {
  	  println " + [WARN] No node type file in directory ${dirFile}, using 'ca'"
	  nodeType = "ca"
  }
  
  return nodeType
}

def getRSuiteNodeTypeFile(File dirFile) {
  def resultFile = null
  def foundNodeType = false

  dirFile.eachFile {  file ->
       if ( file.name =~ /_rsuiteNodeType_([^\.]+)/) {
          if (foundNodeType) {
             throw new RuntimeException("ERROR: Found multiple _rsuiteNodeType files in directory ${file.name}. You can have at most one such file.")
          }
          resultFile = file
          foundNodeType = true
      }
  }
  
  return resultFile
}

/**
 * Load and return the properties associated to the specified directory.
 */
def getProperties(dirFile) {
    File dataFile = getRSuiteNodeTypeFile(dirFile)
    if (dataFile == null) {
        println "\n + [INFO] getProperties(): No directory configuration file for directory ${dirFile.getAbsolutePath()}"
        return
    }
    if (dataFile.length() == 0) {
        return
    }
    try {
        return new ConfigSlurper().parse(dataFile.toURL())
    } catch (e) {
        println "\n + [INFO] Failed to construct properties from data file ${dirFile.name}/${dataFile.name}: ${e}"
        return
    }
}

def setPermissions(id, dirFile) {
	// println "\n + [DEBUG] setPermissions(): Starting, dirFile=${dirFile}"
    def props = getProperties(dirFile)
	if (props == null) {
		println "\n - [WARN] setPermissions(): No properties in directory configuration file ${dirFile.getName()}"
		return
	}
	def acl = props.ACL
	if (acl == null) {
		println "\n - [WARN] setPermissions(): no ACL property in directory configuration file ${dirFile.getName()}"
		return
	}
	def roles = acl.roles
	if (roles == null) {
		println "\n - [WARN] setPermissions(): no ACL.roles property in directory configuration file ${dirFile.getName()}"
		return
	}
	roles.each { roleName, permissions ->
		println "\n + [INFO] Setting permissions for role ${roleName} to \"${permissions}\"..."
		def effectiveRoleName = ("any" == roleName ? "*" : roleName)
		rsuite.setACE(id, effectiveRoleName, permissions)
		
	}
		
}

def setLayeredMetadata(id, dirFile) {
    def props = getProperties(dirFile)
	if (props != null) {
		if (props.lmd != null) {
			println "\n + [INFO] Setting LMD for MO ${id}..."
			props.lmd.each { key, value ->
				println " + [INFO] Setting LMD '${key}' to '${value}'"
				try {
					rsuite.addMetaData(id, key, value)
				} catch(Exception e) {
					println " + [WARN] Exception Setting LMD '${key}' to '${value}'. Error Message: ${e.getMessage()}"
				}
			}
			println " + [INFO] LMD set"
		}
	}
}

/**
 * Given an initial directory, use it to construct a new browse
 * tree under the specified parent folder. A folder of "/" indicates
 * the RSuite browse tree root. Folder is now a CA of type='folder'.
 */
def createBrowseTreeFromFolder(dirFile, parentFolder, parentId, level, skipIfExists, tier) {
   // println " + [DEBUG] createBrowseTreeFromFolder(): Starting..."
   
   println " + [INFO] Creating browse tree from directory ${dirFile.name}..."

  def nodeType = getDirectoryNodeType(dirFile)
  // println " + [DEBUG] createBrowseTreeFromFolder(): Node type is '${nodeType}'"

  switch (nodeType) {
    case "ca":
       throw new RuntimeException("Directory ${dirFile.name} is marked as a CA of type 'ca'. " +
                                  "You cannot have a non-root CA at the root level. Only 'root' is supported here.") 
    case "dynamicCa":
       throw new RuntimeException("Directory ${dirFile.name} is marked as a dynamnic CA. " +
                                  "You cannot have a dynamic node at the root level. It must be within a content assembly") 
    case "folder":
      createContentAssemblyFromDirectory(dirFile, parentId, level, nodeType)
      break
    case "root":      
    default:
      setPermissions(parentId,dirFile)
      processFilesInFolderDir(dirFile, parentId, level++, skipIfExists, tier)
  }  
  
   println " + [INFO] Done"
}

def processFilesInFolderDir(dirFile, parentId, level, skipIfExists, tier) {
  // println " + [DEBUG] processFilesInFolderDir(): Starting, parentFolder='${parentFolder}'..."
  dirFile.eachFile {  file ->
       if (file.isDirectory()) {
          if ((file.name =~ ignoredDirectoriesRegex)) {
            // do not process
          } else if (skipIfExists && isDirectoryAlreadyLoadedIntoRSuite(file, level)) {
            println "[INFO] Skipping the \"" + file.name + "\" directory, with level " + level + ", as it already exists in RSuite. This behavior is configurable."
          } else if (!isOkForThisRSuiteInstance(tier, file)) {
            println "[INFO] Skipping the \"" + file.name + "\" directory as it is not configured for the \"" + tier + "\" RSuite instance."
          } else {
            createBrowseTreeNodeFromFolderChild(file, parentId, level)
          }
       } else {
         if (!(file.name =~ ignoredFilesRegex)) {
           println " - [WARNING] Skipping file ${file.name} with a parent directory of node type folder"
         }
       } 
  }
}

/**
 * Create a CA of type folder, or another type.  The function name
 * lost some of its meaning between RSuite 3.6 and 3.7. 
 */
def createBrowseTreeNodeFromFolderChild(dirFile, parentId, level) {

  def nodeType = getDirectoryNodeType(dirFile)

  switch (nodeType) {
    case "dynamicCa":       
       // special processing if we're dealing with a Smart Folder aka Dynamic Content Assembly
       createDynamicAssemblyNodeFromDirectory(dirFile, parentId, level)
       break
    case "root":
    	// Script was already skipping root here, so continuing
    case ~/[a-zA-Z]+/:
    	// This case handles any node type, such as ca, folder, and custom canode types
       createContentAssemblyFromDirectory(dirFile, parentId, level, nodeType)
       break   
    default:
       processFilesInAssemblyDir(dirFile, parentId, level, nodeType)
  }
}

def createDynamicAssemblyNodeFromDirectory(dirFile, parentMoid, level) {
  // println " + [DEBUG] createDynamicAssemblyNodeFromDirectory(): Starting...'"
  print " + [INFO] "; (0..(level*2)).each { print " "} 
  
  println "Creating dynamic assembly node ${dirFile.name}..."

  def props = getProperties(dirFile)
  if (props == null) {
     throw new RuntimeException("The RSuite node type file, ${file.absolutePath}, is not defined.")
  }
  def query = props.query
  if ("" == query) {
     throw new RuntimeException("No 'query' property in RSuite node type file ${file.absolutePath}. Properties are: ${props}")
  }
  
  rsuite.createDynamicContentAssemblyNode(parentMoid, dirFile.name, query)
}

/**
 * Create nodes from children of assembly-type directories. Assemblies
 * can contain CAs, dynamic CAs, and files of any type.
 */
def createBrowseTreeNodeFromAssemblyChild(dirFile, parentMoid, level, parentCaType) {
  def nodeType = getDirectoryNodeType(dirFile)
  // println " + [DEBUG] createBrowseTreeNodeFromAssemblyChild(): Node type is '${nodeType}'"

  switch (nodeType) {
    case "root":
       throw new RuntimeException("Directory ${dirFile.name} is marked as the root folder type " +
                                  "but is not the root directory") 
    case "dynamicCa":       
       createDynamicAssemblyNodeFromDirectory(dirFile, parentMoid, level)
       break
    case "caNode":
       createContentAssemblyNodeFromDirectory(dirFile, parentMoid, level, parentCaType)
       break
    default:
       // handles built-in and custom CA types
       createContentAssemblyFromDirectory(dirFile, parentMoid, level, nodeType)
       break
  }  
	println " + [INFO] Processed a \"" + nodeType + "\"."
	    
}  

def createContentAssemblyFromDirectory(dirFile, folderId, level, caType) {
  // println " + [DEBUG] createContentAssemblyFromDirectory(): Starting..."
  print "\n + [INFO] ";(0..(level*2)).each { print " "} 
  print "Creating content assembly: ${dirFile.getName()}..."
  
  // HACK: We don't want to continuously re-process some sample data
    def shouldSkip = doesAssemblyExist(folderId, dirFile.name)
    if (shouldSkip) {
        println " + [INFO] Electing not to process '" + dirFile.name + "' as it already exists."
    } else {
  	  def caOptions = new Options()
	  caOptions.add("type", caType)
	
	  /*
	   * COR-2900: As of 3.7.1 RC06, submitting true here tells RSuite to use
	   * an existing CA. False will create another one with the same name and
	   * parent.
	   */
	  caOptions.add("silentIfExists", "true")
	
	  def result = rsuite.createContentAssembly(folderId, dirFile.name, caOptions)
	  def moid = result["moid"]
	  setPermissions(moid, dirFile)
	  setLayeredMetadata(moid, dirFile)  
	  println "\n + [INFO] Content assembly created as moid [${moid}]"
	
	  processFilesInAssemblyDir(dirFile, moid, level++, caType)
    }
}
// Find out if an assembly exists by parent MO ID and child assembly name.
def doesAssemblyExist(parentMoid, childDisplayName) {
    def mapList = rsuite.listContentAssemblyInfos(parentMoid, childDisplayName)
    return mapList.size() > 0
}

def createContentAssemblyNodeFromDirectory(dirFile, parentMoid, level, parentCaType) {
  // println " + [DEBUG] createContentAssemblyNodeFromDirectory(): Starting..."
   print "\n + [INFO] ";(0..(level*2)).each { print " "} 
   print "Creating assembly node: ${dirFile.getName()}..."

  def result = rsuite.createContentAssemblyNode(parentMoid, dirFile.name)
  def moid = result["moid"]
  setPermissions(moid,dirFile)
//println "\n + [DEBUG] calling setLayeredMetadata()..."
  setLayeredMetadata(moid, dirFile)  
  
  println "assembly node created as moid [${moid}]"

  processFilesInAssemblyDir(dirFile, moid, level++, parentCaType)
}

def processFilesInAssemblyDir(dirFile, parentMoid, level, parentCaType) {
  // println " + [DEBUG] processFilesInAssemblyDir(): Starting, parentMoid='${parentMoid}'..."
  
  dirFile.eachFile {  file ->
       if (file.isDirectory()) {
          if (!(file.name =~ ignoredDirectoriesRegex)) {
			  createBrowseTreeNodeFromAssemblyChild(file, parentMoid, level++, parentCaType)
          }
       } else {
         if (!(file.name =~ ignoredFilesRegex)) {
            loadFile(file, parentMoid, level++)
         } else {
            // println " + [DEBUG] processFilesInAssemblyDir(): skipping file"      
         }
       } 
  }
}

def loadFile(file, parentMoid, level) {
   print "\n + [INFO] ";(0..(level*2)).each { print " "} 
   println "Loading file: ${file.getName()}..."

   def isXml = fileIsXml(file);
  
   try {
       if (isXml) {
           println " + [DEBUG]   File is an XML document.";
           result = rsuite.loadXmlFromFileAndAttach(file, true, parentMoid);
       } else {
           println " + [DEBUG]   File is not XML.";
           // FIXME: Set permissions correctly
           result = rsuite.loadNonXmlFromFileAndAttach(file, null, [any:"list,view,edit,delete,copy,reuse"], parentMoid);
       }
     def moid = result.getEntryValue("moid")
     def moInfo = rsuite.getInfo( moid);

     def children = rsuite.listContentAssemblyChildrenInfos( parentMoid);
     children.find { 
        if ( it[ "displayName"] == moInfo[ "displayName"]) {
          println "duplicate object. deleting"
          rsuite.checkOut( moid);
          rsuite.destroyObject( moid);
          moid = null;
          return true;
        }
     }
     
     if ( moid != null) {
       println "load as moid [${moid}] "
       rsuite.addResourceToAssembly(parentMoid, moid); 
     }
   } catch (Exception e) {
     println "Exception loading file: ${e.getMessage()}";
   }
}

/** A way to avoid reloading a directory into the root, when it's already 
 * present in RSuite.  It ain't purty, but it meets our current need.
 * Presently only checks when level is 0.  Doesn't compare contents of
 * directory to RSuite.  Just sees if dir.name is in the root.
 */
def isDirectoryAlreadyLoadedIntoRSuite(dir, level) {
  if (level == 0) {
     return doesObjectExist("/" + dir.name)
  }
  return false
}

/**
 * Find out if this directory should be loaded into this RSuite instance
 */
def isOkForThisRSuiteInstance(tier, dirFile) {
    def props = getProperties(dirFile)
    if (props != null && !props.excludedTiers.isEmpty()) {
        return !props.excludedTiers.tokenize().contains(tier)
    }
    return true // default
}

/** Find out if an object exists in RSuite.
 */
def doesObjectExist(path) {
  println "checking to see if " + path + " already exists"
  def preExistingObject = rsuite.findObjectForPath(path)
  
  if (preExistingObject != null) printRSuiteMap(preExistingObject)
  else println "map is null object does not exist"
  
  if ((preExistingObject == null) || (preExistingObject["moid"] == null) || ("" == preExistingObject["moid"])) {
    println "returning false"
    return false
  } else {
    println "returning true"
    return true
  }
}

/**
 * Print the contents of an RSuite map to the console.
 *
 * @param map
 */
def printRSuiteMap(map) {
    println "Details:"
    def es = map.convertToMap().entrySet()
    es.each {
       println "\t" + it.key + ": \"" + it.value + "\""
    }
}


rsuite.login()

// Determine the tier this is being executed in.
if (!binding.variables.containsKey("tier")) {
    println "[INFO] Tier was not specified; defaulting to 'prod'"
    tier = "prod"
} else {
    println "[INFO] Tier: '" + tier + "'"
}

def dirToImport = initialBrowseTreeDir
def targetFolder = "/"
def rootId = "4" //default for new installs in appa, only used as fallback.

if (!dirToImport.exists()) {
	println "Folder \"" + dirToImport.getAbsolutePath() + "\" does not exist"
	return
}

def rootCaFolderObject = rsuite.findObjectForPath(targetFolder)
if (rootCaFolderObject != null) {
	def rootCaFolderId = rootCaFolderObject["moid"]
	if (rootCaFolderId != null && "" != rootCaFolderId ) {
		rootId = rootCaFolderId
	}
}
if (rootId == null || "" == rootId) {
	println "LoadInitialContent : Import Failed. No Root folder exists for ID='" + rootId + "'!"
	return
}

println "Importing ${dirToImport.name} to RSuite folder \"${targetFolder}\" with ID=\"${rootId}\"..."
createBrowseTreeFromFolder(dirToImport, targetFolder, rootId, 0, false, tier)
rsuite.logout()
