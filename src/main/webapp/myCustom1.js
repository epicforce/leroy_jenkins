/* 
 * template to update select box dynamically after sucessfull update of LEROY_HOME
 */
jQuery(document).ready(function(){
  jQuery('input[name="_.target"]').attr("readonly","readonly");
  jQuery('input[name="_.target"]').attr("disabled","disabled");
//  jQuery('input[id="main-table"]').css({"background-image": "url(plugin/leroy/myCustom1.js)"});
  
  
  //on load refresh the two select boxes rows
  jQuery("input[id='getWo']").click(function(){
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
                new Ajax.Request("/job/"+projectname+"/fillEnvrnItems", {
                timeout: 10000,
                onSuccess: function(rsp) {
                    console.log(rsp);
                    var m = jQuery('select[name="_.envrn"]');
                    var currentSelection = m.value;

                    m.empty();

                    var selectionSet = false; // is the selection forced by the server?
                    var possibleIndex = null; // if there's a new option that matches the current value, remember its index
                    var opts = eval('('+rsp.responseText+')').values;
                    for( var i=0; i<opts.length; i++ ) {
                        m.append(new Option(opts[i].name,opts[i].value));
                        if(opts[i].selected) {
                            m.selectedIndex = i;
                            selectionSet = true;
                        }
                    if (opts[i].value==currentSelection)
                        possibleIndex = i;
                    }

                    // if no value is explicitly selected by the server, try to select the same value
                    if (!selectionSet && possibleIndex!=null)
                        m.selectedIndex = possibleIndex;
                    }
                });
            }
      });
  });
  
});


