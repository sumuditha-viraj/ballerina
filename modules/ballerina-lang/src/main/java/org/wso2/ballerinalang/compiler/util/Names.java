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
package org.wso2.ballerinalang.compiler.util;

import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;

/**
 * @since 0.94
 */
public class Names {

    public static final CompilerContext.Key<Names> NAMES_KEY =
            new CompilerContext.Key<>();

    public static final Name EMPTY = new Name("");
    public static final Name IGNORE = new Name("_");
    public static final Name INVALID = new Name("><");
    public static final Name DEFAULT_VERSION = new Name("0.0.0");
    public static final Name CAST_OP = new Name("(<type>)");
    public static final Name CONVERSION_OP = new Name("<<type>>");
    public static final Name STACK_FRAME = new Name("stackFrame");
    public static final Name CALLER = new Name("caller");
    public static final Name PACKAGE = new Name("package");
    public static final Name FILE_NAME = new Name("fileName");
    public static final Name LINE_NUMBER = new Name("lineNumber");
    public static final Name MSG = new Name("msg");
    public static final Name ERROR = new Name("error");
    public static final Name CAUSE = new Name("cause");
    public static final Name STACK_TRACE = new Name("stackTrace");
    public static final Name INIT_FUNCTION_SUFFIX = new Name(".<init>");
    public static final Name CONNECTOR = new Name("connector");

    public CompilerContext context;

    public static Names getInstance(CompilerContext context) {
        Names names = context.get(NAMES_KEY);
        if (names == null) {
            names = new Names(context);
            context.put(NAMES_KEY, names);
        }
        return names;
    }

    private Names(CompilerContext context) {
        this.context = context;
        this.context.put(NAMES_KEY, this);
    }

    public Name fromIdNode(BLangIdentifier identifier) {
        // identifier.value cannot be null
        return fromString(identifier.value);
    }

    public Name fromString(String value) {
        // value cannot be null
        if (value.equals("")) {
            return EMPTY;
        } else if (value.equals("_")) {
            return IGNORE;
        }
        return new Name(value);
    }

    public Name fromTypeKind(TypeKind typeKind) {
        return fromString(typeKind.typeName());
    }
}
