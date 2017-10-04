/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.test.types.globalvar;

import org.ballerinalang.test.utils.BTestUtils;
import org.ballerinalang.test.utils.CompileResult;
import org.testng.annotations.Test;

/**
 * Global variable error scenarios.
 */
public class GlobalVarErrorTest {

    @Test
    public void testStructFieldWithChildPackagePaths() {
        CompileResult resultNegative = BTestUtils.compile("test-src/types/globalvar/global-var-function-negative.bal");
        BTestUtils.validateError(resultNegative, 0, "struct child fields cannot have package identifiers: 'xyz:name'",
                                 6, 0);
    }
}
