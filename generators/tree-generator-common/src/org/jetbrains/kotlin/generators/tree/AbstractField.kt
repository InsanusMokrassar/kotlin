/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

abstract class AbstractField {

    abstract val name: String

    abstract val typeRef: TypeRefWithNullability

    val nullable: Boolean
        get() = typeRef.nullable

    var kDoc: String? = null

    abstract val isVolatile: Boolean

    abstract val isFinal: Boolean

    abstract val isLateinit: Boolean

    abstract val isParameter: Boolean

    open val arbitraryImportables: MutableList<Importable> = mutableListOf()

    open var optInAnnotation: ClassRef<*>? = null

    abstract val isMutable: Boolean
    open val withGetter: Boolean get() = false
    open val customSetter: String? get() = null

    var deprecation: Deprecated? = null

    var visibility: Visibility = Visibility.PUBLIC

    var fromParent: Boolean = false

    /**
     * Whether this field can contain an element either directly or e.g. in a list.
     */
    open val containsElement: Boolean
        get() = typeRef is ElementOrRef<*, *> || this is ListField && baseType is ElementOrRef<*, *>

    open val defaultValueInImplementation: String? get() = null

    /**
     * @see org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
     */
    var useInBaseTransformerDetection = true

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        other as AbstractField
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
