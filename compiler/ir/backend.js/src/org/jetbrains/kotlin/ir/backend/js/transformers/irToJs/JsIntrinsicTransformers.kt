/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.getValueArgumentOrThrow
import org.jetbrains.kotlin.ir.expressions.getTypeArgumentOrThrow
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassBoxing
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassUnboxing
import java.lang.Exception

typealias IrCallTransformer = (IrCall, context: JsGenerationContext) -> JsExpression

class JsIntrinsicTransformers(backendContext: JsIrBackendContext) {
    private val transformers: Map<IrSymbol, IrCallTransformer>
    val icUtils = backendContext.inlineClassesUtils

    init {
        val intrinsics = backendContext.intrinsics

        transformers = hashMapOf()

        transformers.apply {
            binOp(intrinsics.jsEqeqeq, JsBinaryOperator.REF_EQ)
            binOp(intrinsics.jsNotEqeq, JsBinaryOperator.REF_NEQ)
            binOp(intrinsics.jsEqeq, JsBinaryOperator.EQ)
            binOp(intrinsics.jsNotEq, JsBinaryOperator.NEQ)

            binOp(intrinsics.jsGt, JsBinaryOperator.GT)
            binOp(intrinsics.jsGtEq, JsBinaryOperator.GTE)
            binOp(intrinsics.jsLt, JsBinaryOperator.LT)
            binOp(intrinsics.jsLtEq, JsBinaryOperator.LTE)

            prefixOp(intrinsics.jsNot, JsUnaryOperator.NOT)
            binOp(intrinsics.jsAnd, JsBinaryOperator.AND)
            binOp(intrinsics.jsOr, JsBinaryOperator.OR)

            prefixOp(intrinsics.jsUnaryPlus, JsUnaryOperator.POS)
            prefixOp(intrinsics.jsUnaryMinus, JsUnaryOperator.NEG)

            prefixOp(intrinsics.jsPrefixInc, JsUnaryOperator.INC)
            postfixOp(intrinsics.jsPostfixInc, JsUnaryOperator.INC)
            prefixOp(intrinsics.jsPrefixDec, JsUnaryOperator.DEC)
            postfixOp(intrinsics.jsPostfixDec, JsUnaryOperator.DEC)

            prefixOp(intrinsics.jsDelete, JsUnaryOperator.DELETE)

            binOp(intrinsics.jsPlus, JsBinaryOperator.ADD)
            binOp(intrinsics.jsMinus, JsBinaryOperator.SUB)
            binOp(intrinsics.jsMult, JsBinaryOperator.MUL)
            binOp(intrinsics.jsDiv, JsBinaryOperator.DIV)
            binOp(intrinsics.jsMod, JsBinaryOperator.MOD)

            binOp(intrinsics.jsPlusAssign, JsBinaryOperator.ASG_ADD)
            binOp(intrinsics.jsMinusAssign, JsBinaryOperator.ASG_SUB)
            binOp(intrinsics.jsMultAssign, JsBinaryOperator.ASG_MUL)
            binOp(intrinsics.jsDivAssign, JsBinaryOperator.ASG_DIV)
            binOp(intrinsics.jsModAssign, JsBinaryOperator.ASG_MOD)

            binOp(intrinsics.jsBitAnd, JsBinaryOperator.BIT_AND)
            binOp(intrinsics.jsBitOr, JsBinaryOperator.BIT_OR)
            binOp(intrinsics.jsBitXor, JsBinaryOperator.BIT_XOR)
            prefixOp(intrinsics.jsBitNot, JsUnaryOperator.BIT_NOT)

            binOp(intrinsics.jsBitShiftR, JsBinaryOperator.SHR)
            binOp(intrinsics.jsBitShiftRU, JsBinaryOperator.SHRU)
            binOp(intrinsics.jsBitShiftL, JsBinaryOperator.SHL)

            binOp(intrinsics.jsInstanceOf, JsBinaryOperator.INSTANCEOF)

            binOp(intrinsics.jsIn, JsBinaryOperator.INOP)

            prefixOp(intrinsics.jsTypeOf, JsUnaryOperator.TYPEOF)

            add(intrinsics.jsIsEs6) { _, _ -> JsBooleanLiteral(backendContext.es6mode) }

            add(intrinsics.jsObjectCreateSymbol) { call, context ->
                val classToCreate = call.getTypeArgumentOrThrow(0).classifierOrFail.owner as IrClass
                val className = classToCreate.getClassRef(context.staticContext)
                objectCreate(prototypeOf(className, context.staticContext), context.staticContext)
            }

            add(intrinsics.jsClass) { call, context ->
                val typeArgument = call.getTypeArgument(0)
                typeArgument?.getClassRef(context.staticContext)
                    ?: compilationException(
                        "Type argument of jsClass must be statically known class",
                        typeArgument
                    )
            }

            add(intrinsics.jsNewTarget) { _, _ ->
                JsNameRef(JsName("target", false), JsNameRef(JsName("new", false)))
            }

            add(intrinsics.jsOpenInitializerBox) { call, context ->
                val arguments = translateCallArguments(call, context)

                JsInvocation(
                    JsNameRef("Object.assign"),
                    arguments
                )
            }

            add(intrinsics.jsEmptyObject) { _, _ ->
                JsObjectLiteral()
            }

            addIfNotNull(intrinsics.jsCode) { call, _ ->
                compilationException(
                    "Should not be called",
                    call
                )
            }

            add(intrinsics.jsArrayLength) { call, context ->
                val args = translateCallArguments(call, context)
                JsNameRef("length", args[0])
            }

            add(intrinsics.jsArrayGet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                JsArrayAccess(array, index)
            }

            add(intrinsics.jsArraySet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                val value = args[2]
                JsBinaryOperation(JsBinaryOperator.ASG, JsArrayAccess(array, index), value)
            }

            add(intrinsics.arrayLiteral) { call, context ->
                translateCallArguments(call, context).single()
            }

            for (intrinsic in arrayOf(
                intrinsics.jsArrayLike2Array,
                intrinsics.jsSliceArrayLikeFromIndex,
                intrinsics.jsSliceArrayLikeFromIndexToIndex
            )) {
                add(intrinsic) { call, context ->
                    val args = translateCallArguments(call, context)
                    JsInvocation(JsNameRef(Namer.CALL_FUNCTION, JsNameRef(Namer.SLICE_FUNCTION, JsArrayLiteral())), args)
                }
            }

            add(intrinsics.jsArraySlice) { call, context ->
                JsInvocation(JsNameRef(Namer.SLICE_FUNCTION, translateCallArguments(call, context).single()))
            }

            for ((type, prefix) in intrinsics.primitiveToTypedArrayMap) {
                add(intrinsics.primitiveToSizeConstructor[type] ?: doError(intrinsics.primitiveToSizeConstructor)) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
                add(intrinsics.primitiveToLiteralConstructor[type] ?: doError(intrinsics.primitiveToSizeConstructor)) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
            }

            add(intrinsics.jsBoxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = icUtils.getInlinedClassOrThrow(call.getTypeArgumentOrThrow(0))
                val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }

                JsNew(constructor.getConstructorRef(context.staticContext), listOf(arg))
                    .apply { isInlineClassBoxing = true }
            }

            add(intrinsics.jsUnboxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = icUtils.getInlinedClassOrThrow(call.getTypeArgumentOrThrow(1))
                val field = getInlineClassBackingField(inlineClass)
                val fieldName = context.getNameForField(field)
                JsNameRef(fieldName, arg).apply { isInlineClassUnboxing = true }
            }

            add(intrinsics.jsCall) { call, context: JsGenerationContext ->
                val args = translateCallArguments(call, context)
                val receiver = args[0]
                val target = args[1]
                val varargs = args[2] as? JsArrayLiteral ?: error("Expect to have JsArrayLiteral, because of vararg with dynamic element type")

                val callRef = JsNameRef(Namer.CALL_FUNCTION, target)
                JsInvocation(callRef, receiver, *varargs.expressions.toTypedArray())
            }

            add(intrinsics.jsBind) { call, context: JsGenerationContext ->
                val receiver = call.getValueArgumentOrThrow(0)
                val jsReceiver = receiver.accept(IrElementToJsExpressionTransformer(), context)
                val jsBindTarget = when (val target = call.getValueArgumentOrThrow(1)) {
                    is IrFunctionReference -> {
                        val superClass = call.superQualifierSymbol!!
                        val functionName = context.getNameForMemberFunction(target.symbol.owner as IrSimpleFunction)
                        val superName = superClass.owner.getClassRef(context.staticContext)
                        JsNameRef(functionName, prototypeOf(superName, context.staticContext))
                    }
                    is IrFunctionExpression -> target.accept(IrElementToJsExpressionTransformer(), context)
                    else -> compilationException(
                        "The 'target' argument of 'jsBind' must be either IrFunctionReference or IrFunctionExpression",
                        call
                    )
                }
                val bindRef = JsNameRef(Namer.BIND_FUNCTION, jsBindTarget)
                JsInvocation(bindRef, jsReceiver)
            }

            add(intrinsics.jsContexfulRef) { call, context: JsGenerationContext ->
                val receiver = call.getValueArgumentOrThrow(0)
                val jsReceiver = receiver.accept(IrElementToJsExpressionTransformer(), context)
                val target = call.getValueArgumentOrThrow(1) as IrRawFunctionReference
                val jsTarget = context.getNameForMemberFunction(target.symbol.owner as IrSimpleFunction)

                JsNameRef(jsTarget, jsReceiver)
            }

            add(intrinsics.unreachable) { _, _ ->
                JsInvocation(JsNameRef(Namer.UNREACHABLE_NAME))
            }

            add(intrinsics.createSharedBox) { call, context: JsGenerationContext ->
                val arg = translateCallArguments(call, context).single()
                JsObjectLiteral(listOf(JsPropertyInitializer(JsNameRef(Namer.SHARED_BOX_V), arg)))
            }

            add(intrinsics.readSharedBox) { call, context: JsGenerationContext ->
                val box = translateCallArguments(call, context).single()
                JsNameRef(Namer.SHARED_BOX_V, box)
            }

            add(intrinsics.writeSharedBox) { call, context: JsGenerationContext ->
                val args = translateCallArguments(call, context)
                val box = args[0]
                val value = args[1]
                jsAssignment(JsNameRef(Namer.SHARED_BOX_V, box), value)
            }

            val suspendInvokeTransform: (IrCall, JsGenerationContext) -> JsExpression = { call, context: JsGenerationContext ->
                // Because it is intrinsic, we know everything about this function
                // There is callable reference as extension receiver
                val invokeFun = invokeFunForLambda(call)

                val jsInvokeFunName = context.getNameForMemberFunction(invokeFun)

                val jsExtensionReceiver = call.extensionReceiver?.accept(IrElementToJsExpressionTransformer(), context)!!
                val args = translateCallArguments(call, context)

                JsInvocation(JsNameRef(jsInvokeFunName, jsExtensionReceiver), args)
            }

            add(intrinsics.jsInvokeSuspendSuperType, suspendInvokeTransform)
            add(intrinsics.jsInvokeSuspendSuperTypeWithReceiver, suspendInvokeTransform)
            add(intrinsics.jsInvokeSuspendSuperTypeWithReceiverAndParam, suspendInvokeTransform)

            add(intrinsics.jsArguments) { _, _ -> Namer.ARGUMENTS }

            add(intrinsics.jsNewAnonymousClass) { call, context ->
                val baseClass = translateCallArguments(call, context).single() as JsNameRef
                JsClass(baseClass = baseClass)
            }

            add(intrinsics.void.owner.getter!!.symbol) { _, context ->
                val backingField = context.getNameForField(intrinsics.void.owner.backingField ?: doError(intrinsics.void.owner))
                JsNameRef(backingField)
            }
        }
    }

    operator fun get(symbol: IrSymbol): IrCallTransformer? = transformers[symbol]
}

private fun translateCallArguments(expression: IrCall, context: JsGenerationContext): List<JsExpression> {
    return translateCallArguments(expression, context, IrElementToJsExpressionTransformer(), false)
}

private fun MutableMap<IrSymbol, IrCallTransformer>.add(functionSymbol: IrSymbol, t: IrCallTransformer) {
    put(functionSymbol, t.wrappedWithErrorHandler())
}

private fun MutableMap<IrSymbol, IrCallTransformer>.add(function: IrSimpleFunction, t: IrCallTransformer) {
    put(function.symbol, t.wrappedWithErrorHandler())
}

private fun MutableMap<IrSymbol, IrCallTransformer>.addIfNotNull(symbol: IrSymbol?, t: IrCallTransformer) {
    if (symbol == null) return
    put(symbol, t.wrappedWithErrorHandler())
}

private fun MutableMap<IrSymbol, IrCallTransformer>.binOp(function: IrSimpleFunctionSymbol, op: JsBinaryOperator) {
    withTranslatedArgs(function) { JsBinaryOperation(op, it[0], it[1]) }
}

private fun MutableMap<IrSymbol, IrCallTransformer>.prefixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPrefixOperation(op, it[0]) }
}

private fun MutableMap<IrSymbol, IrCallTransformer>.postfixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPostfixOperation(op, it[0]) }
}

private inline fun MutableMap<IrSymbol, IrCallTransformer>.withTranslatedArgs(
    function: IrSimpleFunctionSymbol,
    crossinline t: (List<JsExpression>) -> JsExpression
) {
    put(function) { call, context ->
        try {
            t(translateCallArguments(call, context))
        } catch (e: Throwable) {
            doError(context.createErrorContextInfo(), cause = e)
        }
    }
}

private fun IrCallTransformer.wrappedWithErrorHandler(): IrCallTransformer = { call, context ->
    try {
        this@wrappedWithErrorHandler(call, context)
    } catch (e: Throwable) {
        doError(context.createErrorContextInfo(), cause = e)
    }
}

private fun doError(
    vararg args: Any,
    cause: Throwable? = null
): Nothing = throw IllegalStateException(
    "Unable to complete transformation. Args: ${args.joinToString()}",
    cause
)
