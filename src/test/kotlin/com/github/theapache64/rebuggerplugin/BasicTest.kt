package com.github.theapache64.rebuggerplugin

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.IndentsModel
import com.intellij.openapi.editor.impl.IndentsModelImpl
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.formatter.kotlinCommonSettings
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class BasicTest : BasePlatformTestCase() {

    fun testKotlinFile() {
        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, "fun something() {}")
        val ktFile = assertInstanceOf(psiFile, KtFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, ktFile.virtualFile))
    }

    @Test
    fun testAddRebugger() {
        // loading file
        val psiFile = myFixture.configureByFile("Basic.kt")

        myFixture.testAction(AddRebuggerHereAction())
        myFixture.checkResultByFile("BasicAfter.kt")
    }

    override fun getTestDataPath() = "src/test/testData/basic"
}
