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

import java.io.File

typealias Fun00 = () -> Unit
typealias Fun10<T> = (T) -> Unit
typealias Fun20<T1, T2> = (T1, T2) -> Unit
typealias Fun30<T1, T2, T3> = (T1, T2, T3) -> Unit
typealias Fun01<T> = () -> T
typealias Fun11<T, R> = (T) -> R
typealias Fun21<T1, T2, R> = (T1, T2) -> R
typealias Fun31<T1, T2, T3, R> = (T1, T2, T3) -> R

typealias  IStringTransformer = (String) -> String?
typealias  IFilePathCallback = (file: File, rpath: String) -> Unit
typealias  IFilePathPredicate = (file: File, rpath: String) -> Boolean
typealias IFilePathCollector<T> = (file: File, rpath: String) -> T?

