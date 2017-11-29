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
 
def defineContentAssemblyType(typeName, label, requiresCheckout, defaultChildType) {
	println " + [INFO] Defining content assembly types:"
	println " + [INFO] Type Name: ${typeName}"
	println " + [INFO] Label: ${label}"
	println " + [INFO] Requires Checkout: ${requiresCheckout}"
	println " + [INFO] Default Child Type: ${defaultChildType}"

	def result = rsuite.createContentAssemblyTypeDefinition(
		typeName, 
		label, 
		requiresCheckout, 
		defaultChildType)
}

rsuite.login()

/* Required by rsuite-templates-manager-plugin. */
defineContentAssemblyType("ca_template", "CA Template", false, null)
defineContentAssemblyType("book", "Book", false, null)
