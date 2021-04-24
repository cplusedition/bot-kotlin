/*
 *  Copyright (c) 2018, Cplusedition Limited.  All rights reserved.
 *
 *  This file is licensed to you under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.cplusedition.bot.core

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

object XMLUt : XMLUtil()

open class XMLUtil {

    fun getDocumentBuilder(): DocumentBuilder {
        return getDocumentBuilder(false, false)
    }

    fun getDocumentBuilder(validating: Boolean = false, expandentity: Boolean = false): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isValidating = validating
        factory.isExpandEntityReferences = expandentity
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", validating)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", validating)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", validating)
        return factory.newDocumentBuilder()
    }

    fun parse(file: File): Document {
        return getDocumentBuilder().parse(file)
    }

    /**
     * @return The trimmed text content of the first child element with the given tag, null if not found.
     */
    fun textContent1(elm: Element, tag: String): String? {
        for (c in elm.childNodes.nodes()) {
            if (c !is Element || tag != c.tagName) {
                continue
            }
            return c.textContent.trim()
        }
        return null
    }
}
