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
    createRoleIfnotExist("RSuiteBasicUser", "This is a basic user role.");
      
}
 finally {
	rsuite.logout();
}

