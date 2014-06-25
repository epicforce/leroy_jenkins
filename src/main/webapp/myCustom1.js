/* 
 * template to update select box dynamically after sucessfull update of LEROY_HOME
 */
jQuery(document).ready(function(){
  jQuery('input[name="_.target"]').attr("readonly","readonly");
  jQuery('input[name="_.target"]').attr("disabled","disabled");
//  jQuery('input[id="main-table"]').css({"background-image": "url(plugin/leroy/myCustom1.js)"});
  
  
  //on load refresh the two select boxes rows
  //jQuery("input[id='getWo']").click(function(){
      var projectname = jQuery("input[name='_.projectname']").val();
      new Ajax.Request("/job/"+projectname+"/wo", {
          timeout: 10000,
            onSuccess: function(rsp) {

                new Ajax.Request("/job/"+projectname+"/fillWorkflowItems", {
                timeout: 10000,
                onSuccess: function(rsp) {
                    console.log(rsp);
                    var l = jQuery('select[name="_.workflow"]');
                    var currentSelection = l.value;

                    l.empty();

                    var selectionSet = false; // is the selection forced by the server?
                    var possibleIndex = null; // if there's a new option that matches the current value, remember its index
                    var opts = eval('('+rsp.responseText+')').values;
                    for( var i=0; i<opts.length; i++ ) {
                        l.append(new Option(opts[i].name,opts[i].value));
                        if(opts[i].selected) {
                            l.selectedIndex = i;
                            selectionSet = true;
                        }
                    if (opts[i].value==currentSelection)
                        possibleIndex = i;
                    }

                    // if no value is explicitly selected by the server, try to select the same value
                    if (!selectionSet && possibleIndex!=null)
                        l.selectedIndex = possibleIndex;
                    }
                });

                // my

                new Ajax.Request("/job/"+projectname+"/getConfig", {
                    timeout: 10000,
                    onSuccess: function(rsp) {
                        console.log(rsp);

                        var config = jQuery.parseJSON(rsp.responseText);

                        // fill in workflow
                        var options = "";
                        for (var i=0; i < config.workflows.length; i++) {
                            var wf = config.workflows[i];
                            options += "<option " + (wf.def ? "selected" : "") + " value='" + wf.name + "'>" + wf.name + "</option>";
                        }
                        jQuery('select[name="_.workflow"]').append(options);

                        // fill in environments
                        var m = jQuery('select[name="_.envrn"]');
                        var table = jQuery('table[name="envtable"]').html();
                        var currentSelection = m.value;

                        m.empty();

                        var selectionSet = false; // is the selection forced by the server?
                        var possibleIndex = null; // if there's a new option that matches the current value, remember its index

                        var tableRows = "";
                        var allenvs = ""; //environment list

                        for (var i=0; i < config.envs.length; i++) {
                            var item = config.envs[i];
                            allenvs += (item.name + "\n");
                            var onerow = "<tr class='envtabledata'>" +
                                "<td>"+
                                "<input type='checkbox' " + (item.enabled ? "checked" : "") +" name='enabled_envs' value='" + item.name + "'/>" +
                                "</td>" +
                                "<td>"+
                                "<input type='radio' name='default_env' value='" + item.name + "' " + (i == 0 || item.def ? "checked" : "") + "/>" +
                                "</td>" +
                                "<td>"+
                                "<input type='checkbox' " + (item.autodeploy ? "checked" : "") + " name='autodeploy_envs' value='" + item.name + "'/>" +
                                "</td>" +
                                "<td style='width:115px;'>"+ item.name + "</td>" +
                                "<td>" +
                                "<select name='"+item.name+"' class='envcheckbox' >" +
                                "<option value='scm' " + (item.usedConfig == 'scm' ? "selected" : "") + ">scm</option>" +
                                "<option value='last'" + (item.usedConfig == 'last' ? "selected" : "") + ">last build</option>" +
                                "</select>" +
                                "</td>" +
                                "</tr>";
                            tableRows += onerow;
                            // add curent environment to Environment variable which will passed to server
                            m.append(new Option(item.name,item.name));
                        }
                        // add environments row
                        var envsRow = "<tr class='envtabledata_envlist' style='display:none;'><td><select name='envlist'><option value='" + allenvs + "'>v</option></select></td></tr>"
                        tableRows += envsRow;
                        jQuery('table[name="envtable"]').append(tableRows);


                         // if no value is explicitly selected by the server, try to select the same value
                        if (!selectionSet && possibleIndex!=null)
                            m.selectedIndex = possibleIndex;

                    }
                });
                // of previous developer - TO CLEANUP
                /*

                new Ajax.Request("/job/"+projectname+"/fillEnvrnItems", {
                timeout: 10000,
                onSuccess: function(rsp) {
                    console.log(rsp);
                    var m = jQuery('select[name="_.envrn"]');
                    var table = jQuery('table[name="envtable"]').html();
                    var currentSelection = m.value;

                    m.empty();

                    var selectionSet = false; // is the selection forced by the server?
                    var possibleIndex = null; // if there's a new option that matches the current value, remember its index
                    var opts = eval('('+rsp.responseText+')').values;
                    for( var i=0; i<opts.length; i++ ) {
                        jQuery('table[name="envtable"]')
                                .html(table +
                                "<tr class='envtabledata'>" +
                                    "<td>"+
                                        "<input type='checkbox' checked='true' name='enabled_envs' value='" + opts[i].name + "'/>" +
                                    "</td>" +
                                    "<td>"+
                                        "<input type='radio' name='default_env' value='" + opts[i].name + "'/>" +
                                    "</td>" +
                                    "<td>"+
                                        "<input type='checkbox' name='autodeploy_envs' value='" + opts[i].name + "'/>" +
                                    "</td>" +
                                    "<td style='width:115px;'>"+ opts[i].name + "</td>" +
                                    "<td>" +
                                        "<select name='"+opts[i].name+"' class='envcheckbox' >" +
                                            "<option value='scm'>scm</option>" +
                                            "<option value='last'>last build</option>" +
                                        "</select>" +
                                    "</td>" +
                                "</tr>");
                                
                        table = jQuery('table[name="envtable"]').html();
                        
                        m.append(new Option(opts[i].name,opts[i].value));
                        if(opts[i].selected) {
                            m.selectedIndex = i;
                            selectionSet = true;
                        }
                        if (opts[i].value==currentSelection)
                            possibleIndex = i;
                        }

                         // fill checkbox - old
                         new Ajax.Request("/job/"+projectname+"/fillConfigCheckBoxItems", {
                            timeout: 10000,
                            onSuccess: function(rsp) {
                                console.log(rsp);
                                //var m = jQuery('select[name="_.envrn"]');
                                //var table = jQuery('table[name="envtable"]').html();
                                //var currentSelection = m.value;

                                m.empty();

                                var selectionSet = false; // is the selection forced by the server?
                                var possibleIndex = null; // if there's a new option that matches the current value, remember its index
                                var opts = eval('('+rsp.responseText+')').values;
                                for( var i=0; i<opts.length; i++ ) {
                                    var l = jQuery('select[name="'+opts[i].name+'"]');
                                    if(opts[i].value=="scm")
                                        jQuery('select[name="'+opts[i].name+'"] option[value="scm"]').attr("selected", "selected" );
                                    else
                                         jQuery('select[name="'+opts[i].name+'"] option[value="last"]').attr("selected", "selected" );
                                        ///jQuery('select[name="'+opts[i].name+'"]').attr("checked", true );                          
                                }
                            }
                         });
                        // if no value is explicitly selected by the server, try to select the same value
                        if (!selectionSet && possibleIndex!=null)
                            m.selectedIndex = possibleIndex;
                        }
                });
                */

            }
      });
 // });

    /* temp commented
    jQuery('select[class="envcheckbox"]').live("change", function(){ 
        var temp;
        if(jQuery(this).attr("value")=='last')
            temp = true;
        else
            temp = false;
         new Ajax.Request("/job/"+projectname+"/updateConfiguration", {
                timeout: 10000,
                method: 'post',
                parameters: {
                    name: jQuery(this).attr("name"),
                    checked: temp
                },
                onSuccess: function(rsp) {
                
                }
                });
    });
    */

    jQuery('input[name="enabled_envs"]').live("change",function(){
        var len = jQuery('input[name="enabled_envs"]:checked').size();
        if (len == 0) {
            jQuery(this).attr('checked', 'true');
            alert("At least one environment must be selected");
        }
    });
});

//jQuery('input[name="test"]').click(function() {
//    alert( "Handler for .change() called." );
//    console.log("Handler for .change() called.");
//  });
//
