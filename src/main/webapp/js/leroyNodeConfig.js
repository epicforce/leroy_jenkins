/**
 * Created by Dzmitry Bahdanovich on 27.07.14.
 */
jQuery(document).ready(function(){

//    setTimeout(function() {
//        jQuery('div[descriptorid="hudson.model.ChoiceParameterDefinition"]').each(function()
//            {
//                // hack - add timer for each such item for now
//                var name = jQuery(this).find('input[name="parameter.name"]');
//                if (name != null) {
//                    if (name.val() == "Target Configuration") {
//                        jQuery(this).css('style="visibility:hidden;display:none;"');
//                    }
//                }
//            }
//        )
//            ,
//            5000});

    //on load refresh the two select boxes rows
//    new Ajax.Request(jQuery("select[name='_.environment']").attr("fillurl"), {
//        timeout: 10000,
//        onSuccess: function(rsp) {
//            console.log(rsp);
//            var l = jQuery('select[name="_.environment"]');
//            var currentSelection = l.value;
//
//            l.empty();
//
//            var selectionSet = false; // is the selection forced by the server?
//            var possibleIndex = null; // if there's a new option that matches the current value, remember its index
//            var opts = eval('('+rsp.responseText+')').values;
//            for( var i=0; i<opts.length; i++ ) {
//                l.append(new Option(opts[i].name,opts[i].value));
//                if(opts[i].selected) {
//                    l.selectedIndex = i;
//                    selectionSet = true;
//                }
//                if (opts[i].value==currentSelection)
//                    possibleIndex = i;
//            }
//
//            // if no value is explicitly selected by the server, try to select the same value
//            if (!selectionSet && possibleIndex!=null)
//                l.selectedIndex = possibleIndex;
//        }
//    });
//
//    //get roles ajax
//    new Ajax.Request(jQuery("select[name='_.roles']").attr("fillurl"), {
//        timeout: 10000,
//        onSuccess: function(rsp) {
//            console.log(rsp);
//            var l = jQuery('select[name="_.roles"]');
//            var currentSelection = l.value;
//
//            l.empty();
//
//            var selectionSet = false; // is the selection forced by the server?
//            var possibleIndex = null; // if there's a new option that matches the current value, remember its index
//            var opts = eval('('+rsp.responseText+')').values;
//            for( var i=0; i<opts.length; i++ ) {
//                l.append(new Option(opts[i].name,opts[i].value));
//                if(opts[i].selected) {
//                    l.selectedIndex = i;
//                    selectionSet = true;
//                }
//                if (opts[i].value==currentSelection)
//                    possibleIndex = i;
//            }
//
//            // if no value is explicitly selected by the server, try to select the same value
//            if (!selectionSet && possibleIndex!=null)
//                l.selectedIndex = possibleIndex;
//        }
//    });
//
//
//    //make the text box visible or invisible
//    jQuery('select[name="_.roles"]').change(function(){
//        if(jQuery(this).val() == '<NEW ROLE>')
//        {
//            var agentname = jQuery(this).attr('agentname');
//            console.log(jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').attr('agentname'));
//            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('display', 'inline');
//            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('visibility', 'visible');
//
//        }
//        else{
//            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('display', 'none');
//            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('visibility', 'hidden');
//        }
//    });
//
//    jQuery('button[name="_.addrole"]').click(function(){
//        var agentname = jQuery(this).attr('agentname');
//        console.log(jQuery('select[name="_.roles"][agentname="'+agentname+'"]').val());
//        if(jQuery('select[name="_.roles"][agentname="'+agentname+'"]').val() == '<NEW ROLE>')
//        {
//            if(jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').val()!='')
//            {
//                var data = {
//                    agentname:agentname ,
//                    environmentname: jQuery('select[name="_.environment"][agentname="'+agentname+'"]').val( ),
//                    rolename: jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').val( ),
//                    leroyhome: jQuery('input[name="_.leroyhome"]').val( )
//                };
//                console.log(data);
//                new Ajax.Request(jQuery("button[name='_.addrole']").attr("addurl"), {
//                    timeout: 10000,
//                    parameters: data,
//                    onComplete: function(rsp) {
//
//                        jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('display', 'inline');
//                        jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('visibility', 'visible');
//                    }
//                });
//            }
//        }
//        else{
////            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('display', 'none');
////            jQuery('input[name="_.rolename"][agentname="'+agentname+'"]').css('visibility', 'hidden');
//        }
//    });
//    // focusout function
//    jQuery('input[name="_.leroyhome"]').focusout(function(){
//        new Ajax.Request(jQuery("select[name='_.environment']").attr("fillurl"), {
//            timeout: 10000,
//            onSuccess: function(rsp) {
//                console.log(rsp);
//                var l = jQuery('select[name="_.environment"]');
//                var currentSelection = l.value;
//
//                l.empty();
//
//                var selectionSet = false; // is the selection forced by the server?
//                var possibleIndex = null; // if there's a new option that matches the current value, remember its index
//                var opts = eval('('+rsp.responseText+')').values;
//                for( var i=0; i<opts.length; i++ ) {
//                    l.append(new Option(opts[i].name,opts[i].value));
//                    if(opts[i].selected) {
//                        l.selectedIndex = i;
//                        selectionSet = true;
//                    }
//                    if (opts[i].value==currentSelection)
//                        possibleIndex = i;
//                }
//
//                // if no value is explicitly selected by the server, try to select the same value
//                if (!selectionSet && possibleIndex!=null)
//                    l.selectedIndex = possibleIndex;
//            }
//        });
//        //get roles ajax
//
//        new Ajax.Request(jQuery("select[name='_.roles']").attr("fillurl"), {
//            timeout: 10000,
//            onSuccess: function(rsp) {
//                console.log(rsp);
//                var l = jQuery('select[name="_.roles"]');
//                var currentSelection = l.value;
//
//                l.empty();
//
//                var selectionSet = false; // is the selection forced by the server?
//                var possibleIndex = null; // if there's a new option that matches the current value, remember its index
//                var opts = eval('('+rsp.responseText+')').values;
//                for( var i=0; i<opts.length; i++ ) {
//                    l.append(new Option(opts[i].name,opts[i].value));
//                    if(opts[i].selected) {
//                        l.selectedIndex = i;
//                        selectionSet = true;
//                    }
//                    if (opts[i].value==currentSelection)
//                        possibleIndex = i;
//                }
//
//                // if no value is explicitly selected by the server, try to select the same value
//                if (!selectionSet && possibleIndex!=null)
//                    l.selectedIndex = possibleIndex;
//            }
//        });
//
//    });

    jQuery('input[name="installViaSshCheckbox"]').click(function()
    {
        if (jQuery(this).is(":checked")) {
            jQuery('input[name="_.sshInstall"]').val("true");
            jQuery('input[name="_.sshInstall"]').style("display: none;");
        } else {
            jQuery('input[name="_.sshInstall"]').val("false");
        }
    });

    /**
     * This method check if agent id is unique in current environment
     * TODO: this method doesn't work with repeated-chunks created by user: need to fugure out how to deal with DOM tree changes caused by hudson js
     */
    jQuery('input[name="_.id"][class="agentId"]').focusout(function(){
        var validationFailed = false;
        var currInput = jQuery(this);
        var currText = currInput.val();
        // if empty
        if (!currText) {
            validationFailed = true;
        }
        // we need to check the uniqueness among all inputs in current environment
        // hence, find the closes div class="repeated-container" ant take it id input fields
        if (!validationFailed) {
            jQuery(this).closest('div[class="repeated-container"]').find('input[name="_.id"][class="agentId"]').not(currInput).each(function(){
                if (jQuery(this).val() == currText) {
                    validationFailed = true;
                    return false; //break each()
                }
            });
        }
        if (validationFailed) {
            jQuery(currInput).css("background-color","#cc6666");
        } else {
            jQuery(currInput).css("background-color","white");
        }
    });

    jQuery('input[class="roleTags"]').tagsInput({
        'defaultText':'Add a Role',
        'width':'100%',
        'height':'15px'
    });


    // this is a temp solution to set a node name to node property
    var nodeNameSet = false;
    function setNodeName() {
        if (!nodeNameSet){
            var pathName = window.location.pathname;
            var to = pathName.lastIndexOf("/configure");
            if (to > -1) {
                var from = pathName.indexOf("/computer/");
                if (jQuery('input[name="_.nodeName"]') != null) {
                    jQuery('input[name="_.nodeName"]').val( pathName.substring(from+10, to) );
                    nodeNameSet = true;
                }
            }
        }
    }
    setNodeName();
    setInterval(setNodeName, 200);

});


