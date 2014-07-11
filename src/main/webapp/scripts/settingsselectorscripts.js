/*
 *  The MIT License
 *
 *  Copyright 2014 Sony Mobile Communications Inc. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

document.observe('dom:loaded', function() {
    var deleteButtons = $$('.repeatable-delete');

    deleteButtons.each( function(button) {
        button.onclick = clickDelete;
    });

});

//Adds the node property to list of removed items when a delete button is pressed.
function clickDelete(e) {
    var id = e.target.id;
    var grandParent = $(id).up(2);
    var input = grandParent.down('input[name="stapler-class"]');
    var className = input.value;
    var displayName = classToDisplayName[className];

    if (displayName != null) {
        var removeProperties = $$('button[suffix="removeProperties"]');

        if (removeProperties.length > 0) {

            //Id is on form "yui-genXX-button. We need the XX:
            var menuId = parseInt(removeProperties[0].id.replace("yui-gen", "").replace("-button", "")) + 1;
            var menu = YAHOO.widget.MenuManager.getMenu("yui-gen" + menuId);

            if (menu.getItems().length == 0) {
                //Trigger rendering of the menu since it lazily loaded:
                menu.show();
                menu.hide();
            }

            //Find the correct menu item to press:
            var menuItems = menu.getItems();
            menuItems.each(function (item) {
                if (item.element.innerHTML.indexOf(displayName) > -1) {
                    item.element.click();
                    throw $break;
                }
            });
        }
    }
}