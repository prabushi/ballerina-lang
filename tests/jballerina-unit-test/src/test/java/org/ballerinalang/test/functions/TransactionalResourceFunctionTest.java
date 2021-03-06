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
package org.ballerinalang.test.functions;

import org.ballerinalang.core.model.values.BValue;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class contains tests that are related to resource function with transactional keyword.
 */
public class TransactionalResourceFunctionTest {

    private CompileResult compileResult;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/functions/transactional_resource_functions.bal");
    }

    //TODO: Enable once the ballerinai-transaction PRs are merged
    @Test(description = "Test transactional resource functions", enabled = false)
    public void testTransactionalResourceFunc() {
        BValue[] result = BRunUtil.invoke(compileResult, "test");
        Assert.assertEquals(result.length, 2, "expected two return type");
        Assert.assertNotNull(result[0]);
        Assert.assertNotNull(result[1]);
        Assert.assertEquals(result[0].stringValue(), "1");
        Assert.assertEquals(result[1].stringValue(), "-1");
    }
}
