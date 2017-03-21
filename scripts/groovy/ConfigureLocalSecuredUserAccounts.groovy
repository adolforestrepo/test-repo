import com.reallysi.rsuite.client.api.*

// -----------------------------------------------------------------------

def projectDir = new File(scriptFile.absolutePath).parentFile
// NOTE: Groups are mapped to roles by the role-mappings.properties file.

// Credit: http://www.codecodex.com/wiki/Generate_a_random_password_or_random_string
def generateString(Random rand) {
	def pool = ['a'..'z','A'..'Z',0..9,'_'].flatten()
	def passChars = (0..10).collect { pool[rand.nextInt(pool.size())] }
	def password = passChars.join()
}

rsuite.login();

def redefineExistingUsers = false;

try {

	// NOTE: username is taken from the email address.
	def file = new File(projectDir, 'data/secureUsers.csv')
	Random rand = null;
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
			rand = new Random(System.currentTimeMillis())
			def pass = generateString(rand);
			
			// Starting with 5.1.3, the returned map is not null if user already exists
			if (user["userId"]) {
				if (redefineExistingUsers) {
					println "Updating ${userId}";
					rsuite.updateLocalUser(userId, fullName, email, groups);
					rsuite.setLocalUserPassword(userId, pass);
				} else {
					println "Skipping existing user ${userId}";
				}
			} else {
				println "Adding ${userId} (password=*********)";
				rsuite.createLocalUser(userId, pass, fullName, email, groups);
			}
			break;
		}
	};

	
} finally {
	rsuite.logout();
}

//===========================================================================
