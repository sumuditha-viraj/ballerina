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
package org.wso2.ballerinalang.compiler.semantics.analyzer;

import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.util.diagnostic.DiagnosticCode;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BCastOperatorSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BOperatorSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.Types.RecordKind;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKey;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValue;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeCastExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.expressions.MultiReturnExpr;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticLog;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 0.94
 */
public class TypeChecker extends BLangNodeVisitor {

    private static final CompilerContext.Key<TypeChecker> TYPE_CHECKER_KEY =
            new CompilerContext.Key<>();

    private Names names;
    private SymbolTable symTable;
    private SymbolResolver symResolver;
    private DiagnosticLog dlog;

    private SymbolEnv env;

    /**
     * Expected types or inherited types
     */
    private List<BType> expTypes;

    private DiagnosticCode diagCode;
    private List<BType> resultTypes;


    public static TypeChecker getInstance(CompilerContext context) {
        TypeChecker typeChecker = context.get(TYPE_CHECKER_KEY);
        if (typeChecker == null) {
            typeChecker = new TypeChecker(context);
        }

        return typeChecker;
    }

    public TypeChecker(CompilerContext context) {
        context.put(TYPE_CHECKER_KEY, this);

        this.names = Names.getInstance(context);
        this.symTable = SymbolTable.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.dlog = DiagnosticLog.getInstance(context);
    }

    public List<BType> checkExpr(BLangExpression expr, SymbolEnv env) {
        return checkExpr(expr, env, Lists.of(symTable.noType));
    }

    public List<BType> checkExpr(BLangExpression expr, SymbolEnv env, List<BType> expTypes) {
        return checkExpr(expr, env, expTypes, DiagnosticCode.INCOMPATIBLE_TYPES);
    }

    /**
     * Check the given list of expressions against the given expected types.
     *
     * @param exprs   list of expressions to be analyzed
     * @param env     current symbol environment
     * @param expType expected type
     * @return the actual types of the given list of expressions
     */
    public List<BType> checkExprs(List<BLangExpression> exprs, SymbolEnv env, BType expType) {
        List<BType> resTypes = new ArrayList<>(exprs.size());
        for (BLangExpression expr : exprs) {
            resTypes.add(checkExpr(expr, env, Lists.of(expType)).get(0));
        }
        return resTypes;
    }

    public List<BType> checkExpr(BLangExpression expr, SymbolEnv env, List<BType> expTypes, DiagnosticCode diagCode) {
        // TODO Check the possibility of using a try/finally here
        SymbolEnv prevEnv = this.env;
        List<BType> preExpTypes = this.expTypes;
        DiagnosticCode preDiagCode = this.diagCode;
        this.env = env;
        this.diagCode = diagCode;
        this.expTypes = verifyAndGetExpectedTypes(expr, expTypes);

        expr.accept(this);

        setExprType(expr, expTypes);
        this.env = prevEnv;
        this.expTypes = preExpTypes;
        this.diagCode = preDiagCode;
        return resultTypes;
    }

    public void checkNodeType(BLangNode node, BType expType, DiagnosticCode diagCode) {
        checkType(node.pos, node.type, expType, diagCode);
    }


    // Expressions

    public void visit(BLangLiteral literalExpr) {
        BType literalType = symTable.getTypeFromTag(literalExpr.typeTag);
        resultTypes = checkTypes(literalExpr, Lists.of(literalType), expTypes);
    }

    public void visit(BLangArrayLiteral arrayLiteral) {
        // Check whether the expected type is an array type
        // var a = []; and var a = [1,2,3,4]; are illegal statements, because we cannot infer the type here.
        BType actualType = symTable.errType;

        int expTypeTag = expTypes.get(0).tag;
        if (expTypeTag == TypeTags.NONE) {
            dlog.error(arrayLiteral.pos, DiagnosticCode.ARRAY_LITERAL_NOT_ALLOWED);

        } else if (expTypeTag != TypeTags.ARRAY && expTypeTag != TypeTags.ERROR) {
            dlog.error(arrayLiteral.pos, DiagnosticCode.INVALID_LITERAL_FOR_TYPE, expTypes.get(0));

        } else if (expTypeTag != TypeTags.ERROR) {
            BArrayType arrayType = (BArrayType) expTypes.get(0);
            checkExprs(arrayLiteral.exprs, this.env, arrayType.eType);
            actualType = new BArrayType(arrayType.eType);
        }

        resultTypes = checkTypes(arrayLiteral, Lists.of(actualType), expTypes);
    }

    public void visit(BLangRecordLiteral recordLiteral) {
        BType actualType = symTable.errType;
        int expTypeTag = expTypes.get(0).tag;
        if (expTypeTag == TypeTags.NONE) {
            // var a = {}
            // Change the expected type to map
            expTypes = Lists.of(symTable.mapType);
        }

        if (expTypeTag == TypeTags.JSON ||
                expTypeTag == TypeTags.MAP ||
                expTypeTag == TypeTags.STRUCT) {
            recordLiteral.keyValuePairs.forEach(keyValuePair ->
                    checkRecLiteralKeyValue(keyValuePair, expTypes.get(0)));
            actualType = expTypes.get(0);
        }

        resultTypes = checkTypes(recordLiteral, Lists.of(actualType), expTypes);
    }

    public void visit(BLangSimpleVarRef varRefExpr) {
        // Set error type as the actual type.
        BType actualType = symTable.errType;

        Name varName = names.fromIdNode(varRefExpr.variableName);
        if (varName == Names.IGNORE) {
            if (varRefExpr.lhsVar) {
                varRefExpr.type = this.symTable.noType;
            } else {
                varRefExpr.type = this.symTable.errType;
                dlog.error(varRefExpr.pos, DiagnosticCode.UNDERSCORE_NOT_ALLOWED);
            }
            resultTypes = Lists.of(varRefExpr.type);
            return;
        }
        BSymbol symbol = symResolver.lookupSymbol(env, varName, SymTag.VARIABLE);
        if (symbol == symTable.notFoundSymbol) {
            dlog.error(varRefExpr.pos, DiagnosticCode.UNDEFINED_SYMBOL, varName.toString());
        } else {
            BVarSymbol varSym = (BVarSymbol) symbol;
            checkSefReferences(varRefExpr.pos, env, varSym);
            varRefExpr.symbol = varSym;
            actualType = varSym.type;
        }

        // Check type compatibility
        resultTypes = checkTypes(varRefExpr, Lists.of(actualType), expTypes);
    }

    public void visit(BLangFieldBasedAccess fieldAccessExpr) {
        BType actualType = symTable.errType;
        // First analyze the variable reference expression.
        checkExpr(fieldAccessExpr.expr, this.env, Lists.of(symTable.noType));

        BType varRefType = fieldAccessExpr.expr.type;
        switch (varRefType.tag) {
            case TypeTags.STRUCT:
                Name fieldName = names.fromIdNode(fieldAccessExpr.field);
                actualType = checkStructFieldAccess(fieldAccessExpr, fieldName, varRefType);
                break;
            case TypeTags.MAP:
                actualType = ((BMapType) varRefType).getConstraint();
                break;
            case TypeTags.JSON:
                // TODO with constrained json
                break;
            case TypeTags.ERROR:
                // Do nothing
                break;
            default:
                dlog.error(fieldAccessExpr.pos, DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_FIELD_ACCESS,
                        varRefType);
        }

        resultTypes = checkTypes(fieldAccessExpr, Lists.of(actualType), this.expTypes);
    }

    public void visit(BLangIndexBasedAccess indexBasedAccessExpr) {
        BType actualType = symTable.errType;
        // First analyze the variable reference expression.
        checkExpr(indexBasedAccessExpr.expr, this.env, Lists.of(symTable.noType));

        BType indexExprType;
        BType varRefType = indexBasedAccessExpr.expr.type;
        BLangExpression indexExpr = indexBasedAccessExpr.indexExpr;
        switch (varRefType.tag) {
            case TypeTags.STRUCT:
                indexExprType = checkIndexExprForStructFieldAccess(indexExpr);
                if (indexExprType.tag == TypeTags.STRING) {
                    String fieldName = (String) ((BLangLiteral) indexExpr).value;
                    actualType = checkStructFieldAccess(indexBasedAccessExpr, names.fromString(fieldName), varRefType);
                }
                break;
            case TypeTags.MAP:
                indexExprType = checkExpr(indexExpr, this.env,
                        Lists.of(symTable.stringType)).get(0);
                if (indexExprType.tag == TypeTags.STRING) {
                    actualType = ((BMapType) varRefType).getConstraint();
                }
                break;
            case TypeTags.JSON:
                // TODO with constrained json
                break;
            case TypeTags.ARRAY:
                indexExprType = checkExpr(indexExpr, this.env,
                        Lists.of(symTable.intType)).get(0);
                if (indexExprType.tag == TypeTags.INT) {
                    actualType = ((BArrayType) varRefType).getElementType();
                }
                break;
            case TypeTags.ERROR:
                // Do nothing
                break;
            default:
                dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_INDEXING,
                        indexBasedAccessExpr.expr.type);
        }

        resultTypes = checkTypes(indexBasedAccessExpr, Lists.of(actualType), this.expTypes);
    }

    public void visit(BLangInvocation iExpr) {
        // Variable ref expression null means this is the leaf node of the variable ref expression tree
        // e.g. foo();, foo(), foo().k;
        if (iExpr.expr == null) {
            // This is a function invocation expression. e.g. foo()
            checkFunctionInvocationExpr(iExpr);
            return;
        }

        // TODO other types of invocation expressions
        //TODO pkg alias should be null or empty here.

//        checkExpr(iExpr.expr, this.env, Lists.of(symTable.noType));
    }

    public void visit(BLangBinaryExpr binaryExpr) {
        BType lhsType = checkExpr(binaryExpr.lhsExpr, env).get(0);
        BType rhsType = checkExpr(binaryExpr.rhsExpr, env).get(0);

        // Set error type as the actual type.
        BType actualType = symTable.errType;

        // Look up operator symbol if both rhs and lhs types are error types
        if (lhsType != symTable.errType && rhsType != symTable.errType) {
            BSymbol opSymbol = symResolver.resolveBinaryOperator(binaryExpr.opKind, lhsType, rhsType);
            if (opSymbol == symTable.notFoundSymbol) {
                dlog.error(binaryExpr.pos, DiagnosticCode.BINARY_OP_INCOMPATIBLE_TYPES,
                        binaryExpr.opKind, lhsType, rhsType);
            } else {
                binaryExpr.opSymbol = (BOperatorSymbol) opSymbol;
                actualType = opSymbol.type.getReturnTypes().get(0);
            }
        }

        resultTypes = checkTypes(binaryExpr, Lists.of(actualType), expTypes);
    }

    public void visit(BLangUnaryExpr unaryExpr) {
        throw new AssertionError();
    }

    public void visit(BLangTypeCastExpr castExpr) {
        // Set error type as the actual type.
        List<BType> actualTypes = getListWithErrorTypes(expTypes.size());

        BType targetType = symResolver.resolveTypeNode(castExpr.typeNode, env);
        BType sourceType = checkExpr(castExpr.expr, env, Lists.of(symTable.noType)).get(0);

        // Lookup type explicit cast operator symbol
        BSymbol symbol = symResolver.resolveExplicitCastOperator(sourceType, targetType);
        if (symbol == symTable.notFoundSymbol) {
            dlog.error(castExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES_CAST, sourceType, targetType);
        } else {
            BCastOperatorSymbol castSym = (BCastOperatorSymbol) symbol;
            castExpr.castSymbol = castSym;
            actualTypes = getActualTypesOfCastExpr(castExpr, targetType, sourceType, castSym);
        }

        resultTypes = checkTypes(castExpr, actualTypes, expTypes);
    }

    public void visit(BLangTypeConversionExpr conversionExpr) {
        throw new AssertionError();
    }


    // Private methods

    private List<BType> checkTypes(BLangExpression node, List<BType> actualTypes, List<BType> expTypes) {
        List<BType> resTypes = new ArrayList<>();
        for (int i = 0; i < actualTypes.size(); i++) {
            resTypes.add(checkType(node, actualTypes.get(i), expTypes.get(i)));
        }
        return resTypes;
    }

    private BType checkType(BLangExpression node, BType type, BType expType) {
        return checkType(node, type, expType, DiagnosticCode.INCOMPATIBLE_TYPES);
    }

    private BType checkType(BLangExpression node, BType type, BType expType, DiagnosticCode diagCode) {
        return node.type = checkType(node.pos, type, expType, diagCode);
    }

    private BType checkType(DiagnosticPos pos, BType type, BType expType, DiagnosticCode diagCode) {
        if (expType.tag == TypeTags.ERROR) {
            return expType;
        } else if (expType.tag == TypeTags.NONE) {
            return type;
        } else if (type.tag == TypeTags.ERROR) {
            return type;
        } else if (type.tag == expType.tag) {
            return type;
        } else if (expType.tag == TypeTags.ANY) {
            // TODO Implicit cast possible
            return expType;
        }

        // TODO Add more logic to check type compatibility assignability etc.

        // e.g. incompatible types: expected 'int', found 'string'
        dlog.error(pos, diagCode, expType, type);
        return symTable.errType;
    }

    private void checkSefReferences(DiagnosticPos pos, SymbolEnv env, BVarSymbol varSymbol) {
        if (env.enclVarSym == varSymbol) {
            dlog.error(pos, DiagnosticCode.SELF_REFERENCE_VAR, varSymbol.name);
        }
    }

    private List<BType> verifyAndGetExpectedTypes(BLangExpression expr, List<BType> expTypes) {
        if (!expr.isMultiReturnExpr() && expTypes.size() > 1) {
            // This error will be reported after analyzing the expression
            return Lists.of(symTable.errType);
        }

        return expTypes;
    }

    private void setExprType(BLangExpression expr, List<BType> expTypes) {
        int expected = expTypes.size();
        int actual = resultTypes.size();
        if (expr.isMultiReturnExpr()) {
            MultiReturnExpr multiReturnExpr = (MultiReturnExpr) expr;
            multiReturnExpr.setTypes(resultTypes);
        } else {
            if (expected > 1) {
                dlog.error(expr.pos, DiagnosticCode.ASSIGNMENT_COUNT_MISMATCH, expected, actual);
                resultTypes = getListWithErrorTypes(expected);
            }
        }

        if (resultTypes.size() > 0) {
            expr.type = resultTypes.get(0);
        }
    }

    private List<BType> getListWithErrorTypes(int count) {
        List<BType> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(symTable.errType);
        }

        return list;
    }

    private List<BType> getActualTypesOfCastExpr(BLangTypeCastExpr castExpr,
                                                 BType targetType,
                                                 BType sourceType,
                                                 BCastOperatorSymbol castSymbol) {
        // If this cast is an unsafe cast, then there MUST to be two expected types/variables
        // If this is an safe cast, then the error variable is optional
        int expected = expTypes.size();
        List<BType> actualTypes = getListWithErrorTypes(expected);
        if (castSymbol.safe && expected == 1) {
            actualTypes = Lists.of(castSymbol.type.getReturnTypes().get(0));

        } else if (!castSymbol.safe && expected == 1) {
            dlog.error(castExpr.pos, DiagnosticCode.UNSAFE_CAST_ATTEMPT, sourceType, targetType);

        } else if (expected == 2) {
            actualTypes = castSymbol.type.getReturnTypes();

        } else if (expected == 0 || expected > 2) {
            dlog.error(castExpr.pos, DiagnosticCode.ASSIGNMENT_COUNT_MISMATCH, expected, 2);
        }

        return actualTypes;
    }

    private void checkFunctionInvocationExpr(BLangInvocation iExpr) {
        List<BType> actualTypes = getListWithErrorTypes(expTypes.size());
        Name funcName = names.fromIdNode(iExpr.name);
        BSymbol funcSymbol = symResolver.resolveInvokable(iExpr.pos, DiagnosticCode.UNDEFINED_FUNCTION,
                this.env, names.fromIdNode(iExpr.pkgAlias), funcName);
        if (funcSymbol == symTable.errSymbol || funcSymbol == symTable.notFoundSymbol) {
            resultTypes = actualTypes;
            return;
        }

        // Set the resolved function symbol in the invocation expression.
        // This is used in the code generation phase.
        iExpr.symbol = (BInvokableSymbol) funcSymbol;

        List<BType> paramTypes = ((BInvokableType) funcSymbol.type).getParameterTypes();
        if (iExpr.argExprs.size() == 1 && iExpr.argExprs.get(0).getKind() == NodeKind.INVOCATION) {
            checkExpr(iExpr.argExprs.get(0), this.env, paramTypes);

        } else if (paramTypes.size() > iExpr.argExprs.size()) {
            dlog.error(iExpr.pos, DiagnosticCode.NOT_ENOUGH_ARGS_FUNC_CALL, funcName);

        } else if (paramTypes.size() < iExpr.argExprs.size()) {
            dlog.error(iExpr.pos, DiagnosticCode.TOO_MANY_ARGS_FUNC_CALL, funcName);

        } else {
            for (int i = 0; i < iExpr.argExprs.size(); i++) {
                checkExpr(iExpr.argExprs.get(i), this.env, Lists.of(paramTypes.get(i)));
            }
            actualTypes = funcSymbol.type.getReturnTypes();
        }

        checkInvocationReturnTypes(iExpr, actualTypes, funcName);
    }

    private void checkInvocationReturnTypes(BLangInvocation iExpr, List<BType> actualTypes, Name funcName) {
        int expected = expTypes.size();
        int actual = actualTypes.size();
        if (expected == 1 && actual > 1) {
            dlog.error(iExpr.pos, DiagnosticCode.MULTI_VAL_IN_SINGLE_VAL_CONTEXT, funcName);
            actualTypes = getListWithErrorTypes(expected);
        } else if (expected == 0) {
            // This could be from a expression statement. e.g foo();
            if (this.env.node.getKind() != NodeKind.EXPRESSION_STATEMENT) {
                dlog.error(iExpr.pos, DiagnosticCode.DOES_NOT_RETURN_VALUE, funcName);
            }
            actualTypes = new ArrayList<>(0);
        } else if (expected != actual) {
            // Special case actual == 0 scenario.. VOID Function
            dlog.error(iExpr.pos, DiagnosticCode.ASSIGNMENT_COUNT_MISMATCH, expected, actual);
            actualTypes = getListWithErrorTypes(expected);
        }

        resultTypes = checkTypes(iExpr, actualTypes, expTypes);
    }

    private void checkRecLiteralKeyValue(BLangRecordKeyValue keyValuePair, BType recType) {
        BType fieldType = symTable.errType;
        switch (recType.tag) {
            case TypeTags.STRUCT:
                fieldType = checkStructLiteralKeyExpr(keyValuePair.key, recType, RecordKind.STRUCT);
                break;
            case TypeTags.MAP:
                fieldType = checkMapLiteralKeyExpr(keyValuePair.key.expr, recType, RecordKind.STRUCT);
                break;
            case TypeTags.JSON:
                fieldType = checkJSONLiteralKeyExpr(keyValuePair.key.expr, recType, RecordKind.STRUCT);
        }

        BLangExpression valueExpr = keyValuePair.valueExpr;
        checkExpr(valueExpr, this.env, Lists.of(fieldType));
    }

    private BType checkStructLiteralKeyExpr(BLangRecordKey key, BType recordType, RecordKind recKind) {
        Name fieldName;
        BLangExpression keyExpr = key.expr;

        if (checkRecLiteralKeyExpr(keyExpr, recKind).tag != TypeTags.STRING) {
            return symTable.errType;

        } else if (keyExpr.getKind() == NodeKind.STRING_TEMPLATE_LITERAL) {
            // keys of the struct literal can only be string literals and identifiers
            dlog.error(keyExpr.pos, DiagnosticCode.STRING_TEMPLATE_LIT_NOT_ALLOWED);
            return symTable.errType;

        } else if (keyExpr.getKind() == NodeKind.LITERAL) {
            Object literalValue = ((BLangLiteral) keyExpr).value;
            fieldName = names.fromString((String) literalValue);

        } else {
            BLangSimpleVarRef varRef = (BLangSimpleVarRef) keyExpr;
            fieldName = names.fromIdNode(varRef.variableName);
        }

        // Check weather the struct field exists
        BSymbol fieldSymbol = symResolver.resolveStructField(keyExpr.pos, fieldName, recordType.tsymbol);
        if (fieldSymbol == symTable.notFoundSymbol) {
            return symTable.errType;
        }

        // Setting the struct field symbol for future use in Desugar and code generator.
        key.fieldSymbol = (BVarSymbol) fieldSymbol;
        return fieldSymbol.type;
    }

    private BType checkJSONLiteralKeyExpr(BLangExpression keyExpr, BType recordType, RecordKind recKind) {
        if (checkRecLiteralKeyExpr(keyExpr, recKind).tag != TypeTags.STRING) {
            return symTable.errType;
        }

        // TODO constrained json
        return symTable.jsonType;
    }

    private BType checkMapLiteralKeyExpr(BLangExpression keyExpr, BType recordType, RecordKind recKind) {
        if (checkRecLiteralKeyExpr(keyExpr, recKind).tag != TypeTags.STRING) {
            return symTable.errType;
        }

        // TODO constrained map
        return symTable.anyType;
    }

    private BType checkRecLiteralKeyExpr(BLangExpression keyExpr, RecordKind recKind) {
        // keys of the record literal can only be string literals, identifiers or string template literals
        if (keyExpr.getKind() != NodeKind.LITERAL &&
                keyExpr.getKind() != NodeKind.SIMPLE_VARIABLE_REF &&
                keyExpr.getKind() != NodeKind.STRING_TEMPLATE_LITERAL) {
            dlog.error(keyExpr.pos, DiagnosticCode.INVALID_FIELD_NAME_RECORD_LITERAL, recKind.value);
            return symTable.errType;

        } else if (keyExpr.getKind() == NodeKind.LITERAL ||
                keyExpr.getKind() == NodeKind.STRING_TEMPLATE_LITERAL) {
            return checkExpr(keyExpr, this.env, Lists.of(symTable.stringType)).get(0);
        }

        // If the key expression is an identifier then we simply set the type as string.
        keyExpr.type = symTable.stringType;
        return keyExpr.type;
    }

    private BType checkIndexExprForStructFieldAccess(BLangExpression indexExpr) {
        if (indexExpr.getKind() != NodeKind.LITERAL) {
            dlog.error(indexExpr.pos, DiagnosticCode.INVALID_INDEX_EXPR_STRUCT_FIELD_ACCESS);
            return symTable.errType;
        }

        return checkExpr(indexExpr, this.env, Lists.of(symTable.stringType)).get(0);
    }

    private BType checkStructFieldAccess(BLangVariableReference varReferExpr, Name fieldName, BType structType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(varReferExpr.pos, fieldName, structType.tsymbol);
        if (fieldSymbol == symTable.notFoundSymbol) {
            return symTable.errType;
        }

        // Setting the field symbol. This is used during the code generation phase
        varReferExpr.symbol = (BVarSymbol) fieldSymbol;
        return fieldSymbol.type;
    }
}
