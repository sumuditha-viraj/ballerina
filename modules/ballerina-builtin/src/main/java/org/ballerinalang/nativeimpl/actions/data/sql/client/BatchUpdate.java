/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.nativeimpl.actions.data.sql.client;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.nativeimpl.actions.data.sql.Constants;
import org.ballerinalang.nativeimpl.actions.data.sql.SQLDatasource;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.Attribute;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.BallerinaAnnotation;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.natives.connectors.AbstractNativeAction;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.osgi.service.component.annotations.Component;

/**
 * {@code BatchUpdate} is the Batch update action implementation of the SQL Connector.
 *
 * @since 0.8.6
 */
@BallerinaAction(
        packageName = "ballerina.data.sql",
        actionName = "batchUpdate",
        connectorName = Constants.CONNECTOR_NAME,
        args = {@Argument(name = "c", type = TypeKind.CONNECTOR),
                @Argument(name = "query", type = TypeKind.STRING),
                @Argument(name = "parameters",
                          type = TypeKind.ARRAY,
                          elementType = TypeKind.STRUCT,
                          arrayDimensions = 2,
                          structType = "Parameter")
        },
        returnType = { @ReturnType(type = TypeKind.ARRAY, elementType = TypeKind.INT) },
        connectorArgs = {
                @Argument(name = "options", type = TypeKind.MAP)
        })
@BallerinaAnnotation(annotationName = "Description", attributes = {@Attribute(name = "value",
        value = "The call action implementation for SQL connector.") })
@BallerinaAnnotation(annotationName = "Param", attributes = {@Attribute(name = "c",
        value = "Connector")})
@BallerinaAnnotation(annotationName = "Param", attributes = {@Attribute(name = "query",
        value = "String")})
@BallerinaAnnotation(annotationName = "Param", attributes = {@Attribute(name = "parameters",
        value = "Parameter array")})
@BallerinaAnnotation(annotationName = "Return", attributes = {@Attribute(name = "updatedCounts",
        value = "Array of update counts") })
@Component(
        name = "action.data.sql.BatchUpdate",
        immediate = true,
        service = AbstractNativeAction.class)
public class BatchUpdate extends AbstractSQLAction {
    @Override
    public BValue execute(Context context) {
        BConnector bConnector = (BConnector) getRefArgument(context, 0);
        String query = getStringArgument(context, 0);
        BRefValueArray parameters = (BRefValueArray) getRefArgument(context, 1);
        BMap sharedMap = (BMap) bConnector.getRefField(1);
        SQLDatasource datasource = null;
        if (sharedMap.get(new BString(Constants.DATASOURCE_KEY)) != null) {
            datasource = (SQLDatasource) sharedMap.get(new BString(Constants.DATASOURCE_KEY));
        } else {
            throw new BallerinaException("Datasource have not been initialized properly at " +
                    "Init native action invocation.");
        }
        executeBatchUpdate(context, datasource, query, parameters);
        return null;
    }
}