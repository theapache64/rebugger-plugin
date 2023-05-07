package com.github.theapache64.rebuggerplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange


private val Caret.selectionRange: TextRange
    get() {
        return TextRange.create(this.selectionStart, this.selectionEnd);
    }

class GeneratePrintLnHereAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val ktFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        val offset = editor.caretModel.currentCaret.selectionEnd

        val trackSet = mutableSetOf<String>()
        ktFile.elementsInRange(editor.caretModel.currentCaret.selectionRange).forEach {
            when (it) {
                is KtParameterList ->{
                    it.parameters.mapNotNull { arg -> arg.name }.forEach { argName ->
                        trackSet.add(argName)
                    }
                }

                is KtParameter -> {
                    it.name?.let { paramName -> trackSet.add(paramName) }
                }

                is KtProperty -> {
                    it.childrenOfType<LeafPsiElement>().find { leafPsiElement ->
                        leafPsiElement.elementType.toString() == "IDENTIFIER"
                    }?.text?.let { valName ->
                        trackSet.add(valName)
                    }
                }
            }
        }

        // Adding code block
        editor.document.executeCommand(project, description = "Generate println()") {

            // Write code block
            val trackMap = StringBuilder().apply {
                for (varName in trackSet) {
                    this.append("\"$varName\" to $varName,\n")
                }
            }

            val println = "\nprintln(mapOf($trackMap))"
            insertString(offset, println)
            val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
            codeStyleManager.reformatText(ktFile, offset, offset + println.length)
        }
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val isKotlinFile = file?.extension == "kt"

        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val hasSelection = editor.caretModel.currentCaret.let {
            it.selectionStart != it.selectionEnd
        }
        event.presentation.isEnabledAndVisible = isKotlinFile && hasSelection
    }
}