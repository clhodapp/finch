/*
 * Copyright 2014, by Vladimir Kostyukov and Contributors.
 *
 * This file is a part of a Finch library that may be found at
 *
 *      https://github.com/finagle/finch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributor(s):
 * Ben Whitehead
 */

package io.finch.request

import com.twitter.finagle.http.Request
import com.twitter.util.{Await, Future}
import io.finch.HttpRequest
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.scalatest.{Matchers, FlatSpec}

class BodySpec extends FlatSpec with Matchers {
  val foo = "foo"
  val fooBytes = foo.getBytes("UTF-8")

  "A RequiredBody" should "be properly read if it exists" in {
    val request: HttpRequest = requestWithBody(fooBytes)
    val futureResult: Future[Array[Byte]] = RequiredBody(request)
    Await.result(futureResult) should equal(fooBytes)
  }

  it should "produce an error if the body is empty" in {
    val request: HttpRequest = requestWithBody(Array[Byte]())
    val futureResult: Future[Array[Byte]] = RequiredBody(request)
    intercept[BodyNotFound] {
      Await.result(futureResult)
    }
  }

  "An OptionalBody" should "be properly read if it exists" in {
    val request: HttpRequest = requestWithBody(fooBytes)
    val futureResult: Future[Option[Array[Byte]]] = OptionalBody(request)
    Await.result(futureResult).get should equal(fooBytes)
  }

  it should "produce an error if the body is empty" in {
    val request: HttpRequest = requestWithBody(Array[Byte]())
    val futureResult: Future[Option[Array[Byte]]] = OptionalBody(request)
    Await.result(futureResult) should equal(None)
  }

  "A RequiredStringBody" should "be properly read if it exists" in {
    val request: HttpRequest = requestWithBody(foo)
    val futureResult: Future[String] = RequiredStringBody(request)
    Await.result(futureResult) should equal(foo)
  }

  it should "produce an error if the body is empty" in {
    val request: HttpRequest = requestWithBody("")
    val futureResult: Future[String] = RequiredStringBody(request)
    intercept[BodyNotFound] {
      Await.result(futureResult)
    }
  }

  "An OptionalStringBody" should "be properly read if it exists" in {
    val request: HttpRequest = requestWithBody(foo)
    val futureResult: Future[Option[String]] = OptionalStringBody(request)
    Await.result(futureResult) should equal(Some(foo))
  }

  it should "produce an error if the body is empty" in {
    val request: HttpRequest = requestWithBody("")
    val futureResult: Future[Option[String]] = OptionalStringBody(request)
    Await.result(futureResult) should equal(None)
  }

  "RequiredBody Reader" should "work without parentheses at call site" in {
    val reader = for {
      body <- RequiredBody
    } yield body

    val request: HttpRequest = requestWithBody(fooBytes)
    Await.result(reader(request)) should equal(fooBytes)
  }

  private[this] def requestWithBody(body: String): HttpRequest = {
    requestWithBody(body.getBytes("UTF-8"))
  }
  private[this] def requestWithBody(body: Array[Byte]): HttpRequest = {
    val r = Request()
    r.setContent(ChannelBuffers.wrappedBuffer(body))
    r.headers().set(HttpHeaders.Names.CONTENT_LENGTH, body.length)
    r
  }
}
