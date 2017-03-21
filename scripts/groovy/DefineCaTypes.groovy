/**
 * Define a custom CA type
 *
 * @param name Name of custom CA type.  Corresponds to CSS class name for
 *        custom icon.
 * @param label Label in UI
 * @param requiresCheckout Submit true if RSuite should require the CA be 
 *        checked out before it may be modified.
 * @param defaultChildType The default CA type for children of this CA type
 *        (e.g., "article" may be a good default for "issue")
 */
 
def defineContentAssemblyType(name, label, requiresCheckout, 
  defaultChildType) {
   info("Defining CA type where name is \"" + name + 
      "\", label is \"" + label + 
      "\", requiresCheckout is \"" + requiresCheckout + 
      "\" and defaultChildType is \"" + defaultChildType + "\"...")
   def result = rsuite.createContentAssemblyTypeDefinition(
      name, 
      label, 
      requiresCheckout, 
      defaultChildType)
}

/**
 * Print an informational message to stdout
 *
 * @param msg
 */
def info(msg) {
   println " + [INFO] ${msg}"
}

rsuite.login()

try {

}
 finally {
	rsuite.logout();
}
