/*
 * An action that opens a browser window to a location specified via windowOptions.
 *
 * Background: https://rsicms.atlassian.net/wiki/pages/viewpage.action?pageId=44466221
 */
$(function (global) {
    if (!RSuite.Action.get('rsuite:openWindow')) {
        RSuite.Action({
            id: 'rsuite:openWindow',
            invoke: function (context) {
                var winOpts = RSuite.Webservice.serviceOptions(context).windowOptions,
                    href = winOpts.href,
                    focus = winOpts.focus;
                delete winOpts.href;
                var win = RSuite.openWindow(href, winOpts);
                if (focus) {
                    win.focus();
                }
                return RSuite.success;
            }
        });
    }
});