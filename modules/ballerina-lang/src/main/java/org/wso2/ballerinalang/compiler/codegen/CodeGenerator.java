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
package org.wso2.ballerinalang.compiler.codegen;

import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.TopLevelNode;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAction;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotAttribute;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangConnector;
import org.wso2.ballerinalang.compiler.tree.BLangEnum;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangResource;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangStruct;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.BLangWorker;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAttachmentAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAttachmentAttributeValue;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess.BLangStructFieldAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangArrayAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangMapAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangJSONLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangMapLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangStructLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangFieldVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangLocalVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangPackageVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStringTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTernaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeCastExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.MultiReturnExpr;
import org.wso2.ballerinalang.compiler.tree.statements.BLanXMLNSStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAbort;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBreak;
import org.wso2.ballerinalang.compiler.tree.statements.BLangCatch;
import org.wso2.ballerinalang.compiler.tree.statements.BLangComment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangContinue;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForkJoin;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReply;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangThrow;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransform;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTryCatchFinally;
import org.wso2.ballerinalang.compiler.tree.statements.BLangVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWorkerSend;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.programfile.CallableUnitInfo;
import org.wso2.ballerinalang.programfile.FunctionInfo;
import org.wso2.ballerinalang.programfile.Instruction;
import org.wso2.ballerinalang.programfile.InstructionCodes;
import org.wso2.ballerinalang.programfile.InstructionFactory;
import org.wso2.ballerinalang.programfile.LocalVariableInfo;
import org.wso2.ballerinalang.programfile.PackageInfo;
import org.wso2.ballerinalang.programfile.PackageVarInfo;
import org.wso2.ballerinalang.programfile.ProgramFile;
import org.wso2.ballerinalang.programfile.WorkerDataChannelInfo;
import org.wso2.ballerinalang.programfile.WorkerInfo;
import org.wso2.ballerinalang.programfile.attributes.AttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.AttributeInfoPool;
import org.wso2.ballerinalang.programfile.attributes.CodeAttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.LineNumberTableAttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.LocalVariableAttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.VarTypeCountAttributeInfo;
import org.wso2.ballerinalang.programfile.cpentries.ConstantPool;
import org.wso2.ballerinalang.programfile.cpentries.FloatCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.FunctionCallCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.FunctionRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.IntegerCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.PackageRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.StringCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.TypeRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.UTF8CPEntry;
import org.wso2.ballerinalang.programfile.cpentries.WorkerDataChannelRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.WrkrInteractionArgsCPEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import static org.wso2.ballerinalang.programfile.ProgramFileConstants.BLOB_OFFSET;
import static org.wso2.ballerinalang.programfile.ProgramFileConstants.BOOL_OFFSET;
import static org.wso2.ballerinalang.programfile.ProgramFileConstants.FLOAT_OFFSET;
import static org.wso2.ballerinalang.programfile.ProgramFileConstants.REF_OFFSET;
import static org.wso2.ballerinalang.programfile.ProgramFileConstants.STRING_OFFSET;

/**
 * @since 0.94
 */
public class CodeGenerator extends BLangNodeVisitor {

    private static final CompilerContext.Key<CodeGenerator> CODE_GENERATOR_KEY =
            new CompilerContext.Key<>();
    /**
     * This structure holds current package-level variable indexes.
     */
    private VariableIndex pvIndexes = new VariableIndex();

    /**
     * This structure holds current local variable indexes.
     */
    private VariableIndex lvIndexes = new VariableIndex();

    /**
     * This structure holds current field indexes.
     */
    private VariableIndex fieldIndexes = new VariableIndex();

    /**
     * This structure holds current register indexes.
     */
    private VariableIndex regIndexes = new VariableIndex();

    /**
     * This structure holds the maximum register count per type.
     * This structure is updated for every statement.
     */
    private VariableIndex maxRegIndexes = new VariableIndex();

    private SymbolEnv env;
    // TODO Remove this dependency from the code generator
    private SymbolEnter symEnter;

    private ProgramFile programFile;

    private PackageInfo currentPkgInfo;
    private String currentPkgName;
    private int currentPackageRefCPIndex;

    private LineNumberTableAttributeInfo lineNoAttrInfo;
    private CallableUnitInfo currentCallableUnitInfo;
    private LocalVariableAttributeInfo localVarAttrInfo;
    private WorkerInfo currentWorkerInfo;

    // Required variables to generate code for assignment statements
    private int rhsExprRegIndex = -1;
    private boolean varAssignment = false;
    private boolean arrayMapAssignment;
    private boolean structAssignment;
    
    private Stack<Instruction> loopResetInstructionStack = new Stack<>();
    private Stack<Instruction> loopExitInstructionStack = new Stack<>();
    
    private int workerChannelCount = 0;

    public static CodeGenerator getInstance(CompilerContext context) {
        CodeGenerator codeGenerator = context.get(CODE_GENERATOR_KEY);
        if (codeGenerator == null) {
            codeGenerator = new CodeGenerator(context);
        }

        return codeGenerator;
    }

    public CodeGenerator(CompilerContext context) {
        context.put(CODE_GENERATOR_KEY, this);

        this.symEnter = SymbolEnter.getInstance(context);
    }

    public ProgramFile generate(BLangPackage pkgNode) {
        programFile = new ProgramFile();
        BPackageSymbol pkgSymbol = pkgNode.symbol;
        genPackage(pkgSymbol);

        programFile.entryPkgCPIndex = addPackageRefCPEntry(programFile, pkgSymbol.name.value,
                pkgSymbol.version.value);

        // TODO Setting this program as a main program. Remove this ASAP
        programFile.setMainEPAvailable(true);

        // Add global variable indexes to the ProgramFile
        prepareIndexes(pvIndexes);

        // Create Global variable attribute info
        addVarCountAttrInfo(programFile, programFile, pvIndexes);

        return programFile;
    }

    public void visit(BLangPackage pkgNode) {
        // first visit all the imports
        pkgNode.imports.forEach(impPkgNode -> genNode(impPkgNode, this.env));

        // Add the current package to the program file
        BPackageSymbol pkgSymbol = pkgNode.symbol;
        currentPkgName = pkgSymbol.name.getValue();
        int pkgNameCPIndex = addUTF8CPEntry(programFile, currentPkgName);
        int pkgVersionCPIndex = addUTF8CPEntry(programFile, pkgSymbol.version.getValue());
        currentPkgInfo = new PackageInfo(pkgNameCPIndex, pkgVersionCPIndex);

        // TODO We need to create identifier for both name and the version
        programFile.packageInfoMap.put(currentPkgName, currentPkgInfo);

        // Insert the package reference to the constant pool of the Ballerina program
        addPackageRefCPEntry(programFile, currentPkgName, pkgSymbol.version.value);

        // Insert the package reference to the constant pool of the current package
        currentPackageRefCPIndex = addPackageRefCPEntry(currentPkgInfo, currentPkgName, pkgSymbol.version.value);

        // This attribute keep track of line numbers
        int lineNoAttrNameIndex = addUTF8CPEntry(currentPkgInfo,
                AttributeInfo.Kind.LINE_NUMBER_TABLE_ATTRIBUTE.value());
        lineNoAttrInfo = new LineNumberTableAttributeInfo(lineNoAttrNameIndex);

        // This attribute keep package-level variable information
        int pkgVarAttrNameIndex = addUTF8CPEntry(currentPkgInfo,
                AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE.value());
        currentPkgInfo.addAttributeInfo(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE,
                new LocalVariableAttributeInfo(pkgVarAttrNameIndex));

//        visitConstants(bLangPackage.getConsts());
//        visitGlobalVariables(bLangPackage.getGlobalVariables());
        pkgNode.globalVars.forEach(varNode -> createPackageVarInfo(varNode.symbol));
//        createStructInfoEntries(bLangPackage.getStructDefs());
//        createConnectorInfoEntries(bLangPackage.getConnectors());
//        createServiceInfoEntries(bLangPackage.getServices());
        pkgNode.functions.forEach(funcNode -> createFunctionInfoEntry(funcNode));

//        // Create function info for the package function
        BLangFunction pkgInitFunc = pkgNode.initFunction;
        createFunctionInfoEntry(pkgInitFunc);
//
        for (TopLevelNode pkgLevelNode : pkgNode.topLevelNodes) {
            genNode((BLangNode) pkgLevelNode, this.env);
        }

        // Visit package init function
        genNode(pkgInitFunc, this.env);

        currentPkgInfo.addAttributeInfo(AttributeInfo.Kind.LINE_NUMBER_TABLE_ATTRIBUTE, lineNoAttrInfo);
        currentPackageRefCPIndex = -1;
        currentPkgName = null;
    }

    public void visit(BLangImportPackage importPkgNode) {
        BPackageSymbol pkgSymbol = importPkgNode.symbol;
        genPackage(pkgSymbol);
    }

    public void visit(BLangFunction funcNode) {
        SymbolEnv funcEnv = SymbolEnv.createFunctionEnv(funcNode, funcNode.symbol.scope, this.env);
        currentCallableUnitInfo = currentPkgInfo.functionInfoMap.get(funcNode.symbol.name.value);
        visitInvokableNode(funcNode, currentCallableUnitInfo, funcEnv);
    }

    public void visit(BLangBlockStmt blockNode) {
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(blockNode, this.env);

        for (BLangStatement stmt : blockNode.stmts) {
//            if (stmt instanceof CommentStmt) {
//                continue;
//            }
//
//            if (!(stmt instanceof TryCatchStmt)) {
//                addLineNumberInfo(stmt.getNodeLocation());
//            }

            genNode(stmt, blockEnv);

            // Update the maxRegIndexes structure
            setMaxRegIndexes();

            // Reset the regIndexes structure for every statement
            regIndexes = new VariableIndex();
        }
    }

    public void visit(BLangVariable varNode) {
        int opcode;
        int lvIndex;
        BVarSymbol varSymbol = varNode.symbol;

        BLangExpression rhsExpr = varNode.expr;
        if (rhsExpr != null) {
            genNode(rhsExpr, this.env);
            rhsExprRegIndex = rhsExpr.regIndex;
        }

        int ownerSymTag = env.scope.owner.tag;
        if ((ownerSymTag & SymTag.INVOKABLE) == SymTag.INVOKABLE) {
            OpcodeAndIndex opcodeAndIndex = getOpcodeAndIndex(varSymbol.type.tag,
                    InstructionCodes.ISTORE, lvIndexes);
            opcode = opcodeAndIndex.opcode;
            lvIndex = opcodeAndIndex.index;
            varSymbol.varIndex = lvIndex;
            if (rhsExpr != null) {
                emit(opcode, rhsExprRegIndex, lvIndex);
            }

            LocalVariableInfo localVarInfo = getLocalVarAttributeInfo(varSymbol);
            localVarAttrInfo.localVars.add(localVarInfo);
        } else {
            // TODO Support other variable nodes
        }
    }


    // Statements

    public void visit(BLangVariableDef varDefNode) {
        genNode(varDefNode.var, this.env);
    }


    public void visit(BLangReturn returnNode) {
        BLangExpression expr;
        int i = 0;
        while (i < returnNode.exprs.size()) {
            expr = returnNode.exprs.get(i);
            this.genNode(expr, this.env);
            if (expr.isMultiReturnExpr()) {
                BLangInvocation invExpr = (BLangInvocation) expr;
                for (int j = 0; j < invExpr.regIndexes.length; j++) {
                    emit(this.typeTagToInstr(invExpr.types.get(j).tag), i, invExpr.regIndexes[j]);
                    i++;
                }
            } else {
                emit(this.typeTagToInstr(expr.type.tag), i, expr.regIndex);
                i++;
            }
        }
        emit(InstructionCodes.RET);
    }

    private int typeTagToInstr(int typeTag) {
        switch (typeTag) {
            case TypeTags.INT:
                return InstructionCodes.IRET;
            case TypeTags.FLOAT:
                return InstructionCodes.FRET;
            case TypeTags.STRING:
                return InstructionCodes.SRET;
            case TypeTags.BOOLEAN:
                return InstructionCodes.BRET;
            case TypeTags.BLOB:
                return InstructionCodes.LRET;
            default:
                return InstructionCodes.RRET;
        }
    }


    // Expressions

    public void visit(BLangLiteral literalExpr) {
        int opcode;
        int regIndex = -1;
        int typeTag = literalExpr.typeTag;

        switch (typeTag) {
            case TypeTags.INT:
                regIndex = ++regIndexes.tInt;
                long longVal = (Long) literalExpr.value;
                if (longVal >= 0 && longVal <= 5) {
                    opcode = InstructionCodes.ICONST_0 + (int) longVal;
                    emit(opcode, regIndex);
                } else {
                    int intCPEntryIndex = currentPkgInfo.addCPEntry(new IntegerCPEntry(longVal));
                    emit(InstructionCodes.ICONST, intCPEntryIndex, regIndex);
                }
                break;

            case TypeTags.FLOAT:
                regIndex = ++regIndexes.tFloat;
                double doubleVal = (Double) literalExpr.value;
                if (doubleVal == 0 || doubleVal == 1 || doubleVal == 2 ||
                        doubleVal == 3 || doubleVal == 4 || doubleVal == 5) {
                    opcode = InstructionCodes.FCONST_0 + (int) doubleVal;
                    emit(opcode, regIndex);
                } else {
                    int floatCPEntryIndex = currentPkgInfo.addCPEntry(new FloatCPEntry(doubleVal));
                    emit(InstructionCodes.FCONST, floatCPEntryIndex, regIndex);
                }
                break;

            case TypeTags.STRING:
                regIndex = ++regIndexes.tString;
                String strValue = (String) literalExpr.value;
                StringCPEntry stringCPEntry = new StringCPEntry(addUTF8CPEntry(currentPkgInfo, strValue), strValue);
                int strCPIndex = currentPkgInfo.addCPEntry(stringCPEntry);
                emit(InstructionCodes.SCONST, strCPIndex, regIndex);
                break;

            case TypeTags.BOOLEAN:
                regIndex = ++regIndexes.tBoolean;
                boolean booleanVal = (Boolean) literalExpr.value;
                if (!booleanVal) {
                    opcode = InstructionCodes.BCONST_0;
                } else {
                    opcode = InstructionCodes.BCONST_1;
                }
                emit(opcode, regIndex);
                break;
        }

        literalExpr.regIndex = regIndex;
    }

    @Override
    public void visit(BLangJSONLiteral jsonLiteral) {

    }

    @Override
    public void visit(BLangMapLiteral mapLiteral) {

    }

    @Override
    public void visit(BLangStructLiteral structLiteral) {

    }

    @Override
    public void visit(BLangLocalVarRef localVarRef) {
        int lvIndex = localVarRef.symbol.varIndex;
        if (varAssignment) {
            int opcode = getOpcode(localVarRef.type.tag, InstructionCodes.ISTORE);
            emit(opcode, rhsExprRegIndex, lvIndex);
            return;
        }

        OpcodeAndIndex opcodeAndIndex = getOpcodeAndIndex(localVarRef.type.tag,
                InstructionCodes.ILOAD, regIndexes);
        int opcode = opcodeAndIndex.opcode;
        int exprRegIndex = opcodeAndIndex.index;
        emit(opcode, lvIndex, exprRegIndex);
        localVarRef.regIndex = exprRegIndex;
    }

    @Override
    public void visit(BLangFieldVarRef fieldVarRef) {
        int varRegIndex;
        int fieldIndex = fieldVarRef.symbol.varIndex;
        if (fieldVarRef.type.tag == TypeTags.STRUCT) {
            // This is a struct field.
            // the struct reference must be stored in the current reference register index.
            varRegIndex = regIndexes.tRef;
        } else {
            // This is a connector field.
            // the connector reference must be stored in the current reference register index.
            varRegIndex = ++regIndexes.tRef;

            // The connector is always the first parameter of the action
            emit(InstructionCodes.RLOAD, 0, varRegIndex);
        }

        if (varAssignment) {
            int opcode = getOpcode(fieldVarRef.type.tag,
                    InstructionCodes.IFIELDSTORE);
            emit(opcode, varRegIndex, fieldIndex, rhsExprRegIndex);
            return;
        }

        OpcodeAndIndex opcodeAndIndex = getOpcodeAndIndex(fieldVarRef.type.tag,
                InstructionCodes.IFIELDLOAD, regIndexes);
        int opcode = opcodeAndIndex.opcode;
        int exprRegIndex = opcodeAndIndex.index;
        emit(opcode, varRegIndex, fieldIndex, exprRegIndex);
        fieldVarRef.regIndex = exprRegIndex;
    }

    @Override
    public void visit(BLangPackageVarRef packageVarRef) {
        int gvIndex = packageVarRef.symbol.varIndex;
        if (varAssignment) {
            int opcode = getOpcode(packageVarRef.type.tag,
                    InstructionCodes.IGSTORE);
            emit(opcode, rhsExprRegIndex, gvIndex);
            return;
        }

        OpcodeAndIndex opcodeAndIndex = getOpcodeAndIndex(packageVarRef.type.tag,
                InstructionCodes.IGLOAD, regIndexes);
        int opcode = opcodeAndIndex.opcode;
        int exprRegIndex = opcodeAndIndex.index;
        emit(opcode, gvIndex, exprRegIndex);
        packageVarRef.regIndex = exprRegIndex;
    }

    @Override
    public void visit(BLangStructFieldAccessExpr fieldAccessExpr) {
    }

    @Override
    public void visit(BLangMapAccessExpr mapKeyAccessExpr) {
    }

    @Override
    public void visit(BLangArrayAccessExpr arrayIndexAccessExpr) {
    }

    public void visit(BLangBinaryExpr binaryExpr) {
        genNode(binaryExpr.lhsExpr, this.env);
        genNode(binaryExpr.rhsExpr, this.env);

        int opcode = binaryExpr.opSymbol.opcode;
        int exprIndex = getNextIndex(binaryExpr.type.tag, regIndexes);

        binaryExpr.regIndex = exprIndex;
        emit(opcode, binaryExpr.lhsExpr.regIndex, binaryExpr.rhsExpr.regIndex, exprIndex);
    }

    public void visit(BLangInvocation iExpr) {
        if (iExpr.expr == null) {
            BInvokableSymbol funcSymbol = iExpr.symbol;
            BPackageSymbol pkgSymbol = (BPackageSymbol) funcSymbol.owner;
            int pkgRefCPIndex = addPackageRefCPEntry(currentPkgInfo, pkgSymbol.name.value, pkgSymbol.version.value);
            int funcNameCPIndex = addUTF8CPEntry(currentPkgInfo, funcSymbol.name.value);
            FunctionRefCPEntry funcRefCPEntry = new FunctionRefCPEntry(pkgRefCPIndex, funcNameCPIndex);

            int funcCallCPIndex = getFunctionCallCPIndex(iExpr);
            int funcRefCPIndex = currentPkgInfo.addCPEntry(funcRefCPEntry);

            if (Symbols.isNative(funcSymbol)) {
                emit(InstructionCodes.NCALL, funcRefCPIndex, funcCallCPIndex);
            } else {
                emit(InstructionCodes.CALL, funcRefCPIndex, funcCallCPIndex);
            }
        }
    }

    public void visit(BLangTypeCastExpr castExpr) {
        BLangExpression rExpr = castExpr.expr;
        genNode(rExpr, this.env);

        // TODO Improve following logic
        int opCode = castExpr.castSymbol.opcode;
        int errorRegIndex = ++regIndexes.tRef;

        if (opCode == InstructionCodes.CHECKCAST) {
            int typeSigCPIndex = addUTF8CPEntry(currentPkgInfo, castExpr.type.getDesc());
            TypeRefCPEntry typeRefCPEntry = new TypeRefCPEntry(typeSigCPIndex);
            int typeCPIndex = currentPkgInfo.addCPEntry(typeRefCPEntry);
            int targetRegIndex = getNextIndex(castExpr.type.tag, regIndexes);

            castExpr.regIndexes = new int[]{targetRegIndex, errorRegIndex};
            emit(opCode, rExpr.regIndex, typeCPIndex, targetRegIndex, errorRegIndex);

        } else if (opCode == InstructionCodes.ANY2T || opCode == InstructionCodes.ANY2C) {
            int typeSigCPIndex = addUTF8CPEntry(currentPkgInfo, castExpr.type.getDesc());
            TypeRefCPEntry typeRefCPEntry = new TypeRefCPEntry(typeSigCPIndex);
            int typeCPIndex = currentPkgInfo.addCPEntry(typeRefCPEntry);
            int targetRegIndex = getNextIndex(castExpr.type.tag, regIndexes);

            castExpr.regIndexes = new int[]{targetRegIndex, errorRegIndex};
            emit(opCode, rExpr.regIndex, typeCPIndex, targetRegIndex, errorRegIndex);

        } else if (opCode != 0) {
            int targetRegIndex = getNextIndex(castExpr.type.tag, regIndexes);
            castExpr.regIndexes = new int[]{targetRegIndex, errorRegIndex};
            emit(opCode, castExpr.regIndex, targetRegIndex, errorRegIndex);

        } else {
            // Ignore NOP opcode
            castExpr.regIndexes = new int[]{rExpr.regIndex, errorRegIndex};
        }

        castExpr.regIndex = castExpr.regIndexes[0];
    }

    public void visit(BLangExpressionStmt exprStmtNode) {
        genNode(exprStmtNode.expr, this.env);
    }


    // private methods

    private void genNode(BLangNode node, SymbolEnv env) {
        SymbolEnv prevEnv = this.env;
        this.env = env;
        node.accept(this);
        this.env = prevEnv;
    }

    private void genPackage(BPackageSymbol pkgSymbol) {
        // TODO First check whether this symbol is from a BALO file.
        SymbolEnv pkgEnv = symEnter.packageEnvs.get(pkgSymbol);
        genNode(pkgEnv.node, pkgEnv);
    }

    private String generateSignature(CallableUnitInfo callableUnitInfo) {
        StringBuilder strBuilder = new StringBuilder("(");
        for (BType paramType : callableUnitInfo.paramTypes) {
            strBuilder.append(paramType.getDesc());
        }
        strBuilder.append(")(");

        for (BType retType : callableUnitInfo.retParamTypes) {
            strBuilder.append(retType.getDesc());
        }
        strBuilder.append(")");

        return strBuilder.toString();
    }

    private OpcodeAndIndex getOpcodeAndIndex(int typeTag, int baseOpcode, VariableIndex indexes) {
        int index;
        int opcode;
        switch (typeTag) {
            case TypeTags.INT:
                opcode = baseOpcode;
                index = ++indexes.tInt;
                break;
            case TypeTags.FLOAT:
                opcode = baseOpcode + FLOAT_OFFSET;
                index = ++indexes.tFloat;
                break;
            case TypeTags.STRING:
                opcode = baseOpcode + STRING_OFFSET;
                index = ++indexes.tString;
                break;
            case TypeTags.BOOLEAN:
                opcode = baseOpcode + BOOL_OFFSET;
                index = ++indexes.tBoolean;
                break;
            case TypeTags.BLOB:
                opcode = baseOpcode + BLOB_OFFSET;
                index = ++indexes.tBlob;
                break;
            default:
                opcode = baseOpcode + REF_OFFSET;
                index = ++indexes.tRef;
                break;
        }

        return new OpcodeAndIndex(opcode, index);
    }

    private int getNextIndex(int typeTag, VariableIndex indexes) {
        return getOpcodeAndIndex(typeTag, -1, indexes).index;
    }

    private int getOpcode(int typeTag, int baseOpcode) {
        int opcode;
        switch (typeTag) {
            case TypeTags.INT:
                opcode = baseOpcode;
                break;
            case TypeTags.FLOAT:
                opcode = baseOpcode + FLOAT_OFFSET;
                break;
            case TypeTags.STRING:
                opcode = baseOpcode + STRING_OFFSET;
                break;
            case TypeTags.BOOLEAN:
                opcode = baseOpcode + BOOL_OFFSET;
                break;
            case TypeTags.BLOB:
                opcode = baseOpcode + BLOB_OFFSET;
                break;
            default:
                opcode = baseOpcode + REF_OFFSET;
                break;
        }

        return opcode;
    }

    private LocalVariableInfo getLocalVarAttributeInfo(BVarSymbol varSymbol) {
        int varNameCPIndex = addUTF8CPEntry(currentPkgInfo, varSymbol.name.value);
        int varIndex = varSymbol.varIndex;
        int sigCPIndex = addUTF8CPEntry(currentPkgInfo, varSymbol.type.getDesc());
        return new LocalVariableInfo(varNameCPIndex, sigCPIndex, varIndex);
    }

    private void visitInvokableNode(BLangInvokableNode invokableNode,
                                    CallableUnitInfo callableUnitInfo,
                                    SymbolEnv invokableSymbolEnv) {
        int localVarAttrNameIndex = addUTF8CPEntry(currentPkgInfo,
                AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE.value());
        LocalVariableAttributeInfo localVarAttributeInfo = new LocalVariableAttributeInfo(localVarAttrNameIndex);

        // TODO Read annotations attached to this callableUnit

        // Add local variable indexes to the parameters and return parameters
        visitInvokableNodeParams(invokableNode.symbol, callableUnitInfo, localVarAttributeInfo);

        if (Symbols.isNative(invokableNode.symbol)) {
            this.processWorker(invokableNode, callableUnitInfo.defaultWorkerInfo, null, 
                    localVarAttributeInfo, invokableSymbolEnv, true, null);
        } else {
            // Clone lvIndex structure here. This structure contain local variable indexes of the input and
            // out parameters and they are common for all the workers.
            VariableIndex lvIndexCopy = this.copyVarIndex(lvIndexes);
            this.processWorker(invokableNode, callableUnitInfo.defaultWorkerInfo, invokableNode.body, 
                    localVarAttributeInfo, invokableSymbolEnv, true, lvIndexCopy);
            for (BLangWorker worker : invokableNode.getWorkers()) {
                this.processWorker(invokableNode, callableUnitInfo.getWorkerInfo(worker.name.value), 
                        worker.body, localVarAttributeInfo, invokableSymbolEnv, false, lvIndexCopy);
            }
        }
    }
    
    private void processWorker(BLangInvokableNode invokableNode, WorkerInfo workerInfo, BLangBlockStmt body, 
            LocalVariableAttributeInfo localVarAttributeInfo, SymbolEnv invokableSymbolEnv,
            boolean defaultWorker, VariableIndex lvIndexCopy) {
        int codeAttrNameCPIndex = this.addUTF8CPEntry(this.currentPkgInfo, AttributeInfo.Kind.CODE_ATTRIBUTE.value());
        workerInfo.codeAttributeInfo.attributeNameIndex = codeAttrNameCPIndex;
        workerInfo.addAttributeInfo(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE, localVarAttributeInfo);
        if (body != null) {
            localVarAttrInfo = new LocalVariableAttributeInfo(localVarAttributeInfo.attributeNameIndex);
            localVarAttrInfo.localVars = new ArrayList<>(localVarAttributeInfo.localVars);
            workerInfo.codeAttributeInfo.codeAddrs = nextIP();
            this.lvIndexes = this.copyVarIndex(lvIndexCopy);
            this.currentWorkerInfo = workerInfo;
            this.genNode(body, invokableSymbolEnv);
            if (invokableNode.retParams.isEmpty() && defaultWorker) {
                /* for functions that has no return values, we must provide a default
                 * return statement to stop the execution and jump to the caller */
                this.emit(InstructionCodes.RET);
            }
        }
        this.endWorkerInfoUnit(workerInfo.codeAttributeInfo);
        if (!defaultWorker) {
            this.emit(InstructionCodes.HALT);
        }
    }

    private void visitInvokableNodeParams(BInvokableSymbol invokableSymbol, CallableUnitInfo callableUnitInfo,
                                          LocalVariableAttributeInfo localVarAttrInfo) {

        // TODO Read param and return param annotations
        invokableSymbol.params.forEach(param -> visitInvokableNodeParam(param, localVarAttrInfo));
        invokableSymbol.retParams.forEach(param -> visitInvokableNodeParam(param, localVarAttrInfo));
        callableUnitInfo.addAttributeInfo(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE, localVarAttrInfo);
    }

    private void visitInvokableNodeParam(BVarSymbol paramSymbol, LocalVariableAttributeInfo localVarAttrInfo) {
        paramSymbol.varIndex = getNextIndex(paramSymbol.type.tag, lvIndexes);
        LocalVariableInfo localVarInfo = getLocalVarAttributeInfo(paramSymbol);
        localVarAttrInfo.localVars.add(localVarInfo);
        // TODO read parameter annotations
    }

    private VariableIndex copyVarIndex(VariableIndex that) {
        VariableIndex vIndexes = new VariableIndex();
        vIndexes.tInt = that.tInt;
        vIndexes.tFloat = that.tFloat;
        vIndexes.tString = that.tString;
        vIndexes.tBoolean = that.tBoolean;
        vIndexes.tBlob = that.tBlob;
        vIndexes.tRef = that.tRef;
        return vIndexes;
    }

    private int nextIP() {
        return currentPkgInfo.instructionList.size();
    }

    private void endWorkerInfoUnit(CodeAttributeInfo codeAttributeInfo) {
        codeAttributeInfo.maxLongLocalVars = lvIndexes.tInt + 1;
        codeAttributeInfo.maxDoubleLocalVars = lvIndexes.tFloat + 1;
        codeAttributeInfo.maxStringLocalVars = lvIndexes.tString + 1;
        codeAttributeInfo.maxIntLocalVars = lvIndexes.tBoolean + 1;
        codeAttributeInfo.maxByteLocalVars = lvIndexes.tBlob + 1;
        codeAttributeInfo.maxRefLocalVars = lvIndexes.tRef + 1;

        codeAttributeInfo.maxLongRegs = maxRegIndexes.tInt + 1;
        codeAttributeInfo.maxDoubleRegs = maxRegIndexes.tFloat + 1;
        codeAttributeInfo.maxStringRegs = maxRegIndexes.tString + 1;
        codeAttributeInfo.maxIntRegs = maxRegIndexes.tBoolean + 1;
        codeAttributeInfo.maxByteRegs = maxRegIndexes.tBlob + 1;
        codeAttributeInfo.maxRefRegs = maxRegIndexes.tRef + 1;

        lvIndexes = new VariableIndex();
        regIndexes = new VariableIndex();
        maxRegIndexes = new VariableIndex();
    }

    private void setMaxRegIndexes() {
        maxRegIndexes.tInt = (maxRegIndexes.tInt > regIndexes.tInt) ?
                maxRegIndexes.tInt : regIndexes.tInt;
        maxRegIndexes.tFloat = (maxRegIndexes.tFloat > regIndexes.tFloat) ?
                maxRegIndexes.tFloat : regIndexes.tFloat;
        maxRegIndexes.tString = (maxRegIndexes.tString > regIndexes.tString) ?
                maxRegIndexes.tString : regIndexes.tString;
        maxRegIndexes.tBoolean = (maxRegIndexes.tBoolean > regIndexes.tBoolean) ?
                maxRegIndexes.tBoolean : regIndexes.tBoolean;
        maxRegIndexes.tBlob = (maxRegIndexes.tBlob > regIndexes.tBlob) ?
                maxRegIndexes.tBlob : regIndexes.tBlob;
        maxRegIndexes.tRef = (maxRegIndexes.tRef > regIndexes.tRef) ?
                maxRegIndexes.tRef : regIndexes.tRef;
    }

    private void prepareIndexes(VariableIndex indexes) {
        indexes.tInt++;
        indexes.tFloat++;
        indexes.tString++;
        indexes.tBoolean++;
        indexes.tBlob++;
        indexes.tRef++;
    }

    private int emit(int opcode, int... operands) {
        currentPkgInfo.instructionList.add(InstructionFactory.get(opcode, operands));
        return currentPkgInfo.instructionList.size();
    }

    private int emit(Instruction instr) {
        currentPkgInfo.instructionList.add(instr);
        return currentPkgInfo.instructionList.size();
    }

    private void addVarCountAttrInfo(ConstantPool constantPool,
                                     AttributeInfoPool attributeInfoPool,
                                     VariableIndex fieldCount) {
        int attrNameCPIndex = addUTF8CPEntry(constantPool,
                AttributeInfo.Kind.VARIABLE_TYPE_COUNT_ATTRIBUTE.value());
        VarTypeCountAttributeInfo varCountAttribInfo = new VarTypeCountAttributeInfo(attrNameCPIndex);
        varCountAttribInfo.setMaxLongVars(fieldCount.tInt);
        varCountAttribInfo.setMaxDoubleVars(fieldCount.tFloat);
        varCountAttribInfo.setMaxStringVars(fieldCount.tString);
        varCountAttribInfo.setMaxIntVars(fieldCount.tBoolean);
        varCountAttribInfo.setMaxByteVars(fieldCount.tBlob);
        varCountAttribInfo.setMaxRefVars(fieldCount.tRef);
        attributeInfoPool.addAttributeInfo(AttributeInfo.Kind.VARIABLE_TYPE_COUNT_ATTRIBUTE, varCountAttribInfo);
    }

    private int getFunctionCallCPIndex(BLangInvocation iExpr) {
        int[] argRegs = new int[iExpr.argExprs.size()];
        for (int i = 0; i < iExpr.argExprs.size(); i++) {
            BLangExpression argExpr = iExpr.argExprs.get(i);
            genNode(argExpr, this.env);
            argRegs[i] = argExpr.regIndex;
        }

        // Calculate registers to store return values
        int[] retRegs = new int[iExpr.types.size()];
        for (int i = 0; i < iExpr.types.size(); i++) {
            BType retType = iExpr.types.get(i);
            retRegs[i] = getNextIndex(retType.tag, regIndexes);
        }

        iExpr.regIndexes = retRegs;
        if (retRegs.length > 0) {
            iExpr.regIndex = retRegs[0];
        }

        FunctionCallCPEntry funcCallCPEntry = new FunctionCallCPEntry(argRegs, retRegs);
        return currentPkgInfo.addCPEntry(funcCallCPEntry);
    }


    // Create info entries

    private void createPackageVarInfo(BVarSymbol varSymbol) {
        BType varType = varSymbol.type;
        varSymbol.varIndex = getNextIndex(varType.tag, pvIndexes);

        int varNameCPIndex = addUTF8CPEntry(currentPkgInfo, varSymbol.name.value);
        int typeSigCPIndex = addUTF8CPEntry(currentPkgInfo, varType.getDesc());
        PackageVarInfo pkgVarInfo = new PackageVarInfo(varNameCPIndex, typeSigCPIndex, varSymbol.flags);
        currentPkgInfo.pkgVarInfoMap.put(varSymbol.name.value, pkgVarInfo);

        LocalVariableInfo localVarInfo = getLocalVarAttributeInfo(varSymbol);
        LocalVariableAttributeInfo pkgVarAttrInfo = (LocalVariableAttributeInfo)
                currentPkgInfo.getAttributeInfo(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE);
        pkgVarAttrInfo.localVars.add(localVarInfo);

        // TODO Populate annotation attribute
    }

    private void createFunctionInfoEntry(BLangInvokableNode invokable) {
        BInvokableSymbol funcSymbol = invokable.symbol;
        BInvokableType funcType = (BInvokableType) funcSymbol.type;

        // Add function name as an UTFCPEntry to the constant pool
        int funcNameCPIndex = this.addUTF8CPEntry(currentPkgInfo, funcSymbol.name.value);

        FunctionInfo invInfo = new FunctionInfo(currentPackageRefCPIndex, funcNameCPIndex);
        invInfo.paramTypes = funcType.paramTypes.toArray(new BType[0]);
        invInfo.retParamTypes = funcType.retTypes.toArray(new BType[0]);
        invInfo.flags = funcSymbol.flags;

        this.addWorkerInfoEntries(invInfo, invokable.getWorkers());

        invInfo.signatureCPIndex = addUTF8CPEntry(this.currentPkgInfo, generateSignature(invInfo));
        this.currentPkgInfo.functionInfoMap.put(funcSymbol.name.value, invInfo);
    }
    
    private void addWorkerInfoEntries(CallableUnitInfo callableUnitInfo, List<BLangWorker> workers) {
        UTF8CPEntry workerNameCPEntry = new UTF8CPEntry("default");
        int workerNameCPIndex = this.currentPkgInfo.addCPEntry(workerNameCPEntry);
        WorkerInfo defaultWorkerInfo = new WorkerInfo(workerNameCPIndex, "default");
        callableUnitInfo.defaultWorkerInfo = defaultWorkerInfo;
        for (BLangWorker worker : workers) {
            workerNameCPEntry = new UTF8CPEntry(worker.name.value);
            workerNameCPIndex = currentPkgInfo.addCPEntry(workerNameCPEntry);
            WorkerInfo workerInfo = new WorkerInfo(workerNameCPIndex, worker.getName().value);
            callableUnitInfo.addWorkerInfo(worker.getName().value, workerInfo);
        }
    }
    
    private WorkerDataChannelInfo getWorkerDataChannelInfo(CallableUnitInfo callableUnit, 
            String source, String target) {
        WorkerDataChannelInfo workerDataChannelInfo = callableUnit.getWorkerDataChannelInfo(
                WorkerDataChannelInfo.generateChannelName(source, target));
        if (workerDataChannelInfo == null) {
            UTF8CPEntry sourceCPEntry = new UTF8CPEntry(source);
            int sourceCPIndex = this.currentPkgInfo.addCPEntry(sourceCPEntry);
            UTF8CPEntry targetCPEntry = new UTF8CPEntry(target);
            int targetCPIndex = this.currentPkgInfo.addCPEntry(targetCPEntry);
            workerDataChannelInfo = new WorkerDataChannelInfo(sourceCPIndex, source, targetCPIndex, target);
            workerDataChannelInfo.setUniqueName(workerDataChannelInfo.getChannelName() + this.workerChannelCount);
            String uniqueName = workerDataChannelInfo.getUniqueName();
            UTF8CPEntry uniqueNameCPEntry = new UTF8CPEntry(uniqueName);
            int uniqueNameCPIndex = this.currentPkgInfo.addCPEntry(uniqueNameCPEntry);
            workerDataChannelInfo.setUniqueNameCPIndex(uniqueNameCPIndex);
            callableUnit.addWorkerDataChannelInfo(workerDataChannelInfo);
            this.workerChannelCount++;
        }
        return workerDataChannelInfo;
    }

    // Constant pool related utility classes

    private int addUTF8CPEntry(ConstantPool pool, String value) {
        UTF8CPEntry pkgPathCPEntry = new UTF8CPEntry(value);
        return pool.addCPEntry(pkgPathCPEntry);
    }

    private int addPackageRefCPEntry(ConstantPool pool, String name, String version) {
        int nameCPIndex = addUTF8CPEntry(pool, name);
        int versionCPIndex = addUTF8CPEntry(pool, version);
        PackageRefCPEntry packageRefCPEntry = new PackageRefCPEntry(nameCPIndex, versionCPIndex);
        return pool.addCPEntry(packageRefCPEntry);
    }

    /**
     * Holds the variable index per type.
     *
     * @since 0.94
     */
    private static class VariableIndex {
        int tInt = -1;
        int tFloat = -1;
        int tString = -1;
        int tBoolean = -1;
        int tBlob = -1;
        int tRef = -1;
    }

    /**
     * Bean class which keep both opcode and the current variable index.
     *
     * @since 0.94
     */
    public static class OpcodeAndIndex {
        int opcode;
        int index;

        public OpcodeAndIndex(int opcode, int index) {
            this.opcode = opcode;
            this.index = index;
        }
    }

    public void visit(BLangXMLNS xmlnsNode) {
        /* ignore */
    }

    public void visit(BLangWorker workerNode) {
        this.genNode(workerNode.body, this.env);
    }

    public void visit(BLangForkJoin forkJoin) {
        /* ignore */
    }

    public void visit(BLangWorkerSend workerSendNode) {
        WorkerDataChannelInfo workerDataChannelInfo = this.getWorkerDataChannelInfo(this.currentCallableUnitInfo, 
                this.currentWorkerInfo.getWorkerName(), workerSendNode.workerIdentifier.value);
        WorkerDataChannelRefCPEntry wrkrInvRefCPEntry = new WorkerDataChannelRefCPEntry(workerDataChannelInfo
                .getUniqueNameCPIndex(), workerDataChannelInfo.getUniqueName());
        wrkrInvRefCPEntry.setWorkerDataChannelInfo(workerDataChannelInfo);
        int wrkrInvRefCPIndex = currentPkgInfo.addCPEntry(wrkrInvRefCPEntry);
        this.currentWorkerInfo.setWrkrDtChnlRefCPIndex(wrkrInvRefCPIndex);
        this.currentWorkerInfo.setWorkerDataChannelInfoForForkJoin(workerDataChannelInfo);
        workerDataChannelInfo.setDataChannelRefIndex(wrkrInvRefCPIndex);
        int workerInvocationIndex = this.getWorkerSendCPIndex(workerSendNode);
        emit(InstructionCodes.WRKINVOKE, wrkrInvRefCPIndex, workerInvocationIndex);
    }
    
    private void genNodeList(List<BLangExpression> exprs, SymbolEnv env) {
        exprs.forEach(e -> this.genNode(e, env));
    }
    
    private int[] extractsRegisters(List<BLangExpression> exprs) {
        int[] regs = new int[exprs.size()];
        for (int i = 0; i < regs.length; i++) {
            regs[i] = exprs.get(i).regIndex;
        }
        return regs;
    }
    
    private BType[] extractTypes(List<BLangExpression> exprs) {
        return exprs.stream().map(e -> e.type).collect(Collectors.toList()).toArray(new BType[0]);
    }
    
    private String generateSig(BType[] types) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(types).forEach(e -> builder.append(e.getDesc()));
        return builder.toString();
    }
    
    private int getWorkerSendCPIndex(BLangWorkerSend workerSendStmt) {
        List<BLangExpression> argExprs = workerSendStmt.exprs;
        this.genNodeList(argExprs, this.env);
        int[] argRegs = this.extractsRegisters(argExprs);
        BType[] bTypes = this.extractTypes(argExprs);
        WrkrInteractionArgsCPEntry workerInvokeCPEntry = new WrkrInteractionArgsCPEntry(argRegs, bTypes);
        UTF8CPEntry sigCPEntry = new UTF8CPEntry(this.generateSig(bTypes));
        int sigCPIndex = this.currentPkgInfo.addCPEntry(sigCPEntry);
        workerInvokeCPEntry.setTypesSignatureCPIndex(sigCPIndex);
        return this.currentPkgInfo.addCPEntry(workerInvokeCPEntry);
    }

    public void visit(BLangWorkerReceive workerReceiveNode) {
        WorkerDataChannelInfo workerDataChannelInfo = this.getWorkerDataChannelInfo(this.currentCallableUnitInfo, 
                workerReceiveNode.workerIdentifier.value, this.currentWorkerInfo.getWorkerName());
        WorkerDataChannelRefCPEntry wrkrChnlRefCPEntry = new WorkerDataChannelRefCPEntry(workerDataChannelInfo
                .getUniqueNameCPIndex(), workerDataChannelInfo.getUniqueName());
        wrkrChnlRefCPEntry.setWorkerDataChannelInfo(workerDataChannelInfo);
        int wrkrRplyRefCPIndex = currentPkgInfo.addCPEntry(wrkrChnlRefCPEntry);
        workerDataChannelInfo.setDataChannelRefIndex(wrkrRplyRefCPIndex);
        int workerReplyIndex = getWorkerReplyCPIndex(workerReceiveNode);
        WrkrInteractionArgsCPEntry wrkrRplyCPEntry  = (WrkrInteractionArgsCPEntry) this.currentPkgInfo.getCPEntry(
                workerReplyIndex);
        emit(InstructionCodes.WRKREPLY, wrkrRplyRefCPIndex, workerReplyIndex);
        /* generate store instructions to store the values */
        int[] rhsExprRegIndexes = wrkrRplyCPEntry.getArgRegs();
        List<BLangExpression> lhsExprs = workerReceiveNode.exprs;
        for (int i = 0; i < lhsExprs.size(); i++) {
            this.genRHSExpr(lhsExprs.get(i), rhsExprRegIndexes[i]);
        }
    }
    
    private void genRHSExpr(BLangExpression lExpr, int regIndex) {
        this.rhsExprRegIndex = regIndex;
        if (lExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            this.varAssignment = true;
            this.genNode(lExpr, this.env);
            this.varAssignment = false;
        } else if (lExpr.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR) {
            this.arrayMapAssignment = true;
            this.genNode(lExpr, this.env);
            this.arrayMapAssignment = false;
        } else if (lExpr.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR) {
            this.structAssignment = true;
            this.genNode(lExpr, this.env);
            this.structAssignment = false;
        }
    }
    
    private int getWorkerReplyCPIndex(BLangWorkerReceive workerReplyStmt) {
        BType[] retTypes = this.extractTypes(workerReplyStmt.exprs);
        int[] argRegs = new int[retTypes.length];
        for (int i = 0; i < retTypes.length; i++) {
            BType retType = retTypes[i];
            argRegs[i] = getNextIndex(retType.tag, this.regIndexes);
        }
        WrkrInteractionArgsCPEntry wrkrRplyCPEntry = new WrkrInteractionArgsCPEntry(argRegs, retTypes);
        UTF8CPEntry sigCPEntry = new UTF8CPEntry(this.generateSig(retTypes));
        int sigCPIndex = currentPkgInfo.addCPEntry(sigCPEntry);
        wrkrRplyCPEntry.setTypesSignatureCPIndex(sigCPIndex);
        return currentPkgInfo.addCPEntry(wrkrRplyCPEntry);
    }

    public void visit(BLangService serviceNode) {
        /* ignore */
    }

    public void visit(BLangResource resourceNode) {
        /* ignore */
    }

    public void visit(BLangConnector connectorNode) {
        /* ignore */
    }

    public void visit(BLangAction actionNode) {
        /* ignore */
    }

    public void visit(BLangStruct structNode) {
        /* ignore */
    }

    public void visit(BLangEnum enumNode) {
        /* ignore */
    }

    public void visit(BLangIdentifier identifierNode) {
        /* ignore */
    }

    public void visit(BLangAnnotation annotationNode) {
        /* ignore */
    }

    public void visit(BLangAnnotAttribute annotationAttribute) {
        /* ignore */
    }

    public void visit(BLangAnnotationAttachment annAttachmentNode) {
        /* ignore */
    }

    public void visit(BLangAnnotAttachmentAttributeValue annotAttributeValue) {
        /* ignore */
    }

    public void visit(BLangAnnotAttachmentAttribute annotAttachmentAttribute) {
        /* ignore */
    }

    public void visit(BLangAssignment assignNode) {
        if (assignNode.declaredWithVar) {
            assignNode.varRefs.stream()
                    .filter(v -> v.type.tag != TypeTags.NONE)
                    .forEach(v -> {
                        v.regIndex = getNextIndex(v.type.tag, lvIndexes);
                        BLangVariableReference varRef = (BLangVariableReference) v;
                        LocalVariableInfo localVarInfo = getLocalVarAttributeInfo(varRef.symbol);
                        localVarAttrInfo.localVars.add(localVarInfo);
                    });
        }
        genNode(assignNode.expr, this.env);
        int[] rhsExprRegIndexes;
        if (assignNode.expr.isMultiReturnExpr()) {
            rhsExprRegIndexes = ((MultiReturnExpr) assignNode.expr).getRegIndexes();
        } else {
            rhsExprRegIndexes = new int[]{assignNode.expr.regIndex};
        }
        for (int i = 0; i < assignNode.varRefs.size(); i++) {
            BLangExpression lExpr = assignNode.varRefs.get(i);
            if (lExpr.type.tag == TypeTags.NONE) {
                continue;
            }
            rhsExprRegIndex = rhsExprRegIndexes[i];
            varAssignment = true;
            genNode(lExpr, this.env);
            varAssignment = false;
        }
    }

    public void visit(BLangAbort abortNode) {
        /* ignore */
    }

    public void visit(BLangContinue continueNode) {
        this.emit(this.loopResetInstructionStack.peek());
    }

    public void visit(BLangBreak breakNode) {
        this.emit(this.loopExitInstructionStack.peek());
    }

    public void visit(BLangReply replyNode) {
        /* ignore */
    }

    public void visit(BLangThrow throwNode) {
        /* ignore */
    }

    public void visit(BLanXMLNSStatement xmlnsStmtNode) {
        /* ignore */
    }

    public void visit(BLangComment commentNode) {
        /* ignore */
    }

    public void visit(BLangIf ifNode) {
        this.genNode(ifNode.expr, this.env);
        Instruction ifCondJumpInstr = InstructionFactory.get(InstructionCodes.BR_FALSE, ifNode.expr.regIndex, -1);
        this.emit(ifCondJumpInstr);
        this.genNode(ifNode.body, this.env);
        Instruction endJumpInstr = InstructionFactory.get(InstructionCodes.GOTO, -1);
        this.emit(endJumpInstr);
        ifCondJumpInstr.setOperand(1, this.nextIP());
        if (ifNode.elseStmt != null) {
            this.genNode(ifNode.elseStmt, this.env);
        }
        endJumpInstr.setOperand(0, this.nextIP());
    }

    public void visit(BLangWhile whileNode) {
        Instruction gotoTopJumpInstr = InstructionFactory.get(InstructionCodes.GOTO, this.nextIP());
        this.genNode(whileNode.expr, this.env);
        Instruction whileCondJumpInstr = InstructionFactory.get(InstructionCodes.BR_FALSE,
                whileNode.expr.regIndex, -1);
        Instruction exitLoopJumpInstr = InstructionFactory.get(InstructionCodes.GOTO, -1);
        this.emit(whileCondJumpInstr);
        this.loopResetInstructionStack.push(gotoTopJumpInstr);
        this.loopExitInstructionStack.push(exitLoopJumpInstr);
        this.genNode(whileNode.body, this.env);
        this.loopResetInstructionStack.pop();
        this.loopExitInstructionStack.pop();
        this.emit(gotoTopJumpInstr);
        int endIP = this.nextIP();
        whileCondJumpInstr.setOperand(1, endIP);
        exitLoopJumpInstr.setOperand(0, endIP);
    }

    public void visit(BLangTransaction transactionNode) {
        /* ignore */
    }

    public void visit(BLangTransform transformNode) {
        /* ignore */
    }

    public void visit(BLangTryCatchFinally tryNode) {
        /* ignore */
    }

    public void visit(BLangCatch catchNode) {
        /* ignore */
    }

    public void visit(BLangArrayLiteral arrayLiteral) {
        /* ignore */
    }

    public void visit(BLangRecordLiteral recordLiteral) {
        /* ignore */
    }

    public void visit(BLangTernaryExpr ternaryExpr) {
        /* ignore */
    }

    public void visit(BLangUnaryExpr unaryExpr) {
        /* ignore */
    }

    public void visit(BLangTypeConversionExpr conversionExpr) {
        /* ignore */
    }

    public void visit(BLangXMLQName xmlQName) {
        /* ignore */
    }

    public void visit(BLangXMLAttribute xmlAttribute) {
        /* ignore */
    }

    public void visit(BLangXMLElementLiteral xmlElementLiteral) {
        /* ignore */
    }

    public void visit(BLangXMLTextLiteral xmlTextLiteral) {
        /* ignore */
    }

    public void visit(BLangXMLCommentLiteral xmlCommentLiteral) {
        /* ignore */
    }

    public void visit(BLangXMLProcInsLiteral xmlProcInsLiteral) {
        /* ignore */
    }

    public void visit(BLangXMLQuotedString xmlQuotedString) {
        /* ignore */
    }

    public void visit(BLangStringTemplateLiteral stringTemplateLiteral) {
        /* ignore */
    }
}
