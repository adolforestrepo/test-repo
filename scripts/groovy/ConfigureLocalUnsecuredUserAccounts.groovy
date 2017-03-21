import com.reallysi.rsuite.client.api.*

// -----------------------------------------------------------------------

def projectDir = new File(scriptFile.absolutePath).parentFile
// NOTE: Groups are mapped to roles by the role-mappings.properties file.
def pass = "test"



rsuite.login();

def redefineExistingUsers = false;

try {

	// NOTE: username is taken from the email address.
	def file = new File(projectDir, 'data/users.csv')
	file.eachLine { line ->
		while (true) {
			// http://stackoverflow.com/questions/205660/best-pattern-for-simulating-continue-in-groovy-closure
			if (line.trim() == "") { 
				break;
			} 
			def fields = line.split(';');
			def fullName = fields[0].trim();
			def userId = fields[1].trim();
			def email = fields[2].trim();
    		if ( userId == "") { 
    		    if ( email == "") { 
					println "Skipping user. Can not determine userId.";
                }            	
       			userId = email.toLowerCase().substring(0, email.indexOf('@'))
			} 
			def groups = fields[3];
			def user = rsuite.getUser(userId);
			if (user["userId"]) {
				if (redefineExistingUsers) {
					println "Local user manager: updating '${userId}'";
					rsuite.updateLocalUser(userId, fullName, email, groups);
					rsuite.setLocalUserPassword(userId, pass);
				} else {
					println "Local user manager: skipped existing user '${userId}'";
				}
			} else {
				println "Local user manager: defining '${userId}' with groups ${groups}";
				rsuite.createLocalUser(userId, pass, fullName, email, groups);
			}
			break;
		}
	};

	
} finally {
	rsuite.logout();
}

//===========================================================================
