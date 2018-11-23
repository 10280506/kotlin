/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

abstract class AbstractIsResultInspection(
    private val typeShortName: String,
    private val typeFullName: String,
    private val allowedSuffix: String,
    private val allowedNames: Set<String>,
    private val suggestedFunctionNameToCall: String,
    private val simplify: (KtExpression) -> Unit = {}
) : AbstractKotlinInspection() {

    protected fun analyzeFunction(function: KtFunction, toReport: PsiElement, holder: ProblemsHolder) {
        if (function is KtConstructor<*>) return
        val returnTypeText = function.getReturnTypeReference()?.text
        if (returnTypeText != null && typeShortName !in returnTypeText) return
        val name = (function as? KtNamedFunction)?.nameAsName?.asString()
        // Filter names from stdlib
        if (name in allowedNames) return
        if (function is KtNamedFunction) {
            val receiverTypeReference = function.receiverTypeReference
            // Filter Result extensions
            if (receiverTypeReference != null && typeShortName in receiverTypeReference.text) return
        }
        if (function is KtFunctionLiteral || returnTypeText == null) {
            // Heuristics to save performance
            val text = function.bodyExpression?.text
            // Check there is something creating Result in function text
            if (text != null && allowedNames.none { it in text } && typeShortName !in text && allowedSuffix !in text) return
        }

        val descriptor = function.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return
        val returnType = descriptor.returnType ?: return
        val returnTypeClass = returnType.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (returnTypeClass.fqNameSafe.asString() != typeFullName) return

        if (name != null && name.endsWith(allowedSuffix)) {
            analyzeFunctionWithAllowedSuffix(name, descriptor, toReport, holder)
        } else {
            holder.registerProblem(
                toReport,
                "Function returning $typeShortName with a name that does not end with $allowedSuffix",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                *listOfNotNull(
                    AddCallOrUnwrapTypeFix(
                        withBody = function.hasBody(),
                        functionName = suggestedFunctionNameToCall,
                        typeName = typeShortName,
                        simplify = simplify
                    ),
                    name?.let { RenameToFix("$it$allowedSuffix") }
                ).toTypedArray()
            )
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                analyzeFunction(function, function.nameIdentifier ?: function.funKeyword ?: function, holder)
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                analyzeFunction(lambdaExpression.functionLiteral, lambdaExpression.functionLiteral.lBrace, holder)
            }
        }
    }

    open fun analyzeFunctionWithAllowedSuffix(name: String, descriptor: FunctionDescriptor, toReport: PsiElement, holder: ProblemsHolder) {}
}