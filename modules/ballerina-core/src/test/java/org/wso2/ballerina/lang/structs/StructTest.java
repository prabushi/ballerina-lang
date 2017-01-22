/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.ballerina.lang.structs;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.ballerina.core.exception.BallerinaException;
import org.wso2.ballerina.core.exception.SemanticException;
import org.wso2.ballerina.core.model.BallerinaFile;
import org.wso2.ballerina.core.model.values.BInteger;
import org.wso2.ballerina.core.model.values.BMap;
import org.wso2.ballerina.core.model.values.BString;
import org.wso2.ballerina.core.model.values.BValue;
import org.wso2.ballerina.core.utils.ParserUtils;
import org.wso2.ballerina.lang.util.Functions;

/**
 * Test cases for user defined struct types in ballerina
 */
public class StructTest {

    private BallerinaFile bFile;
    
    @BeforeTest
    public void setup() {
        bFile = ParserUtils.parseBalFile("lang/structs/struct.bal");
    }
    
    @Test(description = "Test Basic struct operations")
    public void testBasicStruct() {
        BValue[] returns = Functions.invoke(bFile, "testCreateStruct");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "Jack");

        Assert.assertTrue(returns[1] instanceof BMap);
        BMap<BString, ?> adrsMap = ((BMap) returns[1]);
        Assert.assertEquals(adrsMap.get(new BString("country")), new BString("USA"));
        Assert.assertEquals(adrsMap.get(new BString("state")), new BString("CA"));

        Assert.assertTrue(returns[2] instanceof BInteger);
        Assert.assertEquals(((BInteger) returns[2]).intValue(), 25);
    }

    @Test(description = "Test using expressions as index for struct arrays")
    public void testExpressionAsIndex() {
        BValue[] returns = Functions.invoke(bFile, "testExpressionAsIndex");
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "Jane");
    }
    
    @Test(description = "Test struct operations inside a connector")
    public void testStructInConnector() {
        BValue[] returns = Functions.invoke(bFile, "testAction1");
        Assert.assertEquals(returns.length, 1);
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "Jack");
    }
    
    @Test(description = "Test using structs inside structs")
    public void testStructOfStructs() {
        BValue[] returns = Functions.invoke(bFile, "testStructOfStruct");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "USA");
    }
    
    @Test(description = "Test returning attributes of a struct")
    public void testReturnStructAttributes() {
        BValue[] returns = Functions.invoke(bFile, "testReturnStructAttributes");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "emily");
    }
    
    @Test(description = "Test using struct expression as a index in another struct expression")
    public void testStructExpressionAsIndex() {
        BValue[] returns = Functions.invoke(bFile, "testStructExpressionAsIndex");
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "emily");
    }
    
    /*
     *  Negative tests
     */
    
    @Test(description = "Test accessing an attribute of a noninitialized struct",
            expectedExceptions = {BallerinaException.class},
            expectedExceptionsMessageRegExp = "Cannot access attribute 'employees' of non-initialized variable 'dpt'.")
    public void testGetNonInitAttribute() {
        Functions.invoke(bFile, "testGetNonInitAttribute");
    }
    
    @Test(description = "Test accessing the attribute of a noninitialized struct",
            expectedExceptions = {BallerinaException.class},
            expectedExceptionsMessageRegExp = "Cannot access attribute 'employees' of non-initialized variable 'dpt'.")
    public void testGetNonInitLastAttribute() {
        Functions.invoke(bFile, "testGetNonInitLastAttribute");
    }
    
    @Test(description = "Test setting an attribute of a noninitialized struct",
            expectedExceptions = {BallerinaException.class},
            expectedExceptionsMessageRegExp = "Cannot access attribute 'employees' of non-initialized variable 'dpt'.")
    public void testSetNonInitAttribute() {
        Functions.invoke(bFile, "testGetNonInitAttribute");
    }
    
    @Test(description = "Test setting the attribute of a noninitialized struct",
            expectedExceptions = {BallerinaException.class},
            expectedExceptionsMessageRegExp = "Cannot set attribute 'dptName' of non-initialized variable 'dpt'.")
    public void testSetNonInitLastAttribute() {
        Functions.invoke(bFile, "testSetNonInitLastAttribute");
    }
    
    @Test(description = "Test defining structs with duplicate name",
            expectedExceptions = {SemanticException.class},
            expectedExceptionsMessageRegExp = "duplicate-structs.bal:7: Duplicate struct 'Department'.")
    public void testDuplicateStructDefinitions() {
        ParserUtils.parseBalFile("lang/structs/duplicate-structs.bal");
    }
    
    @Test(description = "Test initializing an undeclraed structs",
            expectedExceptions = {SemanticException.class},
            expectedExceptionsMessageRegExp = "undeclared-struct-init.bal:4: Struct 'Department' not found.")
    public void testUndeclaredStructInit() {
        ParserUtils.parseBalFile("lang/structs/undeclared-struct-init.bal");
    }
    
    @Test(description = "Test accessing an undeclared struct",
            expectedExceptions = {SemanticException.class},
            expectedExceptionsMessageRegExp = "undeclared-struct-access.bal:4: Undeclraed attribute 'dpt1'.")
    public void testUndeclaredStructAccess() {
        ParserUtils.parseBalFile("lang/structs/undeclared-struct-access.bal");
    }
    
    @Test(description = "Test accessing an undeclared attribute of a struct",
            expectedExceptions = {SemanticException.class},
            expectedExceptionsMessageRegExp = "undeclared-attribute-access.bal:5: Undeclraed attribute 'id'.")
    public void testUndeclaredAttributeAccess() {
        ParserUtils.parseBalFile("lang/structs/undeclared-attribute-access.bal");
    }
}
