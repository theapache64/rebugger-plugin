package com.github.theapache64.rebuggerplugin

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class ExternalVarsTest : BasePlatformTestCase() {

    @Test
    fun testExternalParam() {
        // loading file
        myFixture.configureByFile("ExternalParam.kt")
        myFixture.testAction(AddRebuggerHereAction())
        myFixture.checkResultByFile("ExternalParamAfter.kt")
    }

    override fun getTestDataPath() = "src/test/testData/external_param"
}
