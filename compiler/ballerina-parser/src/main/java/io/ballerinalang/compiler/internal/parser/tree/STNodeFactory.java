/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerinalang.compiler.internal.parser.tree;

/**
 * A factory that constructs internal tree nodes.
 * <p>
 * This class contains various helper methods that create internal tree nodes.
 * <p>
 * Note that {@code STNodeFactory} must be used to create {@code STNode} instances. This approach allows
 * us to manage {@code STNode} production in the future. We could load nodes from a cache or add debug logs etc.
 *
 * @since 1.3.0
 */
public class STNodeFactory {

    private STNodeFactory() {
    }

    public static STNode makeFunctionDefinition(STNode visibilityQualifier,
                                         STNode functionKeyword,
                                         STNode functionName,
                                         STNode openParenToken,
                                         STNode parameters,
                                         STNode closeParenToken,
                                         STNode returnTypeDesc,
                                         STNode functionBody) {

        return new STFunctionDefinition(visibilityQualifier, functionKeyword, functionName,
                openParenToken, parameters, closeParenToken, returnTypeDesc, functionBody);
    }

}
