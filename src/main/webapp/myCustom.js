/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
jQuery(document).ready(function(){
  jQuery('input[name="_.leroyhome"]').focusout(function(){
    console.log(jQuery('input[name="_.goalType"]'));
            jQuery('input[name="_.goalType"]').select1("refresh");

  });
});


