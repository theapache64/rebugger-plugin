package com.github.theapache64.rebuggerplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class AddRebuggerHereAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val ktFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        val offset = editor.caretModel.offset
        val currentElement = ktFile.findElementAt(offset)
        val function = currentElement?.parentOfType<KtNamedFunction>() ?: return@actionPerformed

        val trackSet = mutableSetOf<String>()

        // Adding function argument
        function.valueParameters.mapNotNull { it.name }.toSet().let { argSet ->
            trackSet.addAll(argSet)
        }

        // Adding states between function header and cursor
        // TODO: Try recursive approach
        function.bodyBlockExpression
            ?.children
            ?.filter {
                // Filtering elements between function header and cursor position
                val isBeforeOffset = it.startOffset < offset
                isBeforeOffset && (it is KtCallExpression || it is KtProperty)
            }?.forEach {
                when (val element = it) {
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
            val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
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


    private fun KtFile.hasImport(classToImport: String): Boolean {
        return importDirectives.find { it.text.contains(classToImport) } != null
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val isKotlinFile = file?.extension == "kt"
        event.presentation.isEnabledAndVisible = isKotlinFile
    }
}