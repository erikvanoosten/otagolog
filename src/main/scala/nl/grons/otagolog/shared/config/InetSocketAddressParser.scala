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

import java.net.InetSocketAddress

object InetSocketAddressParser {

  /**
   * Parses a String to an InetSocketAddress.
   *
   * The following formats are supported:
   * - `null`, `""` => all addresses/ default port
   * - `:port` => all addresses/ given port
   * - `[host]` => address `host`/ default port, host can be a DNS resolvable name, IPv4 address or IPv6 address
   * - `[host]:port` => address `host`/ given port, host can be a DNS resolvable name, IPv4 address or IPv6 address
   * - `host:port` => address `host`/ given port, host can be a DNS resolvable name or IPv4 address
   * - `host` => host/ default port, host can be a DNS resolvable name or IPv4 address
   *
   * @param address a socket address as String (host + port)
   * @parm defaultPort port to use when it is not specified in `address`, or 0 to take any free port
   * @return a socket address
   * @throws IllegalArgumentException when port is negative or > 65535
   */
  def apply(address: String, defaultPort: Int): InetSocketAddress = {
    address match {
      case null | "" => new InetSocketAddress(defaultPort)
      case r"^:(\d+)${port}$$" => new InetSocketAddress(port.toInt)
      case r"^\[([^]]+)${host}\]:(\d+)${port}$$" => new InetSocketAddress(host, port.toInt)
      case r"^\[([^]]+)${host}\]$$" => new InetSocketAddress(host, defaultPort)
      case r"^([^:]+)${host}:(\d+)${port}$$" => new InetSocketAddress(host, port.toInt)
      case r"^(\d+)${port}$$" => new InetSocketAddress(port.toInt)
      case host => new InetSocketAddress(host, defaultPort)
    }
  }

  // From http://hacking-scala.tumblr.com/post/50360896036/regular-expressions-interpolation-in-pattern-matching
  import util.matching.Regex
  private implicit class RegexContext(sc: StringContext) {
    def r = new Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

}
