/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.accessesTopLevelOrObjectField
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.isAccessToNotNullableObject
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrCompileTimeChecker(
    containingDeclaration: IrElement? = null,
    private val mode: EvaluationMode,
    private val interpreterConfiguration: IrInterpreterConfiguration,
) : IrElementVisitor<Boolean, Nothing?> {
    private val visitedStack = mutableListOf<IrElement>().apply { if (containingDeclaration != null) add(containingDeclaration) }

    private inline fun IrElement.asVisited(crossinline block: () -> Boolean): Boolean {
        visitedStack += this
        val result = block()
        visitedStack.removeAt(visitedStack.lastIndex)
        return result
    }

    override fun visitElement(element: IrElement, data: Nothing?) = false

    private fun IrDeclarationParent.getInnerDeclarations(): List<IrStatement> {
        return (this as? IrDeclarationContainer)?.declarations ?: (this as? IrStatementContainer)?.statements ?: emptyList()
    }

    private fun visitStatements(statements: List<IrStatement>): Boolean {
        return statements.all { it.accept(this, null) }
    }

    private fun visitConstructor(expression: IrFunctionAccessExpression): Boolean {
        val constructor = expression.symbol.owner

        if (!mode.canEvaluateFunction(constructor)) return false
        if (!visitValueArguments(expression, null)) return false
        return constructor.visitBodyIfNeeded() &&
                constructor.parentAsClass.declarations.filterIsInstance<IrAnonymousInitializer>().all { it.accept(this, null) }
    }

    private fun IrFunction.visitBodyIfNeeded(): Boolean {
        if (!mode.mustCheckBodyOf(this)) return true
        return this.asVisited { this.body?.accept(this@IrCompileTimeChecker, null) ?: true }
    }

    private fun IrCall.isGetterToConstVal(): Boolean {
        return symbol.owner.correspondingPropertySymbol?.owner?.isConst == true
    }

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        if (!mode.canEvaluateExpression(expression)) return false

        val owner = expression.symbol.owner
        if (!mode.canEvaluateFunction(owner)) return false

        // We disable `toFloat` folding on K/JS till `toFloat` is fixed (KT-35422)
        // This check must be placed here instead of CallInterceptor because we still
        // want to evaluate (1) `const val` expressions and (2) values in annotations.
        if (owner.name.asString() == "toFloat" && interpreterConfiguration.treatFloatInSpecialWay) {
            return super.visitCall(expression, data)
        }

        if (expression.dispatchReceiver.isAccessToNotNullableObject()) {
            return expression.isGetterToConstVal()
        }

        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true
        if (!visitValueArguments(expression, null)) return false
        val bodyComputable = owner.visitBodyIfNeeded()
        return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: Nothing?): Boolean {
        return (0 until expression.valueArgumentsCount)
            .map { expression.getValueArgument(it) }
            .none { it?.accept(this, data) == false }
    }

    override fun visitBody(body: IrBody, data: Nothing?): Boolean {
        return visitStatements(body.statements)
    }

    // We need this separate method to explicitly indicate that IrExpressionBody can be interpreted in any evaluation mode
    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): Boolean {
        return body.expression.accept(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): Boolean {
        if (!mode.canEvaluateBlock(expression)) return false

        // `IrReturnableBlock` will be created from IrCall after inline. We should do basically the same check as for IrCall.
        if (expression is IrReturnableBlock) {
            val inlinedBlock = expression.statements.singleOrNull() as? IrInlinedFunctionBlock
            if (inlinedBlock != null) return inlinedBlock.inlineCall.accept(this, data)
        }

        return visitStatements(expression.statements)
    }

    override fun visitComposite(expression: IrComposite, data: Nothing?): Boolean {
        if (!mode.canEvaluateComposite(expression)) return false

        return visitStatements(expression.statements)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): Boolean {
        return body.kind == IrSyntheticBodyKind.ENUM_VALUES || body.kind == IrSyntheticBodyKind.ENUM_VALUEOF
    }

    override fun visitConst(expression: IrConst<*>, data: Nothing?): Boolean {
        if (expression.type.getUnsignedType() != null) {
            val constructor = expression.type.classOrNull?.owner?.constructors?.singleOrNull() ?: return false
            return mode.canEvaluateFunction(constructor)
        }
        return true
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): Boolean {
        return expression.elements.any { it.accept(this, data) }
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): Boolean {
        return spread.expression.accept(this, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean {
        return expression.arguments.all { arg ->
            when (arg) {
                is IrGetObjectValue -> {
                    val toString = arg.symbol.owner.declarations
                        .filterIsInstance<IrSimpleFunction>()
                        .single { it.name.asString() == "toString" && it.valueParameters.isEmpty() && it.extensionReceiverParameter == null }

                    mode.canEvaluateFunction(toString) && toString.visitBodyIfNeeded()
                }

                else -> arg.accept(this, data)
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): Boolean {
        // to get object value we need nothing, but it will contain only fields with compile time annotation
        return true
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): Boolean {
        if (!mode.canEvaluateEnumValue(expression)) return false

        // we want to avoid recursion in cases like "enum class E(val srt: String) { OK(OK.name) }"
        if (visitedStack.contains(expression)) return true
        return expression.asVisited {
            expression.symbol.owner.initializerExpression?.accept(this, data) == true
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean {
        return visitedStack.contains(expression.symbol.owner.parent)
    }

    override fun visitSetValue(expression: IrSetValue, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean {
        val owner = expression.symbol.owner
        val property = owner.correspondingPropertySymbol?.owner
        val fqName = owner.fqName
        fun isJavaStaticWithPrimitiveOrString(): Boolean {
            return owner.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && owner.isStatic && owner.isFinal &&
                    (owner.type.isPrimitiveType() || owner.type.isStringClassType())
        }
        return when {
            // TODO fix later; used it here because java boolean resolves very strange,
            //  its type is flexible (so its not primitive) and there is no initializer at backing field
            fqName == "java.lang.Boolean.FALSE" || fqName == "java.lang.Boolean.TRUE" -> true
            isJavaStaticWithPrimitiveOrString() -> owner.initializer?.accept(this, data) == true
            expression.receiver == null -> property?.isConst == true && owner.initializer?.accept(this, null) == true
            owner.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && property?.isConst == true -> {
                val receiverComputable = (expression.receiver?.accept(this, null) ?: true)
                        || expression.isAccessToNotNullableObject()
                val initializerComputable = owner.initializer?.accept(this, null) ?: false
                receiverComputable && initializerComputable
            }
            else -> {
                val declarations = owner.parent.getInnerDeclarations()
                val getter = declarations.filterIsInstance<IrProperty>().singleOrNull { it == property }?.getter ?: return false
                visitedStack.contains(getter)
            }
        }
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean {
        if (expression.accessesTopLevelOrObjectField()) return false
        //todo check receiver?
        val property = expression.symbol.owner.correspondingPropertySymbol?.owner
        val declarations = expression.symbol.owner.parent.getInnerDeclarations()
        val setter = declarations.filterIsInstance<IrProperty>().single { it == property }.setter ?: return false
        return visitedStack.contains(setter) && expression.value.accept(this, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): Boolean {
        if (expression.symbol.owner.returnType.isAny()) return true
        return visitConstructor(expression)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): Boolean {
        val irClass = expression.classSymbol.owner
        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }

        return anonymousInitializer.all { init -> init.body.accept(this, data) } && classProperties.all {
            val propertyInitializer = it.backingField?.initializer?.expression
            if ((propertyInitializer as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) return@all true
            return@all (propertyInitializer?.accept(this, data) != false)
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): Boolean {
        if (!mode.canEvaluateCallableReference(expression)) return false

        val owner = expression.symbol.owner
        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true

        if (!mode.canEvaluateFunction(owner)) return false

        val bodyComputable = owner.visitBodyIfNeeded()
        return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): Boolean {
        if (!mode.canEvaluateFunctionExpression(expression)) return false

        val body = expression.function.body ?: return false
        return expression.function.asVisited { body.accept(this, data) }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Boolean {
        return when (expression.operator) {
            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, IrTypeOperator.IMPLICIT_NOTNULL, IrTypeOperator.SAM_CONVERSION,
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> {
                val operand = expression.typeOperand.classifierOrNull?.owner
                if (operand is IrTypeParameter && !visitedStack.contains(operand.parent)) return false
                expression.argument.accept(this, data)
            }
            else -> false
        }
    }

    override fun visitWhen(expression: IrWhen, data: Nothing?): Boolean {
        if (!mode.canEvaluateExpression(expression)) return false

        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: Nothing?): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitTry(aTry: IrTry, data: Nothing?): Boolean {
        if (!mode.canEvaluateExpression(aTry)) return false

        if (!aTry.tryResult.accept(this, data)) return false
        if (aTry.finallyExpression != null && aTry.finallyExpression?.accept(this, data) == false) return false
        return aTry.catches.all { it.result.accept(this, data) }
    }

    override fun visitBreak(jump: IrBreak, data: Nothing?): Boolean = visitedStack.contains(jump.loop)

    override fun visitContinue(jump: IrContinue, data: Nothing?): Boolean = visitedStack.contains(jump.loop)

    override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean {
        if (!visitedStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): Boolean {
        if (!mode.canEvaluateExpression(expression)) return false

        return expression.value.accept(this, data)
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): Boolean {
        if (!mode.canEvaluateCallableReference(expression)) return false

        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true

        val getterIsComputable = expression.getter?.let { mode.canEvaluateFunction(it.owner) } ?: true
        return dispatchReceiverComputable && extensionReceiverComputable && getterIsComputable
    }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): Boolean {
        return mode.canEvaluateClassReference(expression)
    }
}