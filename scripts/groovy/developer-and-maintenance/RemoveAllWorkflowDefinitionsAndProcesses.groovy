// Remove all process/workflow definitions
//
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

rsuite.login();
rsuite.removeAllProcessDefinitions();
rsuite.logout();

