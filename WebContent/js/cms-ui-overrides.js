//
// CMS UI overrides that do not have a more specific home (JS file).
//

// When clicking the title of an MO, do not raise inspect
// We want to raise the context menu but currently cannot figure out how to raise
// the correct context menu
RSuite.Tab.Content.Controller.viewers.Table.proto().ListView.reopen({
	clickRow: function (event, mo) {
		// Do nothing until we know how to raise the proper context menu
		return false;
	}
});

