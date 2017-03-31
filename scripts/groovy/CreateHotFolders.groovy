import com.reallysi.rsuite.admin.importer.*
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

def createDir (path)
{
	File dir = new File(path);

	if	(dir.exists())
	{
		println "The directory \"" + path + "\" already exists.";
		return;
	}

	if	(dir.mkdirs())
	{
		println "Create directory \"" + path + "\".";
		return;
	}

	println "[ERROR] Unable to create directory \"" + path + "\".";
}

def setUpHotFolder (folderPath, workflowKey, processName)
{
	rsuite.removeHotFoldersByProcessDefinition(processName);
	rsuite.setHotFolder(folderPath, workflowKey, true);
	createDir(folderPath);
}

if	(baseDir == null)
{
	println "[ERROR] The \"baseDir\" parameter must be passed. Unable to configure the hot folders.";
	return;
}

rsuite.login();

setUpHotFolder(baseDir + "/" + "TPM", "ATD NEW TPM", "ATD NEW TPM");
setUpHotFolder(baseDir + "/" + "T+D Back Content", "ATD Article Word To XML To Indesign", "ATD Article Word To XML To Indesign");
setUpHotFolder(baseDir + "/" + "CTDO", "ATD CTDO", "ATD CTDO");

rsuite.logout();
