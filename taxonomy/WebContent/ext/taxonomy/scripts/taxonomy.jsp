<%@ page language="java" contentType="text/javascript" %><%@include file="/WEB-INF/common/incl.jsp" %>

// TODO: Custom search tab pops up "Additional Terms" in separate dialog
//       so more room exists to render lists.

var astdTaxSearchTextFields = [
    {name:'pid',label:'GUID',col:1},
    {name:'pcode',label:'Product_Code',col:2},
    {name:'isbn',label:'ISBN',col:3}
    //{name:'sname',label:'Short Name',col:1},
    //{name:'lname',label:'Long Name',col:2},
    //{name:'tagged',label:'Tagged By',col:3}
];

jQuery(document).ready(function() {
    //text fields
    var fields = astdTaxSearchTextFields;
    //taxonomy search form
    var tsf = RSuite.control.BaseSearchForm.create(fields, {
        useColumns: true,
        searchType: 'rql'
    });
    jQuery('<div class="field" style="margin-left:8px;">'+
           '<label for="text">Text</label><div class="input">'+
           '<textarea style="width:98%" name="text"></textarea></div>').
           appendTo(tsf.find(".formContainer > form"));
    var taxSearchSerializer = function(field,data,selNames) {
        var val = "";
        jQuery.each(data,function(k,v) {
            if(k == "children") return;
            val += "<strong>"+k+"</strong>: ";
            var valueNames = new Array();
            jQuery.each(v,function() {
                valueNames.push(this.name);
            });
            val += valueNames.join(", ");
            val += "<br/>";
        });
        field.find(".input").html(val);
    }
    tsf.find(".formContainer")
       .addClass("taxonomyForm")
       .append('<div class="advanced taxonomyField"'+
               ' style="margin:10px 0px;clear:both">'+
               '<a class="addOptions">Additional Terms</a>'+
               '<div class="input"></div></div>')
       .click(function(event) {
            var target = jQuery(event.target);
            if (target.is(".addOptions")) {
                target = target.parent();
                //tsf.find(".taxonomyForm").css('height', 400);
                RSuite.control.Taxonomy.openSearchControl(target, {
                    includeOptionsLevel: true,
                    serializer: taxSearchSerializer,
                    isSearch: true
                });
            }
        });

    var taxMap = RSuite.control.Taxonomy.newMoTaxMap();
    RSuite.control.Taxonomy._initMap(tsf.find(".taxonomyForm"), taxMap);

    tsf.addButton("Search", {
        onClick: RSuite.control.Taxonomy.buildQueryString
    });
    //tsf.addCopyResultsButton();  // not avail in le 3.3.4
    tsf.addButton('Copy Results', {
        requireResults:true,
        preventSubmit:true,
        onClick: RSuite.control.BaseSearchForm.copyResults,
        disabled:true
    });
    RSuite.control.SearchPanel.addTab("Advanced",{
        searchForm: tsf
    });
});

var taxonomyMoId = <%= com.reallysi.rsuite.cmsui.Configuration.getProperty("rsuite.taxonomy.moid","6004") %>;


// Register custom action name for context menus:
// This name is referenced in rsuite-plugin.xml.
RSuite.control.ContextMenu.addAction("astd:taxonomyForm",
    function(moData, options) {
        RSuite.control.Taxonomy.create(moData,{});
    }); 
    
RSuite.control.Taxonomy = {
    taxTextFields:  ['GUID','Product_Code','ISBN','Tagged By'],

    taxXml: null,       // Taxonomy definitions XML (jq'ed)
    taxMap: { },        // Mapping of fields to/from taxonomy-option datums
    currentMoId: null,  // Current MO we are operating against

    _getTaxUrl: function() {
        return RSuite.config.serviceURL+'?loadaction=download&loadskey='+
            RSuite.config.skey+'&loadmoid='+taxonomyMoId;
    },

    create: function(moData, options) {
        RSuite.control.Taxonomy.currentMoId = moData.refId;

        var taxUrl = RSuite.control.Taxonomy._getTaxUrl();
        var url = RSuite.config.restURL+"api/astd:getClassificationOfMo";

        var opts = options;
        var mData = moData;
        jQuery.get(taxUrl,
            function(xhrData,textStatus) {
                RSuite.control.Taxonomy.setTaxXml(xhrData);

                // Make sure caching is disabled so we refetch new
                // each time.  Also, we handling parsing the XML data
                // retrieved, so we tell jq that data is text to
                // avoid it trying to parse it.
                jQuery.ajax({
                  url: url,
                  data: {
                    skey: RSuite.config.skey,
                    moId: moData.refId
                  },
                  cache: false,
                  dataType: 'text',
                  success: function(data,status,req) {
                      RSuite.control.Taxonomy._create(mData, opts, data);
                  }
                });
            },
            "xml");
    },

    // Set the main taxonomy definition based upon given XML
    setTaxXml: function(xml) {
        // Need to strip out the doctype for IE to parse it correctly
        var taxstart = xml.indexOf("<taxonomy>");
        xml = xml.slice(taxstart);
        RSuite.control.Taxonomy.taxXml = jQuery(parseXML(xml));

        RSuite.control.Taxonomy.taxMap = {
            values: { },
            fields: { }
        };
        jQuery.each(
            RSuite.control.Taxonomy.taxXml.find('taxonomy-options'),
            function() {
                var field = jQuery(this).attr('name');
                RSuite.control.Taxonomy.taxMap.fields[field] = { };
                jQuery.each(
                    jQuery(this).find('taxonomy-option'),
                    function () {
                        var v = jQuery(this).attr('value');
                        RSuite.control.Taxonomy.taxMap.fields[field][v] = 1;
                        if (!RSuite.control.Taxonomy.taxMap.values[v]) {
                            RSuite.control.Taxonomy.taxMap.values[v] = { };
                        }
                        RSuite.control.Taxonomy.taxMap.values[v][field] = 1;
                    });
            });
    },

    // Create new tax map from classification XML:
    // The map is composed of two sub-objects: `fields' and `values'.
    // The `fields' object specifies a hash of fields and the set
    // of values that a field can have.  The `values' object is
    // a mapping of a value to its associated field.  Since values
    // are only unique on a per-field basis, a single value can
    // map to multiple fields.
    //
    // The `values' mapping is mainly used for pre-selecting
    // values of a field in the UI in a quick matter.
    //
    newMoTaxMap: function(xml) {
        var moTaxMap = {
            values: { },
            fields: { }
        };
        if (typeof xml === 'undefined' || !xml.match(/\S/)) {
            // No XML provided, so return empty map
            return moTaxMap;
        }

        var taxXml = jQuery(parseXML(xml));
        jQuery.each(
            taxXml.find('taxonomy-field'),
            function() {
                var field = jQuery(this).attr('name');
                moTaxMap.fields[field] = { };
                jQuery.each(
                    jQuery(this).find('taxonomy-value'),
                    function () {
                        var v = jQuery(this).text();
                        moTaxMap.fields[field][v] = 1;
                        if (!moTaxMap.values[v]) {
                            moTaxMap.values[v] =  { };
                        }
                        moTaxMap.values[v][field] = 1;
                    });
            });
        return moTaxMap;
    },

    // Does actual creation of taxonomy-edit form
    _create: function(moData,options,xml) {
        var div = jQuery('<div class="taxonomyForm loading"></div>');

        // We associate a map structure to the form to contain
        // fields that are set with the form.  The data is initialized
        // with the classification of the MO (as specified by the `xml'
        // parameter).
        var taxMap = RSuite.control.Taxonomy.newMoTaxMap(xml);
        RSuite.control.Taxonomy._initMap(div, taxMap);

        /*
         *  The creation of the main field components could be
         *  derived from the taxonomy.xml, but for now it's hard-coded.
         */
        // Free-form text fields
        var fields = [];
        var textFields = RSuite.control.Taxonomy.taxTextFields;
        for (var i=0; i < textFields.length; ++i) {
            var name = textFields[i];
            var f = {
                name: name,
                label: name,
                cssClass: 'taxonomyTextField',
                col: 1
            };
            // See if text field has default value.  If so, the
            // object of the field should only have one property,
            // which is the value for the tax field.
            var vobj = taxMap.fields[name];
            if (vobj) {
                var v = "";
                jQuery.each(vobj, function(k, ignore) {
                    v = k;
                });
                if (v.match(/\S/)) f.value = v;
            }
            fields.push(f);
        }

        // add the taxonomy columns
        // (this gets easier after 3.3.4 with custom renderers)
        var frm = RSuite.control.Form.create(fields, {
            name: 'taxEditForm',
            useColumns: true
        }).hide();
        jQuery.each(
            jQuery(frm).find('input[type="text"]'),
            function() {
                jQuery(this).bind('change',
                    RSuite.control.Taxonomy.handleTaxonomyChange);
            });

        var col2 = jQuery('<div class="col"></div>').appendTo(frm);
        jQuery.each([
                'Research',
                'Point of View',
                'Delivery Method',
                'Delivery Format',
                'Distribution Frequency',
                'Audience',
                'Certification',
                'Level of Expertise'
            ],
            function() {
                col2.append(RSuite.control.Form.fieldRenderer.taxonomy({
                    name: this,
                    label: this,
                    value: RSuite.control.Taxonomy.fieldValueToString(
                        taxMap, this),
                    col: 2,
                    type:'taxonomy'
                }));
            });

        var col3 = jQuery('<div class="col"></div>').appendTo(frm);
        jQuery.each([
                'Affiliation with Training Organizations',
                'Affiliation with Industry Organizations',
                'Affiliation with Higher Education Organizations',
                'Affiliation with Foundations',
                'Affiliation with Government Organizations',
                'Industry',
                'Country',
                'Keywords'
            ],
            function() {
                col3.append(RSuite.control.Form.fieldRenderer.taxonomy({
                    name: this,
                    label: this,
                    value: RSuite.control.Taxonomy.fieldValueToString(
                        taxMap, this),
                    col: 3,
                    type:'taxonomy'
                }));
            });


        //add event delegation for displaying the control
        frm.click(function(event) {
            var target = jQuery(event.target);
            if (!target.is(".taxonomyField")) {
                target = target.parents(".taxonomyField");
            }
            if (!target.length) return;
            var fieldName = target.attr("taxonomyName");
            var settings = {};
            settings.taxonomyName = fieldName;
            RSuite.control.Taxonomy.openControl(target,settings);
        });

        frm.appendTo(div.removeClass("loading")).fadeIn();
        var buttons = jQuery('<div class="buttons centered"></div>').
                appendTo(div);
        jQuery('<button>Save</button>').appendTo(buttons).
                click(RSuite.control.Taxonomy.handleSaveClick);
        jQuery('<button>Cancel</button>').appendTo(buttons).
                click(RSuite.control.Taxonomy.handleCancelClick);

        //display it
        div.modal({
            title: 'Edit MetaData Form',
            width:'75%',
            buttons: false,
            buttonsClass: 'centered'
        });
    },

    handleCancelClick: function(event) {
        RSuite.control.Modal.close();
    },

    handleSaveClick: function(event) {
        var map = RSuite.control.Taxonomy._getMapFromEvent(event);
        RSuite.control.Taxonomy._mapCommitEdit(map);

        // This URL depends on the identifier used in rsuite-plugin.xml
        // for the web service to save classification data.
        var url = RSuite.config.restURL+"api/astd:setClassificationForMo?"+
                  "skey="+RSuite.config.skey;

        var xml = RSuite.control.Taxonomy.moTaxMapToXml(map.main);
        jQuery.ajax({
            type: 'POST',
            url: url,
            data: {
                moId: RSuite.control.Taxonomy.currentMoId,
                xml: xml
            },
            cache: false,
            dataType: 'text',
            success: function(data,status,req) {
                RSuite.control.Modal.close();
                RSuite.control.Modal.open(
                    data,
                    { title: "Taxonomy Saved"
                    });
            },
            error: function(httpReq, status, errorThrown) {
                // To work around bug in RSuite for POST requests,
                // we check the response body to determine if a real
                // error occurred or not.
                var msg = httpReq.responseText;
                if (msg.match(/SUCCESS/)) {
                    RSuite.control.Modal.close();
                    RSuite.control.Modal.open(
                        msg,
                        { title: "Taxonomy Saved"
                        });
                } else {
                    RSuite.control.Modal.open(
                        msg,
                        { title: "Taxonomy Save Error"
                        });
                }
            }
        });
    },

    // Generate XML string from MO taxonomy map
    moTaxMapToXml: function(map) {
        var xml = "<classification>";
        jQuery.each(map.fields, function(field, obj) {
            var isFirst = true;
            jQuery.each(obj, function(val, flag) {
                if (flag) {
                    if (isFirst) {
                        isFirst = false;
                        xml += '<taxonomy-field name="'+
                            RSuite.escapeHtml(field)+'">';
                    }
                    xml += '<taxonomy-value>'+
                           RSuite.escapeHtml(val)+
                           '</taxonomy-value>';
                }
            });
            if (!isFirst) xml += '</taxonomy-field>';
        });
        xml += "</classification>";
        return xml;
    },

    // Create an RQL query based on search form fields.  This function
    // is registered as a pre-function of a search form for building
    // the query string before submission to server.
    //
    // `this' is set to the form object when this function is called.
    //
    // XXX: RQL does not support xpath, so searching on specific
    // terms of a give field is not doable.  For taxonomy terms,
    // we basically just contextualize them via the <classification>
    // element.
    //
    buildQueryString: function() {
        var map = RSuite.control.Taxonomy._getMapFromForm(this);
        RSuite.control.Taxonomy._mapCommitEdit(map);
        map = map.main;

        var form = this;
        var q = "select * from article,chapter";
        var haveField = false;

        // Process text input fields
        jQuery.each(astdTaxSearchTextFields, function(i, obj) {
            var name = obj.name;
            var tf = form.find("input[name='"+name+"']");
            if (tf) {
                var val = tf.attr('value');
                if (!(val.match(/\S/))) return;
                if (haveField) {
                    q += " and ";
                } else {
                    q += " where ";
                    haveField = true;
                }
                q += "(classification contains '" + val + "')";
            }
        });

        // Process taxonomy terms.
        jQuery.each(map.fields, function(field, obj) {
            if (haveField) {
                q += " and ";
            }
            var firstVal = true;
            var haveVal = false;
            jQuery.each(obj, function(val, flag) {
                if (flag) {
                    haveVal = true;
                    if (!haveField) q += " where ";
                    haveField = true;
                    if (firstVal) {
                        q += "(";
                        firstVal = false;
                    } else {
                        q += " or ";
                    }
                    q += "(classification contains '" + val + "')";
                }
            });
            if (haveVal) {
                q += ")";
            }
        });

        // Process general text words
        var textField = this.find("textarea[name='text']");
        if (textField) {
            var val = textField.attr('value');
            if (val.match(/\S/)) {
                var words = val.split(/\s+/);
                jQuery.each(words, function(i, w) {
                    if (haveField) {
                        q += " and";
                    } else {
                        q += " where";
                        haveField = true;
                    }
                    q += " (body contains '" + val + "')";
                });
            }
        }
        return q;
    },

    openSearchControl: function(field,settings) {
        var f = field;
        var set = settings;
        jQuery.get(RSuite.control.Taxonomy._getTaxUrl(),
            function(xhrData,textStatus) {
                RSuite.control.Taxonomy.setTaxXml(xhrData);
                set.width = "60%";
                set.height = "80%";
                set.center = true;
                set.noanimate = true;
                set.domParent = jQuery("body");
                set.mask = true;
                RSuite.control.Taxonomy.openControl(f, set);
            },
            "xml");
    },

    openControl: function(field,settings) {
        var parent = field.parents(".taxonomyForm");
        var settings = jQuery.extend({},settings);
        var div = jQuery('<div class="taxonomyControl"><div class="heading">'+
                         '<div class="fieldName">Edit: <strong>'+
                         field.find("a").text()+
                         '</strong></div></div></div>');
        //store the relationship in data since we can't rely on them being nested in the DOM
        div.data("taxonomyField",field);
        div.data("taxonomyForm",parent);
        parent.data("taxonomyControl",div);
        var acts = jQuery(
            '<div class="actions"><a class="clearAll clickable">clear all</a>'+
            ' <span class="sep">|</span>'+
            ' <a class="cancel clickable">cancel</a>'+
            ' <span class="sep">|</span>'+
            ' <a class="done clickable">done</a></div>').
            appendTo(div.find(".heading"));
        acts.click(function(event) {
            var target = jQuery(event.target);
            var map = RSuite.control.Taxonomy._getMapFromEvent(event);
            if (target.is(".done")) {
                RSuite.control.Taxonomy.saveSelections(div,parent,settings);
                RSuite.control.Taxonomy._mapSaveEdit(map);
                div.find(".taxonomyLevels").hide();
                div.animate(div.data("orig"),
                    function() { jQuery(this).remove() });
                if(settings.domParent && settings.mask) settings.domParent.unmask();
            }
            if (target.is(".cancel")) {
                RSuite.control.Taxonomy._mapResetEdit(
                    map, settings.taxonomyName);
                div.find(".taxonomyLevels").hide();
                div.animate(div.data("orig"),
                    function() { jQuery(this).remove() });
                if (settings.isSearch) {
                    target.parents(".taxonomyForm").css('height', 'auto');
                }
                if(settings.domParent && settings.mask) settings.domParent.unmask();
            }
            if (target.is(".clearAll")) {
                div.find(":checked").removeAttr("checked");
                div.find(":input[disabled=disabled]:not([selectable=false])")
                   .removeAttr("disabled");
                RSuite.control.Taxonomy._mapClearEdit(
                    map, settings.taxonomyName);
            }
        });
        var pos = field.position();
        var ow = field.width();
        var oh = field.height();
        var css = {width:ow,height:oh,left:pos.left,top:pos.top,opacity:0.1};
        div.css(css);
        div.data("orig",css);
        //size it the same as the modal
        var domParent = settings.domParent || parent;
        if(settings.mask) {
        	domParent.mask();
        	div.css("zIndex",7500);
       	}
        div.appendTo(domParent);
        var h = settings.height || domParent.height() - 5;
        var w = settings.width || domParent.width() - 5;
        var levels = jQuery(
            '<div class="taxonomyLevels">'+
            '<table cellpadding="0" cellspacing="0"></table></div>')
            .hide().appendTo(div);
        var sizeLevels = function() {
            levels.css({height:div.innerHeight()-div.find(".heading").innerHeight()});
            RSuite.control.Taxonomy.loadData(field.name,{
                taxonomyName: settings.taxonomyName,
                onSuccess: renderFirstLevel,
                includeOptionsLevel: settings.includeOptionsLevel,
                openSettings: settings
            });
        }
        //callback closure
        var renderFirstLevel = function(data, settings) {
            //alert('adding first level with data '+data.debugXML());
            div.data("taxonomyData",data);
            settings.parent = div;
            RSuite.control.Taxonomy.addLevel(data, levels, settings);
            levels.fadeIn().click(
                RSuite.control.Taxonomy.handleClick).find(
                    ".taxonomyLevel:first").css("margin-left",6);
        }
        var finalCss = {opacity:1.0,top:settings.top || 0,left:settings.left || 0,height:h,width:w};
        if(settings.noanimate) {
        	div.css(finalCss);
            if(settings.center) div.center("both");
        	sizeLevels();
        } else {
        	div.animate(finalCss,sizeLevels);
        }
    },

    handleSelection: function(event) {
        var chx = jQuery(event.target);
        var mylevel = chx.parents(".taxonomyLevel:first");
        var sibs = mylevel.find(":checked").length;
        var parentChx = mylevel.prevAll(
                ":visible:first").find(".active :input");
        if(sibs) {
            parentChx.attr("checked",true);
            parentChx.attr("disabled",true);
        } else {
            parentChx.removeAttr("checked");
            if(parentChx.is("[selectable!=false]")) {
                parentChx.removeAttr("disabled");
            }
        }
        parentChx.triggerHandler("taxonomyChange");
    },

    // Function to handle custom "taxonomyChange" event.
    handleTaxonomyChange: function(event) {
        var target = jQuery(event.target);
        var v = target.val();
        var field;
        var isChecked = false;
        var isText = false;
        if (target.is('input:text')) {
            field = target.attr('name');
            isChecked = v.match(/\S/);
            isText = true;
        } else {
            field = target.data("taxonomyFieldName");
            isChecked = target.is(':checked');
        }

        var con = RSuite.control.Taxonomy._getMapFromTarget(target);
        var map = con.edit;
        if (isText) {
            // Remove old value for field
            if (map.fields[field]) {
                jQuery.each(map.fields[field], function(oldVal, flag) {
                    delete map.values[oldVal][field];
                });
            }
            map.fields[field] = { };
        }
        if (isChecked) {
            if (!map.fields[field]) {
                map.fields[field] = { };
            }
            map.fields[field][v] = 1;
            if (!map.values[v]) {
                map.values[v] = { };
            }
            map.values[v][field] = 1;

        } else {
            if (map.fields[field]) delete map.fields[field][v];
            if (map.values[v])     delete map.values[v][field];
        }
    },

    handleClick: function(event) {
        var target = jQuery(event.target);
        if(target.is("input")) {
            target.trigger("taxonomyChange");
            return;
        }
        if(!target.is(".item")) target = target.parents(".item:first");
        var mylevel = target.parents(".taxonomyLevel");
        var levels = mylevel.parents(".taxonomyLevels");
        if(target.is(".hasChildren")) {
            var itemData = target.data("itemData");
            var childData = (target.attr("taxonomyOptionsGroup"))
                            ? itemData
                            : itemData.children("taxonomy-children");
            RSuite.control.Taxonomy.addLevel(
                childData,
                levels,
                { parent: target,
                  depth: mylevel.prevAll(".taxonomyLevel").length+1,
                  taxonomyName: target.data("taxonomyFieldName")
                });
            mylevel.find(".active").removeClass("active");
            target.addClass("active");
        } else {
            //if there are no children then clicking selects it
            var inp = target.find(":input").click();
            inp.trigger("taxonomyChange");
        }
    },

    loadData: function(taxonomyName,settings) {
        var settings = jQuery.extend({},settings);
        var data = RSuite.control.Taxonomy.taxXml;
        var opts = (settings.taxonomyName)
               ? data.find("taxonomy-options[name="+settings.taxonomyName+"]")
               : data.find("taxonomy-options");
        settings.onSuccess.call(this,opts,settings);
    },

    parseChildValues: function(items,arr) {
        jQuery.each(items,function() {
            if(this.name) arr.push(this.name);
            if(this.children) {
                RSuite.control.Taxonomy.parseChildValues(this.children,arr);
            }
        });
    },

    //saves it as json
    saveSelections: function(taxonomyControl,taxonomyForm,settings) {
        var settings = jQuery.extend({},settings);
        var data = {};
        data.children = [];
        var root = taxonomyControl.find(".taxonomyLevel:first");
        //drill down each level
        RSuite.control.Taxonomy.serializeFlatSelections(
            taxonomyControl, data, settings.includeOptionsLevel);

        //debug the json, jsonify should be used when submitting the
        //entire form if json is desired by the webservice
        //alert(RSuite.jsonify(data));
        var field = taxonomyControl.data("taxonomyField");
        var selNames = [];
        RSuite.control.Taxonomy.parseChildValues(data.children,selNames);
        if (settings.serializer) {
            settings.serializer.call(this,field,data,selNames);
        } else {
            field.data("taxonomySelections",data);
            field.find(".input").html(selNames.join(", "));
        }
    },

    //this is a flat list of everything selected
    serializeFlatSelections: function(level,data,includeFirstLevel) {
        jQuery.each(level.find(":checked"),function() {
            var inp = jQuery(this);
            var selection = {};
            if(inp.attr("selectable") != "false") {
                selection.value = inp.val();
                selection.name = inp.attr("name") || selection.value;
                var myLevel = inp.parents(".taxonomyLevel:first");
                if(includeFirstLevel && myLevel.data("parentItem")) {
                    var parentItem = myLevel.data("parentItem");
                    while(parentItem.parents(".taxonomyLevel:first")
                                    .data("parentItem")) {
                        parentItem = parentItem.parents(
                            ".taxonomyLevel:first").data("parentItem");
                    }
                    var firstLevelName = parentItem.find(":input").attr("name");
                    if(!data[firstLevelName]) {
                        data[firstLevelName] = new Array();
                    }
                    data[firstLevelName].push(selection);

                } else {
                    data.children.push(selection);
                }
            }
        });
    },

    //this is recursive
    serializeSelections: function(level,data) {
        jQuery.each(level.find(".item"),function() {
            var item = jQuery(this);
            var inp = item.find(":checked");
            if(!inp.length) return;
            //if it's checked, add it's data
            var selection = {};
            data.push(selection);
            if(inp.attr("selectable") != "false") {
                selection.value = inp.val();
                selection.name = inp.attr("name") || selection.value;
            }
            //see if it has children also
            var childDiv = item.data("childDiv");
            if(childDiv) {
                selection.children = [];
                RSuite.control.Taxonomy.serializeSelections(
                    childDiv,selection.children);
            }
        });
    },

    addLevel: function(data,levelsDiv,settings) {
        var levelsContainer = levelsDiv.find(".levelsContainer");
        if(!levelsContainer.length) {
            levelsContainer = jQuery(
                '<tr class="levelsContainer"></tr>').appendTo(
                    levelsDiv.find("table:first"));
        }
        if(settings.depth) {
            //delete any at or below this depth already
            levelsDiv.find(".taxonomyLevel:gt("+(settings.depth-1)+")").hide();
        }
        var childDiv = (settings.parent) ? settings.parent.data("childDiv") : 0;
        if(childDiv) {
            childDiv.show().find(".active").removeClass("active");
        } else {
            var level = RSuite.control.Taxonomy.drawLevel(data,settings);
            if(settings.parent) {
                //store parent and child data so we can navigate in both
                //directions as needed
                settings.parent.data("childDiv",level);
                if(settings.parent.is(".item")) {
                    level.data("parentItem",settings.parent);
                }
            }
            if(level) levelsContainer.append(level);
        }
        var overflow = levelsContainer.outerWidth() - levelsDiv.innerWidth();
        if(overflow > 0) {
            levelsDiv.animate({"scrollLeft":overflow+15},{duration:700});
        }
    },

    //data represents the possible values
    //levelsDiv is the DOM element in which to draw the new level
    //options can include the currently selected values
    drawLevel: function(data, options) {
        //alert("drawing level with data "+data.debugXML());
        var options = jQuery.extend({},options);
        var children = (options.includeOptionsLevel)
                        ? data
                        : data.children("taxonomy-option");
        if(!children.length) return;
        var td = jQuery('<td nowrap="true" class="taxonomyLevel"></td>');
        var level = jQuery('<div></div>').appendTo(td);
        jQuery.each(children,function() {
            var option = jQuery(this);
            var isTaxonomyOptions = option.get(0).tagName == "taxonomy-options";
            if(isTaxonomyOptions) {
                //set the value to be the name
                option.attr("value",option.attr("name"));
                //make it not selectable
                option.attr("selectable","false");
            }
            var val = option.attr("value");
            var label = option.attr("name") || val;
            var item = jQuery('<div class="item"><span class="label">'+
                              label+'</span></div>');
            if (isTaxonomyOptions) item.attr("taxonomyOptionsGroup","true");
            var chx = jQuery('<input type="checkbox" name="'+label+
                             '" value="'+val+'"/>')
                    .prependTo(item)
                    .bind("taxonomyChange",
                          RSuite.control.Taxonomy.handleTaxonomyChange);
            var taxFldName = (isTaxonomyOptions)
                                ? label
                                : options.taxonomyName;
            item.data("taxonomyFieldName", taxFldName);
            chx.data("taxonomyFieldName", taxFldName);

            var map = RSuite.control.Taxonomy._getMapFromTarget(
                            options.parent, 'edit');
            if (option.attr("selectable") == "false") {
                chx.attr("disabled","disabled")
                chx.attr("selectable","false");

            } else if (taxFldName && map && map.values && map.values[val] &&
                        map.values[val][taxFldName]) {
                // MO has value, so pre-select it.
                chx.attr("checked", "checked");
            }
            if (isTaxonomyOptions ||
                    option.children("taxonomy-children").length) {
                item.addClass("hasChildren");
            }
            if (option.children("synonym").length) {
                var syns = [];
                jQuery.each(option.children("synonym"),function() {
                    syns.push(jQuery(this).text());
                });
                item.append('<span class="syn">&nbsp;</span>');
                item.attr("title","Same as: "+syns.join(", "));
            }
            item.data("itemData",option);
            level.append(item);
        });
        return td;
    },

    // Given a map and a field name, return a string (list) of the value(s)
    // associated with the field.
    //
    fieldValueToString: function(map, name) {
        var s = "";
        var vobj = map.fields[name];
        var isFirst = true;
        if (vobj) {
            jQuery.each(vobj, function(k, ignore) {
                if (isFirst) isFirst = false;
                else s += ", ";
                s += k;
            });
        }
        return s;
    },

    _getMapFromEvent: function(event, type) {
        var target = jQuery(event.target);
        return RSuite.control.Taxonomy._getMapFromTarget(target, type);
    },
    _getMapFromTarget: function(target, type) {
        var p = target.parents(".taxonomyForm");
        if (p.length != 0) {
            var o = RSuite.control.Taxonomy._getMapFromObj(p, type);
            if (o) {
                return o;
            }
        }
		if(!target.is(".taxonomyControl")) {
            target = target.parents(".taxonomyControl:first");
        }
        var p = target.data("taxonomyForm") || target.parents(".taxonomyForm");
        return RSuite.control.Taxonomy._getMapFromObj(p, type);
    },
    _getMapFromForm: function(form, type) {
        var p = form.find(".taxonomyForm");
        return RSuite.control.Taxonomy._getMapFromObj(p, type);
    },
    _getMapFromObj: function(obj, type) {
        var o = obj.data('taxMap');
        if (!o) return null;
        if (type == 'main') return o.main;
        if (type == 'edit') return o.edit;
        return o;
    },
    _initMap: function(o, baseMap) {
        var m = {
          main: baseMap,
          edit: RSuite.control.Taxonomy.deepCopy(baseMap),
          editSaved: RSuite.control.Taxonomy.deepCopy(baseMap)
        };
        o.data('taxMap', m);
        return m;
    },
    _mapClearEdit: function(o, field) {
        if (!field) {
            o.edit = { fields: {}, values: {} };
            return o;
        }
        if (o.edit.fields[field]) {
            jQuery.each(o.edit.fields[field], function(v, flag) {
                if (o.edit.values[v]) {
                    delete o.edit.values[v][field];
                }
            });
            delete o.edit.fields[field];
        };
        return o;
    },
    _mapResetEdit: function(o, field) {
        if (!field) {
            // No field, so reseting entire edit structure
            o.edit = RSuite.control.Taxonomy.deepCopy(o.editSaved);
            return o;
        }
        // Remove edit values associated with field
        if (o.edit.fields[field]) {
            jQuery.each(o.edit.fields[field], function(v, flag) {
                if (o.edit.values[v]) {
                    delete o.edit.values[v][field];
                }
            });
            delete o.edit.fields[field];
        };
        // Reset field and associated value from last saved
        if (o.editSaved.fields[field]) {
            o.edit.fields[field] = RSuite.control.Taxonomy.deepCopy(
                o.editSaved.fields[field]);
            jQuery.each(o.edit.fields[field], function(v, flag) {
                if (!o.edit.values[v]) {
                    o.edit.values[v] = { };
                }
                o.edit.values[v][field] = 1;
            });
        }
        return o;
    },
    _mapSaveEdit: function(o) {
        o.editSaved = RSuite.control.Taxonomy.deepCopy(o.edit);
        return o;
    },
    _mapCommitEdit: function(o) {
        RSuite.control.Taxonomy._mapSaveEdit(o);
        o.main = RSuite.control.Taxonomy.deepCopy(o.editSaved);
        return o;
    },

    deepCopy: function(obj) {
        return jQuery.extend(true, {}, obj);
    }
}

RSuite.control.Form.fieldRenderer = {
    taxonomy: function(fld,settings) {
        var valStr = "";
        //we expect an array of items as values
        if (fld.value) {
            valStr = fld.value;
        }
        var h = jQuery('<div class="field taxonomyField" taxonomyName="'+
                      fld.name+
                      '"><label><a class="clickable">'+
                      fld.label+
                      '</a></label><div class="input"></div></div>');
        if (valStr.match(/\S/)) {
            jQuery(h.find(".input")).html(valStr);
        }
        return h;
    }
}

RSuite.escapeHtml = function(s) {
    return s.replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/'/g,"&#x27;")
        .replace(/"/g,"&#x22;");
}

/* vim: set ts=4 sw=4 expandtab softtabstop=4: */
