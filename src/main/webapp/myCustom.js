/* 
 * template to update select box dynamically after sucessfull update of LEROY_HOME
 */
jQuery(document).ready(function(){
  jQuery('input[name="_.leroyhome"]').focusout(function(){
       new Ajax.Request(jQuery("select[name='_.environment']").attr("fillurl"), {
            timeout: 10000,
            onSuccess: function(rsp) {
                console.log(rsp);
                var l = jQuery('select[name="_.environment"]');
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
     
      
   });
});


