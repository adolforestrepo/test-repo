//
// CMS UI action customizations for the ASTD project.
//

(function () {
    // Replace this value with whatever you want to be the default.
	var defaultBrowseCode = 'rsuite:available-tasks';
    RSuite.model.session.addObserver('key', RSuite.model.session, function () {
        if (this.get('key')) {
                var ctl = RSuite.view.Activity.getController();
                ctl.set('selectedBrowseCode', defaultBrowseCode);
                ctl.send('publishBrowseCode');
                window.history.pushState("object or string", "Title", "/rsuite-cms/workflow/Browse/rsuite:available-tasks");
          }
    });
}());

