
package org.w3c.dom

//
// NOTE THIS FILE IS AUTO-GENERATED by the GeneratedJavaScriptStubs.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//

import js.noImpl

// Contains stub APIs for the W3C DOM API so we can delegate to the platform DOM instead


native public trait Attr: Node {
    fun getName(): String = js.noImpl
    fun getValue(): String = js.noImpl
    fun setValue(arg1: String): Unit = js.noImpl
    fun getSchemaTypeInfo(): TypeInfo = js.noImpl
    fun getSpecified(): Boolean = js.noImpl
    fun getOwnerElement(): Element = js.noImpl
    fun isId(): Boolean = js.noImpl
}

native public trait CDATASection: Text {
}

native public trait CharacterData: Node {
    fun getLength(): Int = js.noImpl
    fun getData(): String = js.noImpl
    fun replaceData(arg1: Int, arg2: Int, arg3: String): Unit = js.noImpl
    fun setData(arg1: String): Unit = js.noImpl
    fun substringData(arg1: Int, arg2: Int): String = js.noImpl
    fun appendData(arg1: String): Unit = js.noImpl
    fun insertData(arg1: Int, arg2: String): Unit = js.noImpl
    fun deleteData(arg1: Int, arg2: Int): Unit = js.noImpl
}

native public trait Comment: CharacterData {
}

native public trait Document: Node {
    fun getImplementation(): DOMImplementation = js.noImpl
    fun getInputEncoding(): String = js.noImpl
    fun getXmlEncoding(): String = js.noImpl
    fun createElement(arg1: String): Element = js.noImpl
    fun createComment(arg1: String): Comment = js.noImpl
    fun getElementsByTagName(arg1: String): NodeList = js.noImpl
    fun getElementsByTagNameNS(arg1: String, arg2: String): NodeList = js.noImpl
    fun getDoctype(): DocumentType = js.noImpl
    fun getDocumentElement(): Element = js.noImpl
    fun createDocumentFragment(): DocumentFragment = js.noImpl
    fun createTextNode(arg1: String): Text = js.noImpl
    fun createCDATASection(arg1: String): CDATASection = js.noImpl
    fun createProcessingInstruction(arg1: String, arg2: String): ProcessingInstruction = js.noImpl
    fun createAttribute(arg1: String): Attr = js.noImpl
    fun createEntityReference(arg1: String): EntityReference = js.noImpl
    fun importNode(arg1: Node, arg2: Boolean): Node = js.noImpl
    fun createElementNS(arg1: String, arg2: String): Element = js.noImpl
    fun createAttributeNS(arg1: String, arg2: String): Attr = js.noImpl
    fun getElementById(arg1: String): Element = js.noImpl
    fun getXmlStandalone(): Boolean = js.noImpl
    fun setXmlStandalone(arg1: Boolean): Unit = js.noImpl
    fun getXmlVersion(): String = js.noImpl
    fun setXmlVersion(arg1: String): Unit = js.noImpl
    fun getStrictErrorChecking(): Boolean = js.noImpl
    fun setStrictErrorChecking(arg1: Boolean): Unit = js.noImpl
    fun getDocumentURI(): String = js.noImpl
    fun setDocumentURI(arg1: String): Unit = js.noImpl
    fun adoptNode(arg1: Node): Node = js.noImpl
    fun getDomConfig(): DOMConfiguration = js.noImpl
    fun normalizeDocument(): Unit = js.noImpl
    fun renameNode(arg1: Node, arg2: String, arg3: String): Node = js.noImpl
}

native public trait DocumentFragment: Node {
}

native public trait DocumentType: Node {
    fun getName(): String = js.noImpl
    fun getEntities(): NamedNodeMap = js.noImpl
    fun getNotations(): NamedNodeMap = js.noImpl
    fun getPublicId(): String = js.noImpl
    fun getSystemId(): String = js.noImpl
    fun getInternalSubset(): String = js.noImpl
}

native public trait DOMConfiguration {
    fun setParameter(arg1: String, arg2: Any): Unit = js.noImpl
    fun getParameter(arg1: String): Any = js.noImpl
    fun canSetParameter(arg1: String, arg2: Any): Boolean = js.noImpl
    fun getParameterNames(): DOMStringList = js.noImpl
}

native public trait DOMError {
    fun getLocation(): DOMLocator = js.noImpl
    fun getMessage(): String = js.noImpl
    fun getType(): String = js.noImpl
    fun getSeverity(): Short = js.noImpl
    fun getRelatedException(): Any = js.noImpl
    fun getRelatedData(): Any = js.noImpl
    public val SEVERITY_WARNING: Short = 1
    public val SEVERITY_ERROR: Short = 2
    public val SEVERITY_FATAL_ERROR: Short = 3
}

native public trait DOMErrorHandler {
    fun handleError(arg1: DOMError): Boolean = js.noImpl
}

native public trait DOMImplementation {
    fun getFeature(arg1: String, arg2: String): Any = js.noImpl
    fun hasFeature(arg1: String, arg2: String): Boolean = js.noImpl
    fun createDocumentType(arg1: String, arg2: String, arg3: String): DocumentType = js.noImpl
    fun createDocument(arg1: String, arg2: String, arg3: DocumentType): Document = js.noImpl
}

native public trait DOMLocator {
    fun getLineNumber(): Int = js.noImpl
    fun getColumnNumber(): Int = js.noImpl
    fun getByteOffset(): Int = js.noImpl
    fun getUtf16Offset(): Int = js.noImpl
    fun getRelatedNode(): Node = js.noImpl
    fun getUri(): String = js.noImpl
}

native public trait DOMStringList {
    fun getLength(): Int = js.noImpl
    fun contains(arg1: String): Boolean = js.noImpl
    fun item(arg1: Int): String = js.noImpl
}

native public trait Element: Node {
    fun getAttribute(arg1: String): String = js.noImpl
    fun setAttribute(arg1: String, arg2: String): Unit = js.noImpl
    fun getTagName(): String = js.noImpl
    fun removeAttribute(arg1: String): Unit = js.noImpl
    fun getAttributeNode(arg1: String): Attr = js.noImpl
    fun setAttributeNode(arg1: Attr): Attr = js.noImpl
    fun removeAttributeNode(arg1: Attr): Attr = js.noImpl
    fun getElementsByTagName(arg1: String): NodeList = js.noImpl
    fun getAttributeNS(arg1: String, arg2: String): String = js.noImpl
    fun setAttributeNS(arg1: String, arg2: String, arg3: String): Unit = js.noImpl
    fun removeAttributeNS(arg1: String, arg2: String): Unit = js.noImpl
    fun getAttributeNodeNS(arg1: String, arg2: String): Attr = js.noImpl
    fun setAttributeNodeNS(arg1: Attr): Attr = js.noImpl
    fun getElementsByTagNameNS(arg1: String, arg2: String): NodeList = js.noImpl
    fun hasAttribute(arg1: String): Boolean = js.noImpl
    fun hasAttributeNS(arg1: String, arg2: String): Boolean = js.noImpl
    fun getSchemaTypeInfo(): TypeInfo = js.noImpl
    fun setIdAttribute(arg1: String, arg2: Boolean): Unit = js.noImpl
    fun setIdAttributeNS(arg1: String, arg2: String, arg3: Boolean): Unit = js.noImpl
    fun setIdAttributeNode(arg1: Attr, arg2: Boolean): Unit = js.noImpl
}

native public trait Entity: Node {
    fun getInputEncoding(): String = js.noImpl
    fun getXmlEncoding(): String = js.noImpl
    fun getXmlVersion(): String = js.noImpl
    fun getPublicId(): String = js.noImpl
    fun getSystemId(): String = js.noImpl
    fun getNotationName(): String = js.noImpl
}

native public trait EntityReference: Node {
}

native public trait NameList {
    fun getLength(): Int = js.noImpl
    fun getName(arg1: Int): String = js.noImpl
    fun contains(arg1: String): Boolean = js.noImpl
    fun getNamespaceURI(arg1: Int): String = js.noImpl
    fun containsNS(arg1: String, arg2: String): Boolean = js.noImpl
}

native public trait NamedNodeMap {
    fun getLength(): Int = js.noImpl
    fun item(arg1: Int): Node = js.noImpl
    fun getNamedItem(arg1: String): Node = js.noImpl
    fun setNamedItem(arg1: Node): Node = js.noImpl
    fun removeNamedItem(arg1: String): Node = js.noImpl
    fun getNamedItemNS(arg1: String, arg2: String): Node = js.noImpl
    fun setNamedItemNS(arg1: Node): Node = js.noImpl
    fun removeNamedItemNS(arg1: String, arg2: String): Node = js.noImpl
}

native public trait Node {
    fun normalize(): Unit = js.noImpl
    fun isSupported(arg1: String, arg2: String): Boolean = js.noImpl
    fun getAttributes(): NamedNodeMap = js.noImpl
    fun getUserData(arg1: String): Any = js.noImpl
    fun setUserData(arg1: String, arg2: Any, arg3: UserDataHandler): Any = js.noImpl
    fun getPrefix(): String = js.noImpl
    fun getFeature(arg1: String, arg2: String): Any = js.noImpl
    fun hasAttributes(): Boolean = js.noImpl
    fun setPrefix(arg1: String): Unit = js.noImpl
    fun removeChild(arg1: Node): Node = js.noImpl
    fun replaceChild(arg1: Node, arg2: Node): Node = js.noImpl
    fun getFirstChild(): Node = js.noImpl
    fun getLastChild(): Node = js.noImpl
    fun getNextSibling(): Node = js.noImpl
    fun insertBefore(arg1: Node, arg2: Node): Node = js.noImpl
    fun getLocalName(): String = js.noImpl
    fun getNodeName(): String = js.noImpl
    fun getNodeValue(): String = js.noImpl
    fun setNodeValue(arg1: String): Unit = js.noImpl
    fun getNodeType(): Short = js.noImpl
    fun getParentNode(): Node = js.noImpl
    fun getChildNodes(): NodeList = js.noImpl
    fun getPreviousSibling(): Node = js.noImpl
    fun getOwnerDocument(): Document = js.noImpl
    fun appendChild(arg1: Node): Node = js.noImpl
    fun hasChildNodes(): Boolean = js.noImpl
    fun cloneNode(arg1: Boolean): Node = js.noImpl
    fun getNamespaceURI(): String = js.noImpl
    fun getBaseURI(): String = js.noImpl
    fun compareDocumentPosition(arg1: Node): Short = js.noImpl
    fun getTextContent(): String = js.noImpl
    fun setTextContent(arg1: String): Unit = js.noImpl
    fun isSameNode(arg1: Node): Boolean = js.noImpl
    fun lookupPrefix(arg1: String): String = js.noImpl
    fun isDefaultNamespace(arg1: String): Boolean = js.noImpl
    fun lookupNamespaceURI(arg1: String): String = js.noImpl
    fun isEqualNode(arg1: Node): Boolean = js.noImpl
    public val ELEMENT_NODE: Short = 1
    public val ATTRIBUTE_NODE: Short = 2
    public val TEXT_NODE: Short = 3
    public val CDATA_SECTION_NODE: Short = 4
    public val ENTITY_REFERENCE_NODE: Short = 5
    public val ENTITY_NODE: Short = 6
    public val PROCESSING_INSTRUCTION_NODE: Short = 7
    public val COMMENT_NODE: Short = 8
    public val DOCUMENT_NODE: Short = 9
    public val DOCUMENT_TYPE_NODE: Short = 10
    public val DOCUMENT_FRAGMENT_NODE: Short = 11
    public val NOTATION_NODE: Short = 12
    public val DOCUMENT_POSITION_DISCONNECTED: Short = 1
    public val DOCUMENT_POSITION_PRECEDING: Short = 2
    public val DOCUMENT_POSITION_FOLLOWING: Short = 4
    public val DOCUMENT_POSITION_CONTAINS: Short = 8
    public val DOCUMENT_POSITION_CONTAINED_BY: Short = 16
    public val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short = 32
}

native public trait NodeList {
    fun getLength(): Int = js.noImpl
    fun item(arg1: Int): Node = js.noImpl
}

native public trait Notation: Node {
    fun getPublicId(): String = js.noImpl
    fun getSystemId(): String = js.noImpl
}

native public trait ProcessingInstruction: Node {
    fun getData(): String = js.noImpl
    fun getTarget(): String = js.noImpl
    fun setData(arg1: String): Unit = js.noImpl
}

native public trait Text: CharacterData {
    fun splitText(arg1: Int): Text = js.noImpl
    fun isElementContentWhitespace(): Boolean = js.noImpl
    fun getWholeText(): String = js.noImpl
    fun replaceWholeText(arg1: String): Text = js.noImpl
}

native public trait TypeInfo {
    fun getTypeName(): String = js.noImpl
    fun getTypeNamespace(): String = js.noImpl
    fun isDerivedFrom(arg1: String, arg2: String, arg3: Int): Boolean = js.noImpl
    public val DERIVATION_RESTRICTION: Int = 1
    public val DERIVATION_EXTENSION: Int = 2
    public val DERIVATION_UNION: Int = 4
    public val DERIVATION_LIST: Int = 8
}

native public trait UserDataHandler {
    fun handle(arg1: Short, arg2: String, arg3: Any, arg4: Node, arg5: Node): Unit = js.noImpl
    public val NODE_CLONED: Short = 1
    public val NODE_IMPORTED: Short = 2
    public val NODE_DELETED: Short = 3
    public val NODE_RENAMED: Short = 4
    public val NODE_ADOPTED: Short = 5
}

