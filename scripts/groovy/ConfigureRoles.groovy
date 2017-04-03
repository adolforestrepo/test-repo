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
    createRoleIfnotExist("Art Director", "Art Director.");
    createRoleIfnotExist("Managing Editor", "Managing Editor.");
    createRoleIfnotExist("Associate Editor", "Associate Editor.");
    createRoleIfnotExist("Editor", "Editor.");
    
}
 finally {
	rsuite.logout();
}

