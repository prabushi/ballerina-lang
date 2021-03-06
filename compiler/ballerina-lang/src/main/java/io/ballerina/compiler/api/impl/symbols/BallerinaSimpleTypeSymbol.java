/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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
package io.ballerina.compiler.api.impl.symbols;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.SimpleTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

/**
 * Represents the built-in simple type descriptor.
 *
 * @since 2.0.0
 */
public class BallerinaSimpleTypeSymbol extends AbstractTypeSymbol implements SimpleTypeSymbol {

    private final String typeName;

    public BallerinaSimpleTypeSymbol(CompilerContext context, ModuleID moduleID, BType bType) {
        super(context, TypesFactory.getTypeDescKind(bType.getKind()), moduleID, bType);
        this.typeName = bType.getKind().typeName();
    }

    public BallerinaSimpleTypeSymbol(CompilerContext context, ModuleID moduleID, String name, BType bType) {
        super(context, TypesFactory.getTypeDescKind(bType.getKind()), moduleID, bType);
        this.typeName = name;
    }

    @Override
    public String name() {
        return this.typeName;
    }

    @Override
    public String signature() {
        return this.typeName;
    }
}
