package com.github.theapache64.rebuggerplugin

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.kotlin.idea.KotlinFileType
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
    fun testAddRebugger(){
        // loading file
        val psiFile = myFixture.configureByFile("Basic.kt")
        val ktFile = assertInstanceOf(psiFile, KtFile::class.java)
        myFixture.testAction(AddRebuggerHereAction())
        myFixture.checkResultByFile("BasicAfter.kt")
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }


    override fun getTestDataPath() = "src/test/testData/basic"
}
