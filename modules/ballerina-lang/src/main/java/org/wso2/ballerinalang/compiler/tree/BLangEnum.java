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


package org.wso2.ballerinalang.compiler.tree;

import org.ballerinalang.model.tree.EnumNode;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.tree.NodeKind;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 0.94
 */
public class BLangEnum extends BLangNode implements EnumNode {

    public BLangIdentifier name;
    public List<BLangIdentifier> enumFields;

    public BLangEnum() {
        this.enumFields = new ArrayList<>();
    }

    @Override
    public BLangIdentifier getName() {
        return name;
    }

    @Override
    public void setName(IdentifierNode name) {
        this.name = (BLangIdentifier) name;
    }

    @Override
    public List<BLangIdentifier> getEnumFields() {
        return enumFields;
    }

    public void addEnumField(IdentifierNode enumField) {
        this.enumFields.add((BLangIdentifier) enumField);
    }

    @Override
    public void accept(BLangNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.ENUM;
    }

    @Override
    public String toString() {
        return "BLangEnum: " + this.name + " -> " + this.enumFields;
    }
}