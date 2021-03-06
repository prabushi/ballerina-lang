
/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.compiler.api.impl;

import io.ballerina.compiler.api.impl.symbols.BallerinaAnnotationSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaClassSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaConstantSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaFunctionSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaMethodSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaModule;
import io.ballerina.compiler.api.impl.symbols.BallerinaParameterSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaServiceSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaTypeDefinitionSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaVariableSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaWorkerSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaXMLNSSymbol;
import io.ballerina.compiler.api.impl.symbols.TypesFactory;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BClassSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BServiceSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BXMLNSSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFutureType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of factory methods to generate the {@link Symbol}s.
 *
 * @since 2.0.0
 */
public class SymbolFactory {

    private static final CompilerContext.Key<SymbolFactory> SYMBOL_FACTORY_KEY = new CompilerContext.Key<>();

    private final CompilerContext context;
    private final TypesFactory typesFactory;

    private SymbolFactory(CompilerContext context) {
        context.put(SYMBOL_FACTORY_KEY, this);
        this.context = context;
        this.typesFactory = TypesFactory.getInstance(context);
    }

    public static SymbolFactory getInstance(CompilerContext context) {
        SymbolFactory symbolFactory = context.get(SYMBOL_FACTORY_KEY);
        if (symbolFactory == null) {
            symbolFactory = new SymbolFactory(context);
        }

        return symbolFactory;
    }

    /**
     * Get the matching {@link Symbol} for a given {@link BSymbol}.
     *
     * @param symbol BSymbol to generated the BCompiled Symbol
     * @param name   symbol name
     * @return generated compiled symbol
     */
    public Symbol getBCompiledSymbol(BSymbol symbol, String name) {

        if (symbol == null) {
            throw new IllegalArgumentException("Symbol is 'null'");
        }

        if (symbol instanceof BVarSymbol) {
            if (symbol.kind == SymbolKind.FUNCTION) {
                if (Symbols.isFlagOn(symbol.flags, Flags.ATTACHED)) {
                    return createMethodSymbol((BInvokableSymbol) symbol, name);
                }
                return createFunctionSymbol((BInvokableSymbol) symbol, name);
            }
            if (symbol.kind == SymbolKind.ERROR_CONSTRUCTOR) {
                return createTypeDefinition(symbol.type.tsymbol, name);
            }
            if (symbol instanceof BConstantSymbol) {
                return createConstantSymbol((BConstantSymbol) symbol, name);
            }
            if (symbol.type instanceof BFutureType && ((BFutureType) symbol.type).workerDerivative) {
                return createWorkerSymbol((BVarSymbol) symbol, name);
            }
            if (symbol instanceof BServiceSymbol) {
                return createServiceSymbol((BServiceSymbol) symbol, name);
            }
            // return the variable symbol
            return createVariableSymbol((BVarSymbol) symbol, name);
        }

        if (symbol instanceof BTypeSymbol) {
            if (symbol.kind == SymbolKind.ANNOTATION) {
                return createAnnotationSymbol((BAnnotationSymbol) symbol);
            }
            if (symbol instanceof BPackageSymbol) {
                return createModuleSymbol((BPackageSymbol) symbol, name);
            }
            if (symbol instanceof BClassSymbol) {
                return createClassSymbol((BClassSymbol) symbol, name);
            }
            // create the typeDefs
            return createTypeDefinition((BTypeSymbol) symbol, name);
        }

        if (symbol.kind == SymbolKind.XMLNS) {
            return createXMLNamespaceSymbol((BXMLNSSymbol) symbol);
        }

        throw new IllegalArgumentException("Unsupported symbol type: " + symbol.getClass().getName());
    }

    /**
     * Create Function Symbol.
     *
     * @param invokableSymbol {@link BInvokableSymbol} to convert
     * @param name            symbol name
     * @return {@link Symbol} generated
     */
    public BallerinaFunctionSymbol createFunctionSymbol(BInvokableSymbol invokableSymbol, String name) {
        PackageID pkgID = invokableSymbol.pkgID;
        BallerinaFunctionSymbol.FunctionSymbolBuilder builder =
                new BallerinaFunctionSymbol.FunctionSymbolBuilder(name, pkgID, invokableSymbol);
        if (isFlagOn(invokableSymbol.flags, Flags.PUBLIC)) {
            builder.withQualifier(Qualifier.PUBLIC);
        }
        if (isFlagOn(invokableSymbol.flags, Flags.PRIVATE)) {
            builder.withQualifier(Qualifier.PRIVATE);
        }
        if (isFlagOn(invokableSymbol.flags, Flags.ISOLATED)) {
            builder.withQualifier(Qualifier.ISOLATED);
        }
        if (isFlagOn(invokableSymbol.flags, Flags.REMOTE)) {
            builder.withQualifier(Qualifier.REMOTE);
        }
        if (isFlagOn(invokableSymbol.flags, Flags.RESOURCE)) {
            builder.withQualifier(Qualifier.RESOURCE);
        }
        if (isFlagOn(invokableSymbol.flags, Flags.TRANSACTIONAL)) {
            builder.withQualifier(Qualifier.TRANSACTIONAL);
        }

        return builder.withTypeDescriptor((FunctionTypeSymbol) typesFactory.getTypeDescriptor(invokableSymbol.type))
                .build();
    }

    /**
     * Create Method Symbol.
     *
     * @param invokableSymbol {@link BInvokableSymbol} to convert
     * @param name            symbol name
     * @return {@link Symbol} generated
     */
    public BallerinaMethodSymbol createMethodSymbol(BInvokableSymbol invokableSymbol, String name) {
        TypeSymbol typeDescriptor = typesFactory.getTypeDescriptor(invokableSymbol.type);
        BallerinaFunctionSymbol functionSymbol = createFunctionSymbol(invokableSymbol, name);
        if (typeDescriptor.typeKind() == TypeDescKind.FUNCTION) {
            return new BallerinaMethodSymbol(functionSymbol);
        }

        throw new AssertionError("Invalid type descriptor found");
    }

    /**
     * Create a generic variable symbol.
     *
     * @param symbol {@link BVarSymbol} to convert
     * @param name   symbol name
     * @return {@link BallerinaVariableSymbol} generated
     */
    public BallerinaVariableSymbol createVariableSymbol(BVarSymbol symbol, String name) {
        PackageID pkgID = symbol.pkgID;
        BallerinaVariableSymbol.VariableSymbolBuilder symbolBuilder =
                new BallerinaVariableSymbol.VariableSymbolBuilder(name, pkgID, symbol);

        if (isFlagOn(symbol.flags, Flags.FINAL) || isFlagOn(symbol.flags, Flags.FUNCTION_FINAL)) {
            symbolBuilder.withQualifier(Qualifier.FINAL);
        }
        if (isFlagOn(symbol.flags, Flags.LISTENER)) {
            symbolBuilder.withQualifier(Qualifier.LISTENER);
        }
        if (isFlagOn(symbol.flags, Flags.READONLY)) {
            symbolBuilder.withQualifier(Qualifier.READONLY);
        }
        return symbolBuilder
                .withTypeDescriptor(typesFactory.getTypeDescriptor(symbol.type))
                .build();
    }

    public BallerinaWorkerSymbol createWorkerSymbol(BVarSymbol symbol, String name) {
        return new BallerinaWorkerSymbol.WorkerSymbolBuilder(name, symbol.pkgID, symbol)
                .withReturnType(typesFactory.getTypeDescriptor(((BFutureType) symbol.type).constraint))
                .build();
    }

    public BallerinaServiceSymbol createServiceSymbol(BServiceSymbol symbol, String name) {
        return new BallerinaServiceSymbol.ServiceSymbolBuilder(this.context, name, symbol.pkgID, symbol).build();
    }

    /**
     * Create a ballerina parameter.
     *
     * @param symbol Variable symbol for the parameter
     * @param kind   The kind of the parameter
     * @return {@link ParameterSymbol} generated parameter
     */
    public ParameterSymbol createBallerinaParameter(BVarSymbol symbol, ParameterKind kind) {
        if (symbol == null) {
            return null;
        }
        String name = symbol.getName().getValue().isBlank() ? null : symbol.getName().getValue();
        TypeSymbol typeDescriptor = typesFactory.getTypeDescriptor(symbol.getType());
        List<Qualifier> qualifiers = new ArrayList<>();
        if ((symbol.flags & Flags.PUBLIC) == Flags.PUBLIC) {
            qualifiers.add(Qualifier.PUBLIC);
        }
        return new BallerinaParameterSymbol(name, typeDescriptor, qualifiers, kind);
    }

    /**
     * Create a Ballerina Type Definition Symbol.
     *
     * @param typeSymbol type symbol to convert
     * @param name       symbol name
     * @return {@link}
     */
    public BallerinaTypeDefinitionSymbol createTypeDefinition(BTypeSymbol typeSymbol, String name) {
        BallerinaTypeDefinitionSymbol.TypeDefSymbolBuilder symbolBuilder =
                new BallerinaTypeDefinitionSymbol.TypeDefSymbolBuilder(name, typeSymbol.pkgID, typeSymbol);

        if (isFlagOn(typeSymbol.flags, Flags.PUBLIC)) {
            symbolBuilder.withQualifier(Qualifier.PUBLIC);
        }

        return symbolBuilder.withTypeDescriptor(typesFactory.getTypeDescriptor(typeSymbol.type, true)).build();
    }

    public BallerinaClassSymbol createClassSymbol(BClassSymbol classSymbol, String name) {
        TypeSymbol type = typesFactory.getTypeDescriptor(classSymbol.type, true);
        return createClassSymbol(classSymbol, name, type);
    }

    public BallerinaClassSymbol createClassSymbol(BClassSymbol classSymbol, String name, TypeSymbol type) {
        BallerinaClassSymbol.ClassSymbolBuilder symbolBuilder =
                new BallerinaClassSymbol.ClassSymbolBuilder(this.context, name, classSymbol.pkgID, classSymbol);

        if (isFlagOn(classSymbol.flags, Flags.PUBLIC)) {
            symbolBuilder.withQualifier(Qualifier.PUBLIC);
        }
        if (isFlagOn(classSymbol.flags, Flags.DISTINCT)) {
            symbolBuilder.withQualifier(Qualifier.DISTINCT);
        }
        if (isFlagOn(classSymbol.flags, Flags.CLIENT)) {
            symbolBuilder.withQualifier(Qualifier.CLIENT);
        }
        if (isFlagOn(classSymbol.flags, Flags.READONLY)) {
            symbolBuilder.withQualifier(Qualifier.READONLY);
        }

        if (type.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            type = ((TypeReferenceTypeSymbol) type).typeDescriptor();
        }

        return symbolBuilder.withTypeDescriptor((ObjectTypeSymbol) type).build();
    }

    /**
     * Create a constant symbol.
     *
     * @param constantSymbol BConstantSymbol to convert
     * @param name           symbol name
     * @return {@link BallerinaConstantSymbol} generated
     */
    public BallerinaConstantSymbol createConstantSymbol(BConstantSymbol constantSymbol, String name) {
        BallerinaConstantSymbol.ConstantSymbolBuilder symbolBuilder =
                new BallerinaConstantSymbol.ConstantSymbolBuilder(name, constantSymbol.pkgID, constantSymbol);
        symbolBuilder.withConstValue(constantSymbol.getConstValue())
                .withTypeDescriptor(typesFactory.getTypeDescriptor(constantSymbol.literalType));
        if ((constantSymbol.flags & Flags.PUBLIC) == Flags.PUBLIC) {
            symbolBuilder.withQualifier(Qualifier.PUBLIC);
        }

        return symbolBuilder.build();
    }

    /**
     * Creates an annotation Symbol.
     *
     * @param symbol Annotation symbol to convert
     * @return {@link BallerinaAnnotationSymbol}
     */
    public BallerinaAnnotationSymbol createAnnotationSymbol(BAnnotationSymbol symbol) {
        BallerinaAnnotationSymbol.AnnotationSymbolBuilder symbolBuilder =
                new BallerinaAnnotationSymbol.AnnotationSymbolBuilder(symbol.name.getValue(), symbol.pkgID, symbol);
        if ((symbol.flags & Flags.PUBLIC) == Flags.PUBLIC) {
            symbolBuilder.withQualifier(Qualifier.PUBLIC);
        }
        if (symbol.attachedType != null && symbol.attachedType.getType() != null) {
            symbolBuilder.withTypeDescriptor(typesFactory.getTypeDescriptor(symbol.attachedType.getType()));
        }

        return symbolBuilder.build();
    }

    /**
     * Creates an annotation Symbol.
     *
     * @param symbol Annotation symbol to convert
     * @return {@link BallerinaAnnotationSymbol}
     */
    public BallerinaXMLNSSymbol createXMLNamespaceSymbol(BXMLNSSymbol symbol) {
        BallerinaXMLNSSymbol.XmlNSSymbolBuilder symbolBuilder =
                new BallerinaXMLNSSymbol.XmlNSSymbolBuilder(symbol.name.getValue(), symbol.pkgID, symbol);

        return symbolBuilder.build();
    }

    /**
     * Create a module symbol.
     *
     * @param symbol Package Symbol to evaluate
     * @param name   symbol name
     * @return {@link BallerinaModule} symbol generated
     */
    public BallerinaModule createModuleSymbol(BPackageSymbol symbol, String name) {
        return new BallerinaModule.ModuleSymbolBuilder(this.context, name, symbol.pkgID, symbol).build();
    }

    // Private methods
    public static boolean isFlagOn(long mask, long flags) {
        return (mask & flags) == flags;
    }
}
