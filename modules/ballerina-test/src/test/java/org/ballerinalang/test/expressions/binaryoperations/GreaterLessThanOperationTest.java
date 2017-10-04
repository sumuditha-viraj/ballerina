/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.expressions.binaryoperations;

import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.test.utils.BTestUtils;
import org.ballerinalang.test.utils.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GreaterLessThanOperationTest {

    CompileResult result;
    CompileResult resultNegative;

    @BeforeClass
    public void setup() {
        result = BTestUtils.compile("test-src/expressions/binaryoperations/greater-less-than-operation.bal");
        resultNegative = BTestUtils.
         compile("test-src/expressions/binaryoperations/greater-less-than-operation-negative.bal");
    }

    @Test(description = "Test int greater than, less than expression")
    public void testIntRangeExpr() {
        BValue[] args = {new BInteger(0)};
        BValue[] returns = BTestUtils.invoke(result, "testIntRanges", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BInteger.class);

        long actual = ((BInteger) returns[0]).intValue();
        long expected = 1;
        Assert.assertEquals(actual, expected);

        args = new BValue[]{new BInteger(50)};
        returns = BTestUtils.invoke(result, "testIntRanges", args);

        actual = ((BInteger) returns[0]).intValue();
        expected = 2;
        Assert.assertEquals(actual, expected);

        args = new BValue[]{new BInteger(200)};
        returns = BTestUtils.invoke(result, "testIntRanges", args);

        actual = ((BInteger) returns[0]).intValue();
        expected = 3;
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "Test float greater than, less than expression")
    public void testFloatRangeExpr() {
        BValue[] args = {new BFloat(-123.8f)};
        BValue[] returns = BTestUtils.invoke(result, "testFloatRanges", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BInteger.class);

        long actual = ((BInteger) returns[0]).intValue();
        long expected = 1;
        Assert.assertEquals(actual, expected);

        args = new BValue[]{new BFloat(75.4f)};
        returns = BTestUtils.invoke(result, "testFloatRanges", args);

        actual = ((BInteger) returns[0]).intValue();
        expected = 2;
        Assert.assertEquals(actual, expected);

        args = new BValue[]{new BFloat(321.45f)};
        returns = BTestUtils.invoke(result, "testFloatRanges", args);

        actual = ((BInteger) returns[0]).intValue();
        expected = 3;
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "Test Integer and long comparison")
    public void testIntAndFloatComparison() {
        int a = 10;
        float b = 20f;

        boolean expectedResult = a > b;

        BValue[] args = {new BInteger(a), new BFloat(b)};
        BValue[] returns = BTestUtils.invoke(result, "testIntAndFloatCompare", args);

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BBoolean.class);

        boolean actualResult = ((BBoolean) returns[0]).booleanValue();

        Assert.assertEquals(actualResult, expectedResult);
    }

    @Test
    public void testIntGTFloat() {
        BValue[] args = {new BInteger(110), new BFloat(22L)};
        BValue[] returns = BTestUtils.invoke(result, "intGTFloat", args);
        Assert.assertTrue(returns[0] instanceof BBoolean);
        final String expected = "true";
        Assert.assertEquals(returns[0].stringValue(), expected);
    }

    @Test
    public void testFloatGTInt() {
        BValue[] args = {new BFloat(110f), new BInteger(22)};
        BValue[] returns = BTestUtils.invoke(result, "floatGTInt", args);
        Assert.assertTrue(returns[0] instanceof BBoolean);
        final String expected = "true";
        Assert.assertEquals(returns[0].stringValue(), expected);
    }

    @Test(description = "Test binary statement with errors")
    public void testSubtractStmtNegativeCases() {
        Assert.assertEquals(resultNegative.getErrorCount(), 8);
        BTestUtils.validateError(resultNegative, 0, "operator '>' not defined for 'json' and 'json'", 7, 11);
        BTestUtils.validateError(resultNegative, 1, "operator '>=' not defined for 'json' and 'json'", 16, 11);
        BTestUtils.validateError(resultNegative, 2, "operator '<' not defined for 'json' and 'json'", 26, 11);
        BTestUtils.validateError(resultNegative, 3, "operator '<=' not defined for 'json' and 'json'", 35, 11);
        BTestUtils.validateError(resultNegative, 4, "operator '>' not defined for 'int' and 'string'", 41, 11);
        BTestUtils.validateError(resultNegative, 5, "operator '>=' not defined for 'int' and 'string'", 47, 11);
        BTestUtils.validateError(resultNegative, 6, "operator '<' not defined for 'int' and 'string'", 53, 11);
        BTestUtils.validateError(resultNegative, 7, "operator '<=' not defined for 'int' and 'string'", 59, 11);
    }
}
