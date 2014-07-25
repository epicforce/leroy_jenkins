/* 
 * template to update select box dynamically after sucessfull update of LEROY_HOME
 */
jQuery(document).ready(function(){
  jQuery('input[name="_.target"]').attr("readonly","readonly");
  jQuery('input[name="_.target"]').attr("disabled","disabled");

    jQuery('input[name="_.autoDeploy"]').each(function()
    {
        jQuery(this).change(function()
        {
            jQuery('input[name="_.autoDeploy"]').attr('checked',false);
            jQuery(this).attr('checked',true);
        });
    });


//    <tr><td class="setting-leftspace">&nbsp;</td><td class="setting-name">Is Leroy Property</td><td class="setting-main"><input name="parameter.defaultValue" class=" " type="checkbox"></td><td class="setting-help"><a class="help-button" href="#" helpurl="/help/parameter/boolean-default.html" tabindex="9999"><img height="16" alt="Help for feature: Default Value" width="16" src="/static/74b91e88/images/16x16/help.png"></a></td></tr>

//    jQuery('input[name="_.autoDeploy"]').live("change",function(){
//        var checkboxes = jQuery('input[name="_.autoDeploy"]:checked');
//        var len = checkboxes.size();
//        var currentCheckbox = jQuery(this);
//        // try to uncheck the last checked checkbox
//        if (len == 0) {
//            currentCheckbox.attr('checked', 'true');
//            alert("At least one target environment/workflow combination must be selected");
//        } else {
//            for (var i = 0; i < len; i++) {
//                if (checkboxes[i] != currentCheckbox) {
//                    checkboxes[i].attr('checked', 'false');
//                }
//            }
//        }
//    });

});

