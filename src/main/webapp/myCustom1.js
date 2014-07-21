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

