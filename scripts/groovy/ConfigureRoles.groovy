import com.reallysi.rsuite.client.api.*

def  createRoleIfnotExist(roleName, roleDesc){
	try{
	rsuite.createRole(roleName, "", roleDesc);
	println "Adding "+ roleName +" (desc="+roleDesc+")";
	}catch (Exception e) {
		// ignore
	}
}

rsuite.login();

try {
    /* Basic role; RSuiteUser had unexpected results in 5.0.5
     */
    createRoleIfnotExist("Art_Director", "Art Director.");
    createRoleIfnotExist("Managing_Editor", "Managing Editor.");
    createRoleIfnotExist("Associate_Editor", "Associate Editor.");
    createRoleIfnotExist("Editor", "Editor.");
	createRoleIfnotExist("Reviewer", "Reviewer.");
	createRoleIfnotExist("Production_Specialist", "Production Specialist.");

    /* Book workflow roles */
	createRoleIfnotExist("DevelopmentalEditor", "Developmental Editor.");
	createRoleIfnotExist("CopyEditor", "Copy Editor (Production Editor).");
	createRoleIfnotExist("Proofreader", "Proofreader.");
	createRoleIfnotExist("Designer", "Designer.");
	createRoleIfnotExist("DesignReviewer", "Design Reviewer.");
	createRoleIfnotExist("ProductionManager", "Production Manager.");
	createRoleIfnotExist("Author", "Author.");
}
 finally {
	rsuite.logout();
}

