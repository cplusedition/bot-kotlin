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

import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSuperclassOf

typealias Property = KProperty1<out Any, Any?>

open class ReflectUtil {

    companion object {
        val ReflectUt = ReflectUtil()
    }

    fun KClass<*>.declaredProperty(name: String): Property? {
        return Without.exception<Property?> {
            declaredMemberProperties.first { name == it.name }
        }
    }

    fun getDeclaredPropertyValue(o: Any, name: String): Any? {
        return Without.exception X@{
            val member = o::class.declaredMemberProperties.firstOrNull {
                name == it.name
            } ?: return@X null
            if (member.parameters.size > 1) return@X null
            return@X member.call(o)
        }
    }

    fun getBooleanProperty(o: Any, name: String): Boolean? {
        return getPropertyValue1(o, name, Boolean::class)
    }

    /**
     * Get value of a Int property of the given object o with the given name, using reflection.
     * @return The value of the property or null if not found.
     */
    fun getIntProperty(o: Any, name: String): Int? {
        return getPropertyValue1(o, name, Int::class)
    }

    /**
     * Get value of a Long property of the given object o with the given name, using reflection.
     * @return The value of the property or null if not found.
     */
    fun getLongProperty(o: Any, name: String): Long? {
        return getPropertyValue1(o, name, Long::class)
    }

    /**
     * Get value of a Float property of the given object o with the given name, using reflection.
     * @return The value of the property or null if not found.
     */
    fun getFloatProperty(o: Any, name: String): Float? {
        return getPropertyValue1(o, name, Float::class)
    }

    /**
     * Get value of a Double property of the given object o with the given name, using reflection.
     * @return The value of the property or null if not found.
     */
    fun getDoubleProperty(o: Any, name: String): Double? {
        return getPropertyValue1(o, name, Double::class)
    }

    /**
     * Get value of a string property of the given object o with the given name, using reflection.
     * @return The value of the property or null if not found.
     */
    fun getStringProperty(o: Any, name: String): String? {
        return getPropertyValue1(o, name, String::class)
    }

    fun <T> getPropertyValue1(o: Any, name: String, superclass: KClass<*>): T? {
        return getPropertyValue1<T>(o, superclass, o::class.declaredProperty(name))
    }

    fun <T> getPropertyValue1(o: Any, superclass: KClass<*>, property: Property?): T? {
        val m = property ?: return null
        return Without.exception X@{
            if (m.parameters.size > 1) return@X null
            val c = m.returnType.classifier ?: return@X null
            if (c !is KClass<*> || !superclass.isSuperclassOf(c)) return@X null
            return@X m.call(o) as T
        }
    }

    fun objectProperty(o: Any, name: String): Property? {
        return objectProperties(o).firstOrNull { name == it.name }
    }

    /**
     * Iterate through declared properties of an object instance, eg. a companion object.
     *
     * @param callback(property, returntype)
     * @throws Exception
     */
    @Throws(Exception::class)
    fun objectProperties(o: Any, callback: (Property, KClass<*>) -> Unit) {
        o::class.declaredMemberProperties.forEach X@{
            if (it.parameters.size > 1) return@X
            val r = it.returnType.classifier ?: return@X
            if (r !is KClass<*>) return@X
            callback(it, r)
        }
    }

    /**
     * Iterate through declared properties of an object instance, eg. a companion object.
     *
     * @param callback(property, returntype, value)
     * @throws Exception
     */
    @Throws(Exception::class)
    fun objectProperties(o: Any, callback: (Property, KClass<*>, Any?) -> Unit) {
        objectProperties(o) { property, type ->
            val value = property.call(o)
            callback(property, type, value)
        }
    }

    /**
     * @return A sequence of the declared properties of an object instance, eg. a companion object.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun objectProperties(o: Any): Sequence<Property> {
        return buildSequence {
            val c = o::class
            for (it in c.declaredMemberProperties) {
                if (it.parameters.size > 1) continue
                val r = it.returnType.classifier ?: continue
                if (r !is KClass<*>) continue
                yield(it)
            }
        }
    }
}
