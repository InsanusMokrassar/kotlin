/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

private const val konanHomePropertyKey = "org.jetbrains.kotlin.native.home"

val konanHomePath: String
    get() = System.getProperty(konanHomePropertyKey) ?: error("Missing System property: '$konanHomePropertyKey'")