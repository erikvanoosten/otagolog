/*
 * Copyright (c) 2014-2014 Erik van Oosten All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.grons.otagolog.shared.config

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, OneInstancePerTest}
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import java.net.InetSocketAddress

@RunWith(classOf[JUnitRunner])
class InetSocketAddressParserSpec extends FunSpec with MockitoSugar with OneInstancePerTest {
  import InetSocketAddressParserSpec._

  describe("InetSocketAddressParser") {

    it("parses null and empty string") {
      InetSocketAddressParser(null, 1) should havePort(1)
      InetSocketAddressParser("", 1) should havePort(1)
    }

    it("parses port only address") {
      InetSocketAddressParser(":2", 1) should havePort(2)
    }

    it("parses illegal port only address") {
      a [IllegalArgumentException] should be thrownBy { InetSocketAddressParser(":100000", 1) }
    }

    it("parses [host] only address") {
      InetSocketAddressParser("[www.google.com]", 1) should (haveHost("www.google.com") and havePort(1))
      InetSocketAddressParser("[82.94.234.20]", 1) should (haveHost("82.94.234.20") and havePort(1))
      InetSocketAddressParser("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]", 1) should (haveHost("2001:db8:85a3:0:0:8a2e:370:7334") and havePort(1))
    }

    it("parses [host]:port address") {
      InetSocketAddressParser("[www.google.com]:2", 1) should (haveHost("www.google.com") and havePort(2))
      InetSocketAddressParser("[82.94.234.20]:2", 1) should (haveHost("82.94.234.20") and havePort(2))
      InetSocketAddressParser("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:2", 1) should (haveHost("2001:db8:85a3:0:0:8a2e:370:7334") and havePort(2))
    }

    it("parses host only address") {
      InetSocketAddressParser("www.google.com", 1) should (haveHost("www.google.com") and havePort(1))
      InetSocketAddressParser("82.94.234.20", 1) should (haveHost("82.94.234.20") and havePort(1))
    }

    it("parses host:port address") {
      InetSocketAddressParser("www.google.com:2", 1) should (haveHost("www.google.com") and havePort(2))
      InetSocketAddressParser("82.94.234.20:2", 1) should (haveHost("82.94.234.20") and havePort(2))
    }

  }

}

private object InetSocketAddressParserSpec {
  def haveHost(expectedHost: String): Matcher[InetSocketAddress] = new Matcher[InetSocketAddress] {
    def apply(actual: InetSocketAddress) = MatchResult(
      actual.getHostString == expectedHost,
      "host " + actual.getHostString + " is not " + expectedHost,
      "host " + actual.getHostString + " is " + expectedHost
    )
  }

  def havePort(expectedPort: Int): Matcher[InetSocketAddress] = new Matcher[InetSocketAddress] {
    def apply(actual: InetSocketAddress) = MatchResult(
      actual.getPort == expectedPort,
      "port " + actual.getPort + " is not " + expectedPort,
      "port " + actual.getPort + " is " + expectedPort
    )
  }
}
