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

package io.ballerina.semantic.api.test;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.BallerinaSemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.FieldSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.FutureTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.symbols.XMLTypeSymbol;
import org.ballerinalang.test.util.CompileResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.util.Collections;
import java.util.List;

import static io.ballerina.compiler.api.symbols.ParameterKind.DEFAULTABLE;
import static io.ballerina.compiler.api.symbols.ParameterKind.REQUIRED;
import static io.ballerina.compiler.api.symbols.ParameterKind.REST;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ANY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ANYDATA;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ARRAY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.DECIMAL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ERROR;
import static io.ballerina.compiler.api.symbols.TypeDescKind.FLOAT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.FUTURE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.INT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.JSON;
import static io.ballerina.compiler.api.symbols.TypeDescKind.MAP;
import static io.ballerina.compiler.api.symbols.TypeDescKind.NIL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.OBJECT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.READONLY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.RECORD;
import static io.ballerina.compiler.api.symbols.TypeDescKind.SINGLETON;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STREAM;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STRING;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TABLE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TUPLE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPEDESC;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPE_REFERENCE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.UNION;
import static io.ballerina.compiler.api.symbols.TypeDescKind.XML;
import static io.ballerina.semantic.api.test.util.SemanticAPITestUtils.compile;
import static io.ballerina.tools.text.LinePosition.from;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the types returned from the typeDescriptor() method in the API.
 *
 * @since 2.0.0
 */
public class TypedescriptorTest {

    SemanticModel model;

    @BeforeClass
    public void setup() {
        CompilerContext context = new CompilerContext();
        CompileResult result = compile("test-src/typedesc_test.bal", context);
        BLangPackage pkg = (BLangPackage) result.getAST();
        model = new BallerinaSemanticModel(pkg, context);
    }

    @Test
    public void testAnnotationType() {
        Symbol symbol = getSymbol(22, 37);
        TypeReferenceTypeSymbol type =
                (TypeReferenceTypeSymbol) ((AnnotationSymbol) symbol).typeDescriptor().get();
        assertEquals(type.typeDescriptor().typeKind(), TypeDescKind.RECORD);
    }

    @Test
    public void testConstantType() {
        Symbol symbol = getSymbol(16, 7);
        TypeSymbol type = ((ConstantSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), FLOAT);
    }

    @Test
    public void testFunctionType() {
        Symbol symbol = getSymbol(43, 12);
        FunctionTypeSymbol type = ((FunctionSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), TypeDescKind.FUNCTION);

        List<ParameterSymbol> parameters = type.parameters();
        assertEquals(parameters.size(), 2);
        validateParam(parameters.get(0), "x", REQUIRED, INT);

        validateParam(parameters.get(1), "y", DEFAULTABLE, FLOAT);

        ParameterSymbol restParam = type.restParam().get();
        validateParam(restParam, "rest", REST, ARRAY);

        TypeSymbol returnType = type.returnTypeDescriptor().get();
        assertEquals(returnType.typeKind(), INT);
    }

    @Test
    public void testFutureType() {
        Symbol symbol = getSymbol(45, 16);
        FutureTypeSymbol type = (FutureTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), FUTURE);
        assertEquals(type.typeParameter().get().typeKind(), INT);
    }

    @Test
    public void testArrayType() {
        Symbol symbol = getSymbol(47, 18);
        ArrayTypeSymbol type = (ArrayTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), ARRAY);
        assertEquals(((TypeReferenceTypeSymbol) type.memberTypeDescriptor()).typeDescriptor().typeKind(), OBJECT);
    }

    @Test
    public void testMapType() {
        Symbol symbol = getSymbol(49, 16);
        MapTypeSymbol type = (MapTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), MAP);
        assertEquals(type.typeParameter().get().typeKind(), STRING);
    }

    @Test
    public void testNilType() {
        Symbol symbol = getSymbol(38, 9);
        FunctionTypeSymbol type = ((FunctionSymbol) symbol).typeDescriptor();
        assertEquals(type.returnTypeDescriptor().get().typeKind(), NIL);
    }

    @Test
    public void testObjectType() {
        Symbol symbol = getSymbol(28, 6);
        ClassSymbol clazz = (ClassSymbol) symbol;
        assertEquals(clazz.typeKind(), OBJECT);

        List<FieldSymbol> fields = clazz.fieldDescriptors();
        FieldSymbol field = fields.get(0);
        assertEquals(fields.size(), 1);
        assertEquals(field.name(), "name");
        assertEquals(field.typeDescriptor().typeKind(), STRING);

        List<MethodSymbol> methods = clazz.methods();
        MethodSymbol method = methods.get(0);
        assertEquals(fields.size(), 1);
        assertEquals(method.name(), "getName");

        assertEquals(clazz.initMethod().get().name(), "init");
    }

    @Test
    public void testRecordType() {
        Symbol symbol = getSymbol(18, 5);
        RecordTypeSymbol type = (RecordTypeSymbol) ((TypeDefinitionSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), RECORD);
        assertFalse(type.inclusive());
        assertFalse(type.restTypeDescriptor().isPresent());

        List<FieldSymbol> fields = type.fieldDescriptors();
        FieldSymbol field = fields.get(0);
        assertEquals(fields.size(), 1);
        assertEquals(field.name(), "path");
        assertEquals(field.typeDescriptor().typeKind(), STRING);
    }

    @Test
    public void testTupleType() {
        Symbol symbol = getSymbol(51, 28);
        TupleTypeSymbol type = (TupleTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), TUPLE);

        List<TypeSymbol> members = type.memberTypeDescriptors();
        assertEquals(members.size(), 2);
        assertEquals(members.get(0).typeKind(), INT);
        assertEquals(members.get(1).typeKind(), STRING);

        assertTrue(type.restTypeDescriptor().isPresent());
        assertEquals(type.restTypeDescriptor().get().typeKind(), FLOAT);
    }

    @Test(dataProvider = "TypedescDataProvider")
    public void testTypedescType(int line, int col, TypeDescKind kind) {
        Symbol symbol = getSymbol(line, col);
        TypeDescTypeSymbol type = (TypeDescTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), TYPEDESC);
        assertTrue(type.typeParameter().isPresent());
        assertEquals(type.typeParameter().get().typeKind(), kind);
    }

    @DataProvider(name = "TypedescDataProvider")
    public Object[][] getTypedescPositions() {
        return new Object[][]{
                {53, 22, ANYDATA},
                {54, 13, UNION}
        };
    }

    @Test
    public void testUnionType() {
        Symbol symbol = getSymbol(56, 21);
        UnionTypeSymbol type = (UnionTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), UNION);

        List<TypeSymbol> members = type.memberTypeDescriptors();
        assertEquals(members.get(0).typeKind(), INT);
        assertEquals(members.get(1).typeKind(), STRING);
        assertEquals(members.get(2).typeKind(), FLOAT);
    }

    @Test(enabled = false)
    public void testNamedUnion() {
        Symbol symbol = getSymbol(58, 11);
        TypeReferenceTypeSymbol typeRef =
                (TypeReferenceTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(typeRef.typeKind(), TYPE_REFERENCE);

        UnionTypeSymbol type = (UnionTypeSymbol) typeRef.typeDescriptor();

        List<TypeSymbol> members = type.memberTypeDescriptors();
        assertEquals(members.get(0).typeKind(), INT);
        assertEquals(members.get(1).typeKind(), FLOAT);
        assertEquals(members.get(2).typeKind(), DECIMAL);
    }

    @Test(dataProvider = "FiniteTypeDataProvider")
    public void testFiniteType(int line, int column, List<String> expSignatures) {
        Symbol symbol = getSymbol(line, column);
        UnionTypeSymbol union = (UnionTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(union.typeKind(), UNION);

        List<TypeSymbol> members = union.memberTypeDescriptors();
        for (int i = 0; i < members.size(); i++) {
            TypeSymbol member = members.get(i);
            assertEquals(member.typeKind(), SINGLETON);
            assertEquals(member.signature(), expSignatures.get(i));
        }
    }

    @DataProvider(name = "FiniteTypeDataProvider")
    public Object[][] getFiniteTypePos() {
        return new Object[][]{
                {60, 10, List.of("0", "1", "2", "3")},
                {62, 11, List.of("default", "csv", "tdf")}
        };
    }

    @Test(dataProvider = "CommonTypesPosProvider")
    public void testCommonTypes(int line, int column, TypeDescKind kind) {
        VariableSymbol symbol = (VariableSymbol) getSymbol(line, column);
        assertEquals(symbol.typeDescriptor().typeKind(), kind);
    }

    @DataProvider(name = "CommonTypesPosProvider")
    public Object[][] getTypesPos() {
        return new Object[][]{
                {64, 9, JSON},
                {68, 13, READONLY},
                {70, 8, ANY},
                {71, 12, ANYDATA},
        };
    }

    @Test(dataProvider = "XMLPosProvider")
    public void testXML(int line, int column, TypeDescKind kind, String name) {
        VariableSymbol symbol = (VariableSymbol) getSymbol(line, column);
        TypeSymbol type = symbol.typeDescriptor();
        assertEquals(type.typeKind(), XML);
        assertEquals(((XMLTypeSymbol) type).typeParameter().get().typeKind(), kind);
        assertEquals(type.name(), name);
    }

    @DataProvider(name = "XMLPosProvider")
    public Object[][] getXMLTypePos() {
        return new Object[][]{
                {66, 8, UNION, "xml"},
                {91, 22, TYPE_REFERENCE, "xml<Element>"},
        };
    }

    @Test(dataProvider = "TablePosProvider")
    public void testTable(int line, int column, TypeDescKind rowTypeKind, String rowTypeName,
                          List<String> keySpecifiers, TypeDescKind keyConstraintTypeKind, String signature) {
        VariableSymbol symbol = (VariableSymbol) getSymbol(line, column);
        TypeSymbol type = symbol.typeDescriptor();
        assertEquals(type.typeKind(), TABLE);

        TableTypeSymbol tableType = (TableTypeSymbol) type;
        assertEquals(tableType.rowTypeParameter().typeKind(), rowTypeKind);
        assertEquals(tableType.rowTypeParameter().name(), rowTypeName);
        assertEquals(tableType.keySpecifiers(), keySpecifiers);
        tableType.keyConstraintTypeParameter().ifPresent(t -> assertEquals(t.typeKind(), keyConstraintTypeKind));
        assertEquals(type.signature(), signature);
    }

    @DataProvider(name = "TablePosProvider")
    public Object[][] getTableTypePos() {
        return new Object[][]{
                {73, 28, TYPE_REFERENCE, "Person", List.of("name"), null, "table<Person> key(name)"},
                {74, 18, TYPE_REFERENCE, "Person", Collections.emptyList(), null, "table<Person>"},
                {75, 30, TYPE_REFERENCE, "Person", Collections.emptyList(), STRING, "table<Person> key<string>"}
        };
    }

    @Test(dataProvider = "BuiltinTypePosProvider")
    public void testBuiltinSubtypes(int line, int column, TypeDescKind kind, String name) {
        Symbol symbol = getSymbol(line, column);
        TypeSymbol typeRef = ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(typeRef.typeKind(), TYPE_REFERENCE);

        TypeSymbol type = ((TypeReferenceTypeSymbol) typeRef).typeDescriptor();
        assertEquals(type.typeKind(), kind);
        assertEquals(type.name(), name);
    }

    @DataProvider(name = "BuiltinTypePosProvider")
    public Object[][] getBuiltinTypePos() {
        return new Object[][]{
                {77, 20, INT, "Unsigned32"},
                {78, 18, INT, "Signed32"},
                {79, 19, INT, "Unsigned8"},
                {80, 17, INT, "Signed8"},
                {81, 20, INT, "Unsigned16"},
                {82, 18, INT, "Signed16"},
                {84, 17, STRING, "Char"},
                {86, 17, XML, "Element"},
                {87, 31, XML, "ProcessingInstruction"},
                {88, 17, XML, "Comment"},
                {89, 14, XML, "Text"},
        };
    }

    @Test(dataProvider = "StreamTypePosProvider")
    public void testStreamType(int line, int column, TypeDescKind typeParamKind, TypeDescKind completionValueTypeKind) {
        Symbol symbol = getSymbol(line, column);
        TypeSymbol type = ((VariableSymbol) symbol).typeDescriptor();
        assertEquals(type.typeKind(), STREAM);

        StreamTypeSymbol streamType = (StreamTypeSymbol) type;
        assertEquals(streamType.typeParameter().typeKind(), typeParamKind);

        if (streamType.completionValueTypeParameter().isPresent()) {
            assertEquals(streamType.completionValueTypeParameter().get().typeKind(), completionValueTypeKind);
        } else {
            assertNull(completionValueTypeKind);
        }
    }

    @DataProvider(name = "StreamTypePosProvider")
    public Object[][] getStreamTypePos() {
        return new Object[][]{
                {93, 19, TYPE_REFERENCE, null},
                {94, 23, TYPE_REFERENCE, NIL},
                {95, 45, RECORD, ERROR}
        };
    }

    private Symbol getSymbol(int line, int column) {
        return model.symbol("typedesc_test.bal", from(line, column)).get();
    }

    private void validateParam(ParameterSymbol param, String name, ParameterKind kind, TypeDescKind typeKind) {
        assertEquals(param.name().get(), name);
        assertEquals(param.kind(), kind);
        assertEquals(param.typeDescriptor().typeKind(), typeKind);
    }
}
