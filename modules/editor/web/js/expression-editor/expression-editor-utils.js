/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
define(['require', 'lodash', 'jquery'], function (require, _, $) {
    var expressionEditorUtil = {};
    expressionEditorUtil.createEditor = function (editorWrapper, wrapperClass, property) {
        var propertyWrapper = $("<div/>", {
            "class": wrapperClass
        }).appendTo(editorWrapper);

        var propertyValue = _.isNil(property.getterMethod.call(property.model)) ? "" : property.getterMethod.call(property.model);
        var propertyInputValue = $("<input type='text' value=''>").appendTo(propertyWrapper);
        $(propertyInputValue).focus();
        $(propertyInputValue).val(propertyValue);
        $(propertyInputValue).on("change paste keyup", function () {
            property.setterMethod.call(property.model, $(this).val());
            //var textValToUpdate = (($(this).val().length) > 11 ? ($(this).val().substring(0,11) + '...') : $(this).val());
            property.model.trigger('update-property-text', $(this).val(), property.key);
        });
    };
    return expressionEditorUtil;
});