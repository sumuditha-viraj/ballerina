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
package org.wso2.ballerinalang.programfile;

import org.wso2.ballerinalang.programfile.attributes.AnnotationAttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.AttributeInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code ServiceInfo} contains metadata of a Ballerina service entry in the program file.
 *
 * @since 0.87
 */
public class ServiceInfo extends StructureTypeInfo {

    public int protocolPkgPathCPIndex;

    public Map<String, ResourceInfo> resourceInfoMap = new HashMap<>();

    public FunctionInfo initFuncInfo;

    public ServiceInfo(int pkgPathCPIndex,
                       int nameCPIndex,
                       int protocolPkgPathCPIndex) {

        super(pkgPathCPIndex, nameCPIndex);
        this.protocolPkgPathCPIndex = protocolPkgPathCPIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkgNameCPIndex, nameCPIndex);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ServiceInfo
                && pkgNameCPIndex == (((ServiceInfo) obj).pkgNameCPIndex)
                && nameCPIndex == (((ServiceInfo) obj).nameCPIndex);
    }

    public AnnAttachmentInfo getAnnotationAttachmentInfo(String packageName, String annotationName) {
        AnnotationAttributeInfo attributeInfo = (AnnotationAttributeInfo) getAttributeInfo(
                AttributeInfo.Kind.ANNOTATIONS_ATTRIBUTE);
        if (attributeInfo == null || packageName == null || annotationName == null) {
            return null;
        }
        for (AnnAttachmentInfo annotationInfo : attributeInfo.getAttachmentInfoEntries()) {
            if (packageName.equals(annotationInfo.getPkgPath()) && annotationName.equals(annotationInfo.getName())) {
                return annotationInfo;
            }
        }
        return null;
    }
}