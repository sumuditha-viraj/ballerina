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
package org.wso2.ballerinalang.compiler;

import org.ballerinalang.compiler.CompilerOptionName;
import org.ballerinalang.compiler.CompilerPhase;
import org.ballerinalang.model.tree.PackageNode;
import org.wso2.ballerinalang.compiler.codegen.CodeGenerator;
import org.wso2.ballerinalang.compiler.desugar.Desugar;
import org.wso2.ballerinalang.compiler.semantics.analyzer.CodeAnalyzer;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SemanticAnalyzer;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.CompilerOptions;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticLog;
import org.wso2.ballerinalang.programfile.ProgramFile;

/**
 * @since 0.94
 */
public class Compiler {

    private static final CompilerContext.Key<Compiler> COMPILER_KEY =
            new CompilerContext.Key<>();

    private CompilerOptions options;
    private DiagnosticLog dlog;
    private PackageLoader pkgLoader;
    private SymbolTable symbolTable;
    private SemanticAnalyzer semAnalyzer;
    private CodeAnalyzer codeAnalyzer;
    private Desugar desugar;
    private CodeGenerator codeGenerator;

    private CompilerPhase compilerPhase;
    private ProgramFile programFile;
    private BLangPackage pkgNode;

    public static Compiler getInstance(CompilerContext context) {
        Compiler compiler = context.get(COMPILER_KEY);
        if (compiler == null) {
            compiler = new Compiler(context);
        }
        return compiler;
    }

    public Compiler(CompilerContext context) {
        context.put(COMPILER_KEY, this);

        this.options = CompilerOptions.getInstance(context);
        this.dlog = DiagnosticLog.getInstance(context);
        this.pkgLoader = PackageLoader.getInstance(context);
        this.symbolTable = SymbolTable.getInstance(context);
        this.semAnalyzer = SemanticAnalyzer.getInstance(context);
        this.codeAnalyzer = CodeAnalyzer.getInstance(context);
        this.desugar = Desugar.getInstance(context);
        this.codeGenerator = CodeGenerator.getInstance(context);

        this.compilerPhase = getCompilerPhase();
    }

    public BLangPackage compile(String sourcePkg) {
        loadBuiltInPackage();
        BLangPackage bLangPackage = null;
        switch (compilerPhase) {
            case DEFINE:
                bLangPackage = define(sourcePkg);
                break;
            case TYPE_CHECK:
                bLangPackage = typeCheck(define(sourcePkg));
                break;
            case CODE_ANALYZE:
                bLangPackage = codeAnalyze(typeCheck(define(sourcePkg)));
                break;
            case DESUGAR:
                desugar(codeAnalyze(typeCheck(define(sourcePkg))));
                break;
            default:
                gen(desugar(codeAnalyze(typeCheck(define(sourcePkg)))));
                break;
        }

        return bLangPackage;
    }

    private void loadBuiltInPackage() {
        BLangPackage builtInCorePkg = this.desugar(this.codeAnalyze(this.semAnalyzer.analyze(
                this.pkgLoader.loadEntryPackage(Names.BUILTIN_PACKAGE_CORE.value))));
        symbolTable.createErrorTypes();
        symbolTable.loadOperators();
        BLangPackage builtInPkg = this.desugar(this.codeAnalyze(this.semAnalyzer.analyze(
                this.pkgLoader.loadEntryPackage(Names.BUILTIN_PACKAGE.value))));
        builtInCorePkg.getStructs().forEach(s -> builtInPkg.getStructs().add(s));
        symbolTable.builtInPackageSymbol = builtInPkg.symbol;
    }

    public ProgramFile getCompiledProgram() {
        return programFile;
    }

    public ProgramFile getCompiledPackage() {
        // TODO
        return null;
    }

    public PackageNode getAST() {
        return pkgNode;
    }


    // private methods

    private BLangPackage define(String sourcePkg) {
        if (stopCompilation(CompilerPhase.DEFINE)) {
            return null;
        }

        return pkgNode = pkgLoader.loadEntryPackage(sourcePkg);
    }

    private BLangPackage typeCheck(BLangPackage pkgNode) {
        if (stopCompilation(CompilerPhase.TYPE_CHECK)) {
            return pkgNode;
        }

        return semAnalyzer.analyze(pkgNode);
    }

    private BLangPackage codeAnalyze(BLangPackage pkgNode) {
        if (stopCompilation(CompilerPhase.CODE_ANALYZE)) {
            return pkgNode;
        }

        return codeAnalyzer.analyze(pkgNode);
    }

    private BLangPackage desugar(BLangPackage pkgNode) {
        if (stopCompilation(CompilerPhase.DESUGAR)) {
            return pkgNode;
        }

        return desugar.perform(pkgNode);
    }

    private void gen(BLangPackage pkgNode) {
        if (stopCompilation(CompilerPhase.CODE_GEN)) {
            return;
        }

        programFile = this.codeGenerator.generate(pkgNode);
    }

    private CompilerPhase getCompilerPhase() {
        String phaseName = options.get(CompilerOptionName.COMPILER_PHASE);
        if (phaseName == null || phaseName.isEmpty()) {
            return CompilerPhase.CODE_GEN;
        }

        return CompilerPhase.fromValue(phaseName);
    }

    private boolean stopCompilation(CompilerPhase phase) {
        return (phase == CompilerPhase.DESUGAR ||
                phase == CompilerPhase.CODE_GEN) &&
                dlog.errorCount > 0;
    }
}
