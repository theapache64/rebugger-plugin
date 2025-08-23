package com.github.theapache64.rebuggerplugin

import com.intellij.application.options.CodeStyle.getIndentOptions
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.psi.*

class AddRebuggerHereAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val ktFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        val offset = editor.caretModel.offset
        val currentElement = ktFile.findElementAt(offset)
        val function = currentElement?.parentOfType<KtNamedFunction>() ?: return

        val trackSet = mutableSetOf<String>()

        // Adding function argument
        function.valueParameters.mapNotNull { it.name }.toSet().let { argSet ->
            trackSet.addAll(argSet)
        }

        function.bodyBlockExpression?.let { processElementsRecursively(it, offset, trackSet) }

        // Adding code block
        editor.document.executeCommand(project, description = "Add Rebugger Call") {

            // Write code block
            val trackMap = StringBuilder().apply {
                for (varName in trackSet) {
                    this.append("\"$varName\" to $varName,\n")
                }
            }

            val rebuggerCall = "Rebugger(trackMap=mapOf($trackMap),)"
            insertString(offset, rebuggerCall)
            val codeStyleManager = CodeStyleManager.getInstance(project).apply {
                getIndentOptions(ktFile).apply {
                    CONTINUATION_INDENT_SIZE = 4
                    INDENT_SIZE = 4
                }
            }
            codeStyleManager.reformatText(ktFile, offset, offset + rebuggerCall.length)

            // Add import statement
            val classToImport = "com.theapache64.rebugger.Rebugger"
            if (!ktFile.hasImport(classToImport)) {
                // Hack due to https://twitter.com/theapache64/status/1644815369947824130
                val fileContent = ktFile.text
                val lastImportIndex = fileContent.lastIndexOf("import ")
                insertString(lastImportIndex, "import $classToImport\n")
            }
        }
    }

    private fun processElementsRecursively(
        element: PsiElement,
        offset: Int,
        trackSet: MutableSet<String>
    ) {

        if (element.startOffset < offset) {
            when (element) {
                is KtCallExpression -> {
                    element.valueArguments.forEach { arg ->
                        // capturing string template : eg: "Data is $data"
                        arg.stringTemplateExpression?.let { stringTemplate ->
                            stringTemplate.childrenOfType<KtBlockStringTemplateEntry>()
                                .forEach { longString ->
                                    longString.childrenOfType<KtDotQualifiedExpression>()
                                        .forEach { dotQualified ->
                                            trackSet.add(dotQualified.text)
                                        }
                                }
                        }

                        // capturing string template : eg: "Data is ${data.message}"
                        arg.childrenOfType<KtDotQualifiedExpression>()
                            .forEach { dotQualified ->
                                trackSet.add(dotQualified.text)
                            }
                    }
                }

                is KtProperty -> {
                    // Getting var/val property names
                    element.childrenOfType<LeafPsiElement>().find { leafPsiElement ->
                        leafPsiElement.elementType.toString() == "IDENTIFIER"
                    }?.text?.let { valName ->
                        trackSet.add(valName)
                    }
                }
            }
        }

        // Adding states between function header and cursor
        element.children.forEach { child ->
            processElementsRecursively(child, offset, trackSet)
        }
    }


    private fun KtFile.hasImport(classToImport: String): Boolean {
        return importDirectives.find { it.text.contains(classToImport) } != null
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val isKotlinFile = file?.extension == "kt"
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.caretModel?.currentCaret?.let {
            it.selectionStart != it.selectionEnd
        } ?: false
        event.presentation.isEnabledAndVisible = isKotlinFile && !hasSelection && editor != null
    }
}