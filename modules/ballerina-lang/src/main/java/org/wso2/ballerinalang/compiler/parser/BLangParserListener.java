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
package org.wso2.ballerinalang.compiler.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.ballerinalang.model.Whitespace;
import org.ballerinalang.model.tree.CompilationUnitNode;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParserBaseListener;
import org.wso2.ballerinalang.compiler.util.QuoteType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BDiagnosticSource;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * @since 0.94
 */
public class BLangParserListener extends BallerinaParserBaseListener {
    protected static final String KEYWORD_PUBLIC = "public";
    protected static final String KEYWORD_NATIVE = "native";

    private BLangPackageBuilder pkgBuilder;
    private BDiagnosticSource diagnosticSrc;

    private List<String> pkgNameComps;
    private String pkgVersion;

    public BLangParserListener(CompilationUnitNode compUnit, BDiagnosticSource diagnosticSource) {
        this.pkgBuilder = new BLangPackageBuilder(compUnit);
        this.diagnosticSrc = diagnosticSource;
    }

    @Override
    public void enterParameterList(BallerinaParser.ParameterListContext ctx) {
        this.pkgBuilder.startVarList();
    }

    @Override
    public void exitParameter(BallerinaParser.ParameterContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.addVar(getCurrentPos(ctx), getWS(ctx), ctx.Identifier().getText(),
                false, ctx.annotationAttachment().size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCompilationUnit(BallerinaParser.CompilationUnitContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCompilationUnit(BallerinaParser.CompilationUnitContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitPackageDeclaration(BallerinaParser.PackageDeclarationContext ctx) {
        this.pkgBuilder.setPackageDeclaration(getCurrentPos(ctx), this.pkgNameComps, this.pkgVersion);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitPackageName(BallerinaParser.PackageNameContext ctx) {
        this.pkgNameComps = new ArrayList<>();
        ctx.Identifier().forEach(e -> pkgNameComps.add(e.getText()));
        this.pkgVersion = ctx.version() != null ? ctx.version().Identifier().getText() : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitImportDeclaration(BallerinaParser.ImportDeclarationContext ctx) {
        String alias = ctx.Identifier() != null ? ctx.Identifier().getText() : null;
        this.pkgBuilder.addImportPackageDeclaration(getCurrentPos(ctx), getWS(ctx),
                this.pkgNameComps, this.pkgVersion, alias);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterDefinition(BallerinaParser.DefinitionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitDefinition(BallerinaParser.DefinitionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterServiceDefinition(BallerinaParser.ServiceDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.startServiceDef(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitServiceDefinition(BallerinaParser.ServiceDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.endServiceDef(ctx.Identifier(0).getText(), ctx.Identifier(1).getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterServiceBody(BallerinaParser.ServiceBodyContext ctx) {
        this.pkgBuilder.startBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitServiceBody(BallerinaParser.ServiceBodyContext ctx) {
        this.pkgBuilder.addServiceBody();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterResourceDefinition(BallerinaParser.ResourceDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.startResourceDef();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitResourceDefinition(BallerinaParser.ResourceDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.endResourceDef(ctx.Identifier().getText(), ctx.annotationAttachment().size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCallableUnitBody(BallerinaParser.CallableUnitBodyContext ctx) {
        this.pkgBuilder.startBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCallableUnitBody(BallerinaParser.CallableUnitBodyContext ctx) {
        this.pkgBuilder.endCallableUnitBody(getWS(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterFunctionDefinition(BallerinaParser.FunctionDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        this.pkgBuilder.startFunctionDef();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitFunctionDefinition(BallerinaParser.FunctionDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean isReceiverAttached;
        if (ctx.parameter() != null) {
            isReceiverAttached = true;
        } else {
            isReceiverAttached = false;
        }

        int nativeKWTokenIndex = 0;
        boolean publicFunc = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        if (publicFunc) {
            nativeKWTokenIndex = 1;
        }
        boolean nativeFunc = KEYWORD_NATIVE.equals(ctx.getChild(nativeKWTokenIndex).getText());
        boolean bodyExists = ctx.callableUnitBody() != null;
        this.pkgBuilder.endFunctionDef(getCurrentPos(ctx), getWS(ctx), publicFunc, nativeFunc,
                bodyExists, isReceiverAttached);
    }

    @Override
    public void enterLambdaFunction(BallerinaParser.LambdaFunctionContext ctx) {
        this.pkgBuilder.startLambdaFunctionDef();
    }

    @Override
    public void exitLambdaFunction(BallerinaParser.LambdaFunctionContext ctx) {
        this.pkgBuilder.addLambdaFunctionDef(getCurrentPos(ctx), getWS(ctx), ctx.parameterList() != null,
                ctx.returnParameters() != null,
                ctx.returnParameters() != null && ctx.returnParameters().typeList() != null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCallableUnitSignature(BallerinaParser.CallableUnitSignatureContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCallableUnitSignature(BallerinaParser.CallableUnitSignatureContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        this.pkgBuilder.endCallableUnitSignature(getWS(ctx), ctx.Identifier().getText(),
                ctx.parameterList() != null, ctx.returnParameters() != null,
                ctx.returnParameters() != null ? ctx.returnParameters().typeList() != null : false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterConnectorDefinition(BallerinaParser.ConnectorDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        this.pkgBuilder.startConnectorDef();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitConnectorDefinition(BallerinaParser.ConnectorDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean publicConnector = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        this.pkgBuilder.endConnectorDef(getCurrentPos(ctx), ctx.Identifier().getText(), publicConnector);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterConnectorBody(BallerinaParser.ConnectorBodyContext ctx) {
        this.pkgBuilder.startConnectorBody();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitConnectorBody(BallerinaParser.ConnectorBodyContext ctx) {
        this.pkgBuilder.endConnectorBody();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterActionDefinition(BallerinaParser.ActionDefinitionContext ctx) {
        this.pkgBuilder.startActionDef();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitActionDefinition(BallerinaParser.ActionDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean nativeAction = KEYWORD_NATIVE.equals(ctx.getChild(0).getText());
        boolean bodyExists = ctx.callableUnitBody() != null;
        this.pkgBuilder.endActionDef(getCurrentPos(ctx), ctx.annotationAttachment().size(), nativeAction, bodyExists);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStructDefinition(BallerinaParser.StructDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        this.pkgBuilder.startStructDef();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStructDefinition(BallerinaParser.StructDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean publicStruct = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        this.pkgBuilder.endStructDef(getCurrentPos(ctx), ctx.Identifier().getText(), publicStruct);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStructBody(BallerinaParser.StructBodyContext ctx) {
        this.pkgBuilder.startVarList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStructBody(BallerinaParser.StructBodyContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationDefinition(BallerinaParser.AnnotationDefinitionContext ctx) {
        this.pkgBuilder.startAnnotationDef(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationDefinition(BallerinaParser.AnnotationDefinitionContext ctx) {
        this.pkgBuilder.endAnnotationDef(ctx.Identifier().getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterEnumDefinition(BallerinaParser.EnumDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        this.pkgBuilder.startEnumDef(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitEnumDefinition(BallerinaParser.EnumDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean publicEnum = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        this.pkgBuilder.endEnumDef(ctx.Identifier().getText(), publicEnum);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterEnumFieldList(BallerinaParser.EnumFieldListContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitEnumFieldList(BallerinaParser.EnumFieldListContext ctx) {
        List<String> identifierList = new ArrayList<>();
        ctx.Identifier().forEach(identifier -> identifierList.add(identifier.getText()));
        this.pkgBuilder.addEnumFieldList(identifierList);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterGlobalVariableDefinition(BallerinaParser.GlobalVariableDefinitionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitGlobalVariableDefinition(BallerinaParser.GlobalVariableDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean publicVar = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        this.pkgBuilder.addGlobalVariable(getCurrentPos(ctx), getWS(ctx),
                ctx.Identifier().getText(), ctx.expression() != null, publicVar);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterServiceAttachPoint(BallerinaParser.ServiceAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitServiceAttachPoint(BallerinaParser.ServiceAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterResourceAttachPoint(BallerinaParser.ResourceAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitResourceAttachPoint(BallerinaParser.ResourceAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterConnectorAttachPoint(BallerinaParser.ConnectorAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitConnectorAttachPoint(BallerinaParser.ConnectorAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterActionAttachPoint(BallerinaParser.ActionAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitActionAttachPoint(BallerinaParser.ActionAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterFunctionAttachPoint(BallerinaParser.FunctionAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitFunctionAttachPoint(BallerinaParser.FunctionAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTypemapperAttachPoint(BallerinaParser.TypemapperAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTypemapperAttachPoint(BallerinaParser.TypemapperAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStructAttachPoint(BallerinaParser.StructAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStructAttachPoint(BallerinaParser.StructAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterConstAttachPoint(BallerinaParser.ConstAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitConstAttachPoint(BallerinaParser.ConstAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterParameterAttachPoint(BallerinaParser.ParameterAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitParameterAttachPoint(BallerinaParser.ParameterAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttachPoint(BallerinaParser.AnnotationAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttachPoint(BallerinaParser.AnnotationAttachPointContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationBody(BallerinaParser.AnnotationBodyContext ctx) {
        this.pkgBuilder.startVarList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationBody(BallerinaParser.AnnotationBodyContext ctx) {
    }

    @Override
    public void exitConstantDefinition(BallerinaParser.ConstantDefinitionContext ctx) {
        if (ctx.exception != null) {
            return;
        }

        boolean publicVar = KEYWORD_PUBLIC.equals(ctx.getChild(0).getText());
        this.pkgBuilder.addConstVariable(getCurrentPos(ctx), getWS(ctx), ctx.Identifier().getText(), publicVar);
    }

    @Override
    public void enterWorkerDeclaration(BallerinaParser.WorkerDeclarationContext ctx) {
        this.pkgBuilder.startWorker();
    }

    @Override
    public void exitWorkerDeclaration(BallerinaParser.WorkerDeclarationContext ctx) {
        String workerName = null;
        if (ctx.workerDefinition() != null) {
            workerName = ctx.workerDefinition().Identifier().getText();
        }
        this.pkgBuilder.addWorker(getCurrentPos(ctx), workerName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterWorkerDefinition(BallerinaParser.WorkerDefinitionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitWorkerDefinition(BallerinaParser.WorkerDefinitionContext ctx) {
    }

    @Override
    public void exitTypeName(BallerinaParser.TypeNameContext ctx) {
        if (ctx.referenceTypeName() != null || ctx.valueTypeName() != null) {
            return;
        }
        if (ctx.typeName() != null) {
            // This ia an array Type.
            this.pkgBuilder.addArrayType(getCurrentPos(ctx), (ctx.getChildCount() - 1) / 2);
            return;
        }
        // This is 'any' type
        this.pkgBuilder.addValueType(getCurrentPos(ctx), getWS(ctx), ctx.getChild(0).getText());
    }

    @Override
    public void exitReferenceTypeName(BallerinaParser.ReferenceTypeNameContext ctx) {
    }

    @Override
    public void exitUserDefineTypeName(BallerinaParser.UserDefineTypeNameContext ctx) {
        this.pkgBuilder.addUserDefineType();
    }


    @Override
    public void exitValueTypeName(BallerinaParser.ValueTypeNameContext ctx) {
        this.pkgBuilder.addValueType(getCurrentPos(ctx), getWS(ctx), ctx.getText());
    }

    @Override
    public void exitBuiltInReferenceTypeName(BallerinaParser.BuiltInReferenceTypeNameContext ctx) {
        if (ctx.functionTypeName() != null) {
            return;
        }
        String typeName = ctx.getChild(0).getText();
        if (ctx.nameReference() != null) {
            this.pkgBuilder.addConstraintType(getCurrentPos(ctx), typeName);
        } else {
            this.pkgBuilder.addBuiltInReferenceType(getCurrentPos(ctx), typeName);
        }
    }

    @Override
    public void exitFunctionTypeName(BallerinaParser.FunctionTypeNameContext ctx) {
        boolean paramsAvail = false, paramsTypeOnly = false, retParamsAvail = false, retParamTypeOnly = false,
                returnsKeywordExists = false;
        if (ctx.parameterList() != null) {
            paramsAvail = ctx.parameterList().parameter().size() > 0;
        } else if (ctx.typeList() != null) {
            paramsAvail = ctx.typeList().typeName().size() > 0;
            paramsTypeOnly = true;
        }

        if (ctx.returnParameters() != null) {
            BallerinaParser.ReturnParametersContext returnCtx = ctx.returnParameters();
            returnsKeywordExists = "returns".equals(returnCtx.getChild(0).getText());
            if (returnCtx.parameterList() != null) {
                retParamsAvail = returnCtx.parameterList().parameter().size() > 0;
            } else if (returnCtx.typeList() != null) {
                retParamsAvail = returnCtx.typeList().typeName().size() > 0;
                retParamTypeOnly = true;
            }
        }

        this.pkgBuilder.addFunctionType(getCurrentPos(ctx), paramsAvail, paramsTypeOnly, retParamsAvail,
                retParamTypeOnly, returnsKeywordExists);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlNamespaceName(BallerinaParser.XmlNamespaceNameContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlNamespaceName(BallerinaParser.XmlNamespaceNameContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlLocalName(BallerinaParser.XmlLocalNameContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlLocalName(BallerinaParser.XmlLocalNameContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttachment(BallerinaParser.AnnotationAttachmentContext ctx) {
        this.pkgBuilder.startAnnotationAttachment(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttachment(BallerinaParser.AnnotationAttachmentContext ctx) {
        this.pkgBuilder.setAnnotationAttachmentName(ctx.nameReference().getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttributeList(BallerinaParser.AnnotationAttributeListContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttributeList(BallerinaParser.AnnotationAttributeListContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttribute(BallerinaParser.AnnotationAttributeContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttribute(BallerinaParser.AnnotationAttributeContext ctx) {
        String attrName = ctx.Identifier().getText();
        this.pkgBuilder.createAnnotAttachmentAttribute(getCurrentPos(ctx), attrName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttributeValue(BallerinaParser.AnnotationAttributeValueContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttributeValue(BallerinaParser.AnnotationAttributeValueContext ctx) {
        ParseTree childContext = ctx.getChild(0);
        if (childContext instanceof BallerinaParser.SimpleLiteralContext) {
            this.pkgBuilder.createLiteralTypeAttributeValue(getCurrentPos(ctx));
        } else if (childContext instanceof BallerinaParser.NameReferenceContext) {
            this.pkgBuilder.createVarRefTypeAttributeValue(getCurrentPos(ctx));
        } else if (childContext instanceof BallerinaParser.AnnotationAttachmentContext) {
            this.pkgBuilder.createAnnotationTypeAttributeValue(getCurrentPos(ctx));
        } else if (childContext instanceof BallerinaParser.AnnotationAttributeArrayContext) {
            this.pkgBuilder.createArrayTypeAttributeValue(getCurrentPos(ctx));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAnnotationAttributeArray(BallerinaParser.AnnotationAttributeArrayContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAnnotationAttributeArray(BallerinaParser.AnnotationAttributeArrayContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStatement(BallerinaParser.StatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStatement(BallerinaParser.StatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTransformStatement(BallerinaParser.TransformStatementContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.startTransformStmt();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTransformStatement(BallerinaParser.TransformStatementContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.createTransformStatement(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTransformStatementBody(BallerinaParser.TransformStatementBodyContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTransformStatementBody(BallerinaParser.TransformStatementBodyContext ctx) {
    }

    @Override
    public void enterExpressionAssignmentStatement(BallerinaParser.ExpressionAssignmentStatementContext ctx) {

    }

    @Override
    public void exitExpressionAssignmentStatement(BallerinaParser.ExpressionAssignmentStatementContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        boolean isVarDeclaration = false;
        if (ctx.getChild(0).getText().equals("var")) {
            isVarDeclaration = true;
        }
        this.pkgBuilder.addAssignmentStatement(getCurrentPos(ctx), isVarDeclaration);
    }

    @Override
    public void enterExpressionVariableDefinitionStatement(
            BallerinaParser.ExpressionVariableDefinitionStatementContext ctx) {

    }

    @Override
    public void exitExpressionVariableDefinitionStatement(
            BallerinaParser.ExpressionVariableDefinitionStatementContext ctx) {
        this.pkgBuilder.addVariableDefStatement(getCurrentPos(ctx),
                ctx.Identifier().getText(), ctx.ASSIGN() != null);

    }

    @Override
    public void exitVariableDefinitionStatement(BallerinaParser.VariableDefinitionStatementContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.addVariableDefStatement(getCurrentPos(ctx),
                ctx.Identifier().getText(), ctx.ASSIGN() != null);
    }

    @Override
    public void exitConnectorVarDefStatement(BallerinaParser.ConnectorVarDefStatementContext ctx) {
        if (ctx.exception != null) {
            return;
        }
        this.pkgBuilder.addConnectorVarDeclaration(getCurrentPos(ctx),
                ctx.Identifier().getText(), ctx.ASSIGN() != null);
    }

    @Override
    public void enterMapStructLiteral(BallerinaParser.MapStructLiteralContext ctx) {
        this.pkgBuilder.startMapStructLiteral();
    }

    @Override
    public void exitMapStructLiteral(BallerinaParser.MapStructLiteralContext ctx) {
        this.pkgBuilder.addMapStructLiteral(getCurrentPos(ctx));
    }

    @Override
    public void exitMapStructKeyValue(BallerinaParser.MapStructKeyValueContext ctx) {
        this.pkgBuilder.addKeyValueRecord();
    }

    @Override
    public void exitArrayLiteral(BallerinaParser.ArrayLiteralContext ctx) {
        boolean argsAvailable = ctx.expressionList() != null;
        this.pkgBuilder.addArrayInitExpr(getCurrentPos(ctx), argsAvailable);
    }

    @Override
    public void exitConnectorInitExpression(BallerinaParser.ConnectorInitExpressionContext ctx) {
        boolean argsAvailable = ctx.expressionList() != null;
        this.pkgBuilder.addConnectorInitExpression(getCurrentPos(ctx), argsAvailable);
    }

    @Override
    public void exitFilterInitExpression(BallerinaParser.FilterInitExpressionContext ctx) {
        boolean argsAvailable = ctx.expressionList() != null;
        this.pkgBuilder.addFilterConnectorInitExpression(getCurrentPos(ctx), argsAvailable);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAssignmentStatement(BallerinaParser.AssignmentStatementContext ctx) {
        boolean isVarDeclaration = false;
        if (ctx.getChild(0).getText().equals("var")) {
            isVarDeclaration = true;
        }
        this.pkgBuilder.addAssignmentStatement(getCurrentPos(ctx), isVarDeclaration);
    }

    @Override
    public void enterVariableReferenceList(BallerinaParser.VariableReferenceListContext ctx) {
        this.pkgBuilder.startExprNodeList();
    }

    @Override
    public void exitVariableReferenceList(BallerinaParser.VariableReferenceListContext ctx) {
        this.pkgBuilder.endExprNodeList(ctx.getChildCount() / 2 + 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterIfElseStatement(BallerinaParser.IfElseStatementContext ctx) {
        this.pkgBuilder.startIfElseNode(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitIfElseStatement(BallerinaParser.IfElseStatementContext ctx) {
        this.pkgBuilder.endIfElseNode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterIfClause(BallerinaParser.IfClauseContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitIfClause(BallerinaParser.IfClauseContext ctx) {
        this.pkgBuilder.addIfBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterElseIfClause(BallerinaParser.ElseIfClauseContext ctx) {
        // else-if clause is also modeled as an if-else statement
        this.pkgBuilder.startIfElseNode(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitElseIfClause(BallerinaParser.ElseIfClauseContext ctx) {
        this.pkgBuilder.addElseIfBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterElseClause(BallerinaParser.ElseClauseContext ctx) {
        this.pkgBuilder.startBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitElseClause(BallerinaParser.ElseClauseContext ctx) {
        this.pkgBuilder.addElseBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterIterateStatement(BallerinaParser.IterateStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitIterateStatement(BallerinaParser.IterateStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterWhileStatement(BallerinaParser.WhileStatementContext ctx) {
        this.pkgBuilder.startWhileStmt();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitWhileStatement(BallerinaParser.WhileStatementContext ctx) {
        this.pkgBuilder.addWhileStmt(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterContinueStatement(BallerinaParser.ContinueStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitContinueStatement(BallerinaParser.ContinueStatementContext ctx) {
        this.pkgBuilder.addContinueStatement(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterBreakStatement(BallerinaParser.BreakStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitBreakStatement(BallerinaParser.BreakStatementContext ctx) {
        this.pkgBuilder.addBreakStatement(getCurrentPos(ctx));
    }

    @Override
    public void enterForkJoinStatement(BallerinaParser.ForkJoinStatementContext ctx) {
        this.pkgBuilder.startForkJoinStmt();
    }

    @Override
    public void exitForkJoinStatement(BallerinaParser.ForkJoinStatementContext ctx) {
        this.pkgBuilder.addForkJoinStmt(getCurrentPos(ctx));
    }

    @Override
    public void enterJoinClause(BallerinaParser.JoinClauseContext ctx) {
        this.pkgBuilder.startJoinCause();
    }

    @Override
    public void exitJoinClause(BallerinaParser.JoinClauseContext ctx) {
        this.pkgBuilder.addJoinCause(ctx.Identifier().getText(), this.getWS(ctx));
    }

    @Override
    public void exitAnyJoinCondition(BallerinaParser.AnyJoinConditionContext ctx) {
        List<String> workerNames = new ArrayList<>();
        if (ctx.Identifier() != null) {
            workerNames = ctx.Identifier().stream().map(TerminalNode::getText).collect(Collectors.toList());
        }
        int joinCount = 0;
        if (ctx.IntegerLiteral() != null) {
            try {
                joinCount = Integer.valueOf(ctx.IntegerLiteral().getText());
            } catch (NumberFormatException ex) {
                // When ctx.IntegerLiteral() is not a string or missing, compilation fails due to NumberFormatException.
                // Hence catching the error and ignore. Still Parser complains about missing IntegerLiteral.
            }
        }
        this.pkgBuilder.addJoinCondition("SOME", workerNames, joinCount);
    }

    @Override
    public void exitAllJoinCondition(BallerinaParser.AllJoinConditionContext ctx) {
        List<String> workerNames = new ArrayList<>();
        if (ctx.Identifier() != null) {
            workerNames = ctx.Identifier().stream().map(TerminalNode::getText).collect(Collectors.toList());
        }
        this.pkgBuilder.addJoinCondition("ALL", workerNames, -1);
    }

    @Override
    public void enterTimeoutClause(BallerinaParser.TimeoutClauseContext ctx) {
        this.pkgBuilder.startTimeoutCause();
    }

    @Override
    public void exitTimeoutClause(BallerinaParser.TimeoutClauseContext ctx) {
        this.pkgBuilder.addTimeoutCause(ctx.Identifier().getText());
    }

    @Override
    public void enterTryCatchStatement(BallerinaParser.TryCatchStatementContext ctx) {
        this.pkgBuilder.startTryCatchFinallyStmt();
    }

    @Override
    public void exitTryCatchStatement(BallerinaParser.TryCatchStatementContext ctx) {
        this.pkgBuilder.addTryCatchFinallyStmt(getCurrentPos(ctx));
    }

    @Override
    public void enterCatchClauses(BallerinaParser.CatchClausesContext ctx) {
        this.pkgBuilder.addTryClause(getCurrentPos(ctx));
    }

    @Override
    public void enterCatchClause(BallerinaParser.CatchClauseContext ctx) {
        this.pkgBuilder.startCatchClause();
    }

    @Override
    public void exitCatchClause(BallerinaParser.CatchClauseContext ctx) {
        String paramName = ctx.Identifier().getText();
        this.pkgBuilder.addCatchClause(getCurrentPos(ctx), paramName);
    }

    @Override
    public void enterFinallyClause(BallerinaParser.FinallyClauseContext ctx) {
        this.pkgBuilder.startFinallyBlock();
    }

    @Override
    public void exitFinallyClause(BallerinaParser.FinallyClauseContext ctx) {
        this.pkgBuilder.addFinallyBlock(getCurrentPos(ctx));
    }

    @Override
    public void exitThrowStatement(BallerinaParser.ThrowStatementContext ctx) {
        this.pkgBuilder.addThrowStmt(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterReturnStatement(BallerinaParser.ReturnStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitReturnStatement(BallerinaParser.ReturnStatementContext ctx) {
        this.pkgBuilder.addReturnStatement(this.getCurrentPos(ctx), ctx.expressionList() != null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterReplyStatement(BallerinaParser.ReplyStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitReplyStatement(BallerinaParser.ReplyStatementContext ctx) {
    }

    @Override
    public void exitInvokeWorker(BallerinaParser.InvokeWorkerContext ctx) {
        this.pkgBuilder.addWorkerSendStmt(getCurrentPos(ctx), ctx.Identifier().getText(), false);
    }

    @Override
    public void exitInvokeFork(BallerinaParser.InvokeForkContext ctx) {
        this.pkgBuilder.addWorkerSendStmt(getCurrentPos(ctx), "FORK", true);
    }

    @Override
    public void exitWorkerReply(BallerinaParser.WorkerReplyContext ctx) {
        this.pkgBuilder.addWorkerReceiveStmt(getCurrentPos(ctx), ctx.Identifier().getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCommentStatement(BallerinaParser.CommentStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCommentStatement(BallerinaParser.CommentStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlAttribVariableReference(BallerinaParser.XmlAttribVariableReferenceContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlAttribVariableReference(BallerinaParser.XmlAttribVariableReferenceContext ctx) {
    }

    @Override
    public void exitSimpleVariableReference(BallerinaParser.SimpleVariableReferenceContext ctx) {
        this.pkgBuilder.createSimpleVariableReference(getCurrentPos(ctx));
    }

    @Override
    public void exitFunctionInvocationReference(BallerinaParser.FunctionInvocationReferenceContext ctx) {
        boolean argsAvailable = ctx.functionInvocation().expressionList() != null;
        this.pkgBuilder.createFunctionInvocation(getCurrentPos(ctx), argsAvailable);
    }

    @Override
    public void exitFieldVariableReference(BallerinaParser.FieldVariableReferenceContext ctx) {
        String fieldName = ctx.field().Identifier().getText();
        this.pkgBuilder.createFieldBasedAccessNode(getCurrentPos(ctx), fieldName);
    }

    @Override
    public void exitMapArrayVariableReference(BallerinaParser.MapArrayVariableReferenceContext ctx) {
        this.pkgBuilder.createIndexBasedAccessNode(getCurrentPos(ctx));
    }

    @Override
    public void exitInvocationReference(BallerinaParser.InvocationReferenceContext ctx) {
        boolean argsAvailable = ctx.invocation().expressionList() != null;
        String invocation = ctx.invocation().Identifier().getText();
        this.pkgBuilder.createInvocationNode(getCurrentPos(ctx), invocation, argsAvailable);
    }

    public void enterExpressionList(BallerinaParser.ExpressionListContext ctx) {
        this.pkgBuilder.startExprNodeList();
    }

    @Override
    public void exitExpressionList(BallerinaParser.ExpressionListContext ctx) {
        this.pkgBuilder.endExprNodeList(ctx.getChildCount() / 2 + 1);
    }

    @Override
    public void exitExpressionStmt(BallerinaParser.ExpressionStmtContext ctx) {
        this.pkgBuilder.addExpressionStmt(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTransactionStatement(BallerinaParser.TransactionStatementContext ctx) {
        this.pkgBuilder.startTransactionStmt();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTransactionStatement(BallerinaParser.TransactionStatementContext ctx) {
        this.pkgBuilder.endTransactionStmt(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTransactionHandlers(BallerinaParser.TransactionHandlersContext ctx) {
        this.pkgBuilder.addTransactionBlock(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTransactionHandlers(BallerinaParser.TransactionHandlersContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterFailedClause(BallerinaParser.FailedClauseContext ctx) {
        this.pkgBuilder.startFailedBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitFailedClause(BallerinaParser.FailedClauseContext ctx) {
        this.pkgBuilder.addFailedBlock(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAbortedClause(BallerinaParser.AbortedClauseContext ctx) {
        this.pkgBuilder.startAbortedBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAbortedClause(BallerinaParser.AbortedClauseContext ctx) {
        this.pkgBuilder.addAbortedBlock(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCommittedClause(BallerinaParser.CommittedClauseContext ctx) {
        this.pkgBuilder.startCommittedBlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCommittedClause(BallerinaParser.CommittedClauseContext ctx) {
        this.pkgBuilder.addCommittedBlock(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAbortStatement(BallerinaParser.AbortStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAbortStatement(BallerinaParser.AbortStatementContext ctx) {
        this.pkgBuilder.addAbortStatement(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterRetryStatement(BallerinaParser.RetryStatementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitRetryStatement(BallerinaParser.RetryStatementContext ctx) {
        this.pkgBuilder.addRetrytmt(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterNamespaceDeclaration(BallerinaParser.NamespaceDeclarationContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitNamespaceDeclaration(BallerinaParser.NamespaceDeclarationContext ctx) {
    }

    @Override
    public void exitBinaryDivMulModExpression(BallerinaParser.BinaryDivMulModExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    @Override
    public void exitBinaryOrExpression(BallerinaParser.BinaryOrExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlLiteralExpression(BallerinaParser.XmlLiteralExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlLiteralExpression(BallerinaParser.XmlLiteralExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterValueTypeTypeExpression(BallerinaParser.ValueTypeTypeExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitValueTypeTypeExpression(BallerinaParser.ValueTypeTypeExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterSimpleLiteralExpression(BallerinaParser.SimpleLiteralExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitSimpleLiteralExpression(BallerinaParser.SimpleLiteralExpressionContext ctx) {
    }

    @Override
    public void exitBinaryEqualExpression(BallerinaParser.BinaryEqualExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterArrayLiteralExpression(BallerinaParser.ArrayLiteralExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitArrayLiteralExpression(BallerinaParser.ArrayLiteralExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterBracedExpression(BallerinaParser.BracedExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitBracedExpression(BallerinaParser.BracedExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterVariableReferenceExpression(BallerinaParser.VariableReferenceExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitVariableReferenceExpression(BallerinaParser.VariableReferenceExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTypeCastingExpression(BallerinaParser.TypeCastingExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTypeCastingExpression(BallerinaParser.TypeCastingExpressionContext ctx) {
        this.pkgBuilder.createTypeCastExpr(getCurrentPos(ctx));
    }

    @Override
    public void exitBinaryAndExpression(BallerinaParser.BinaryAndExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    @Override
    public void exitBinaryAddSubExpression(BallerinaParser.BinaryAddSubExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterTypeConversionExpression(BallerinaParser.TypeConversionExpressionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTypeConversionExpression(BallerinaParser.TypeConversionExpressionContext ctx) {
        this.pkgBuilder.createTypeConversionExpr(getCurrentPos(ctx));
    }

    @Override
    public void exitBinaryCompareExpression(BallerinaParser.BinaryCompareExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    @Override
    public void exitUnaryExpression(BallerinaParser.UnaryExpressionContext ctx) {
        this.pkgBuilder.createUnaryExpr(getCurrentPos(ctx), ctx.getChild(0).getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitTernaryExpression(BallerinaParser.TernaryExpressionContext ctx) {
        this.pkgBuilder.createTernaryExpr(getCurrentPos(ctx));
    }

    @Override
    public void exitBinaryPowExpression(BallerinaParser.BinaryPowExpressionContext ctx) {
        this.pkgBuilder.createBinaryExpr(getCurrentPos(ctx), ctx.getChild(1).getText());
    }

    @Override
    public void exitNameReference(BallerinaParser.NameReferenceContext ctx) {
        if (ctx.Identifier().size() == 2) {
            String pkgName = ctx.Identifier(0).getText();
            String name = ctx.Identifier(1).getText();
            this.pkgBuilder.addNameReference(pkgName, name, getCurrentPos(ctx));
        } else {
            String name = ctx.Identifier(0).getText();
            this.pkgBuilder.addNameReference(null, name, getCurrentPos(ctx));
        }
    }

    @Override
    public void enterTypeList(BallerinaParser.TypeListContext ctx) {
        this.pkgBuilder.startProcessingTypeNodeList();
    }

    @Override
    public void exitTypeList(BallerinaParser.TypeListContext ctx) {
        this.pkgBuilder.endProcessingTypeNodeList(ctx.typeName().size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitParameterList(BallerinaParser.ParameterListContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterParameter(BallerinaParser.ParameterContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterFieldDefinition(BallerinaParser.FieldDefinitionContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitFieldDefinition(BallerinaParser.FieldDefinitionContext ctx) {
        this.pkgBuilder.addVar(getCurrentPos(ctx), getWS(ctx), ctx.Identifier().getText(),
                ctx.simpleLiteral() != null, 0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterSimpleLiteral(BallerinaParser.SimpleLiteralContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitSimpleLiteral(BallerinaParser.SimpleLiteralContext ctx) {
        TerminalNode node;

        DiagnosticPos pos = getCurrentPos(ctx);
        if ((node = ctx.IntegerLiteral()) != null) {
            this.pkgBuilder.addLiteralValue(pos, TypeTags.INT, Long.parseLong(node.getText()));
        } else if ((node = ctx.FloatingPointLiteral()) != null) {
            this.pkgBuilder.addLiteralValue(pos, TypeTags.FLOAT, Double.parseDouble(node.getText()));
        } else if ((node = ctx.BooleanLiteral()) != null) {
            this.pkgBuilder.addLiteralValue(pos, TypeTags.BOOLEAN, Boolean.parseBoolean(node.getText()));
        } else if ((node = ctx.QuotedStringLiteral()) != null) {
            String text = node.getText();
            text = text.substring(1, text.length() - 1);
            text = StringEscapeUtils.unescapeJava(text);
            this.pkgBuilder.addLiteralValue(pos, TypeTags.STRING, text);
        } else if ((node = ctx.NullLiteral()) != null) {
            this.pkgBuilder.addLiteralValue(pos, TypeTags.NULL, null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlLiteral(BallerinaParser.XmlLiteralContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlLiteral(BallerinaParser.XmlLiteralContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlItem(BallerinaParser.XmlItemContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlItem(BallerinaParser.XmlItemContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterContent(BallerinaParser.ContentContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitContent(BallerinaParser.ContentContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterComment(BallerinaParser.CommentContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitComment(BallerinaParser.CommentContext ctx) {
        Stack<String> stringFragments = getTemplateTextFragments(ctx.XMLCommentTemplateText());
        String endingString = getTemplateEndingStr(ctx.XMLCommentText());
        endingString = endingString.substring(0, endingString.length() - 3);
        this.pkgBuilder.createXMLCommentLiteral(getCurrentPos(ctx), stringFragments, endingString);

        if (ctx.getParent() instanceof BallerinaParser.ContentContext) {
            this.pkgBuilder.addChildToXMLElement();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterElement(BallerinaParser.ElementContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitElement(BallerinaParser.ElementContext ctx) {
        if (ctx.getParent() instanceof BallerinaParser.ContentContext) {
            this.pkgBuilder.addChildToXMLElement();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStartTag(BallerinaParser.StartTagContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStartTag(BallerinaParser.StartTagContext ctx) {
        this.pkgBuilder.startXMLElement(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterCloseTag(BallerinaParser.CloseTagContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitCloseTag(BallerinaParser.CloseTagContext ctx) {
        this.pkgBuilder.endXMLElement();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterEmptyTag(BallerinaParser.EmptyTagContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitEmptyTag(BallerinaParser.EmptyTagContext ctx) {
        this.pkgBuilder.startXMLElement(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterProcIns(BallerinaParser.ProcInsContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitProcIns(BallerinaParser.ProcInsContext ctx) {
        String targetQName = ctx.XML_TAG_SPECIAL_OPEN().getText();
        // removing the starting '<?' and the trailing whitespace
        targetQName = targetQName.substring(2, targetQName.length() - 1);

        Stack<String> textFragments = getTemplateTextFragments(ctx.XMLPITemplateText());
        String endingText = getTemplateEndingStr(ctx.XMLPIText());
        endingText = endingText.substring(0, endingText.length() - 2);

        this.pkgBuilder.createXMLPILiteral(getCurrentPos(ctx), targetQName, textFragments, endingText);

        if (ctx.getParent() instanceof BallerinaParser.ContentContext) {
            this.pkgBuilder.addChildToXMLElement();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterAttribute(BallerinaParser.AttributeContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitAttribute(BallerinaParser.AttributeContext ctx) {
        this.pkgBuilder.createXMLAttribute(getCurrentPos(ctx));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterText(BallerinaParser.TextContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitText(BallerinaParser.TextContext ctx) {
        Stack<String> textFragments = getTemplateTextFragments(ctx.XMLTemplateText());
        String endingText = getTemplateEndingStr(ctx.XMLText());
        if (ctx.getParent() instanceof BallerinaParser.ContentContext) {
            this.pkgBuilder.addXMLTextToElement(getCurrentPos(ctx), textFragments, endingText);
        } else {
            this.pkgBuilder.createXMLTextLiteral(getCurrentPos(ctx), textFragments, endingText);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlQuotedString(BallerinaParser.XmlQuotedStringContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlQuotedString(BallerinaParser.XmlQuotedStringContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlSingleQuotedString(BallerinaParser.XmlSingleQuotedStringContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlSingleQuotedString(BallerinaParser.XmlSingleQuotedStringContext ctx) {
        Stack<String> stringFragments = getTemplateTextFragments(ctx.XMLSingleQuotedTemplateString());
        String endingString = getTemplateEndingStr(ctx.XMLSingleQuotedString());
        this.pkgBuilder.createXMLQuotedLiteral(getCurrentPos(ctx), stringFragments, endingString,
                QuoteType.SINGLE_QUOTE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlDoubleQuotedString(BallerinaParser.XmlDoubleQuotedStringContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlDoubleQuotedString(BallerinaParser.XmlDoubleQuotedStringContext ctx) {
        Stack<String> stringFragments = getTemplateTextFragments(ctx.XMLDoubleQuotedTemplateString());
        String endingString = getTemplateEndingStr(ctx.XMLDoubleQuotedString());
        this.pkgBuilder.createXMLQuotedLiteral(getCurrentPos(ctx), stringFragments, endingString,
                QuoteType.DOUBLE_QUOTE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterXmlQualifiedName(BallerinaParser.XmlQualifiedNameContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitXmlQualifiedName(BallerinaParser.XmlQualifiedNameContext ctx) {
        if (ctx.expression() != null) {
            return;
        }

        List<TerminalNode> qnames = ctx.XMLQName();
        String prefix = null;
        String localname = null;

        if (qnames.size() > 1) {
            prefix = qnames.get(0).getText();
            localname = qnames.get(1).getText();
        } else {
            localname = qnames.get(0).getText();
        }

        this.pkgBuilder.createXMLQName(getCurrentPos(ctx), localname, prefix);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStringTemplateLiteral(BallerinaParser.StringTemplateLiteralContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStringTemplateLiteral(BallerinaParser.StringTemplateLiteralContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterStringTemplateContent(BallerinaParser.StringTemplateContentContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitStringTemplateContent(BallerinaParser.StringTemplateContentContext ctx) {
        Stack<String> stringFragments = getTemplateTextFragments(ctx.StringTemplateExpressionStart());
        String endingText = getTemplateEndingStr(ctx.StringTemplateText());
        this.pkgBuilder.createStringTemplateLiteral(getCurrentPos(ctx), stringFragments, endingText);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void visitTerminal(TerminalNode node) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     */
    @Override
    public void visitErrorNode(ErrorNode node) {
    }

    private DiagnosticPos getCurrentPos(ParserRuleContext ctx) {
        int startLine = ctx.getStart().getLine();
        int startCol = ctx.getStart().getCharPositionInLine();

        int endLine = -1;
        int endCol = -1;
        Token stop = ctx.getStop();
        if (stop != null) {
            endLine = stop.getLine();
            endCol = stop.getCharPositionInLine();
        }

        return new DiagnosticPos(diagnosticSrc, startLine, endLine, startCol, endCol);
    }

    protected Set<Whitespace> getWS(ParserRuleContext ctx) {
        return null;
    }

    private Stack<String> getTemplateTextFragments(List<TerminalNode> nodes) {
        Stack<String> templateStrFragments = new Stack<>();
        nodes.forEach(node -> {
            if (node == null) {
                templateStrFragments.push(null);
            } else {
                String str = node.getText();
                templateStrFragments.push(str.substring(0, str.length() - 2));
            }
        });
        return templateStrFragments;
    }

    private String getTemplateEndingStr(TerminalNode node) {
        return node == null ? null : node.getText();
    }
}
