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

import java.io.{FileInputStream, File}
import java.util.Properties
import scala.Function.unlift
import scala.sys.SystemProperties
import java.net.InetSocketAddress
import nl.grons.otagolog.shared.util.InetSocketAddressParser
import nl.grons.otagolog.shared.OtagoLog

/**
 * A very simple String based configuration with properties that can be converted with a [[Converter]] type class.
 *
 * Implementation include: [[InlineConfiguration]], [[PropertiesConfiguration]], [[MapConfiguration]].
 * Configurations can be composed with [[.fallbackTo]] or by constructing a [[ChainedConfiguration]].
 */
trait Configuration {
  /**
   * Reads a property and converts it to the requested type.
   *
   * @return Some(converted property) in case the property is present, else None
   * @throws IllegalArgumentException in case the property can not be converted
   */
  def getProperty[A](name: String)(implicit converter: Converter[String, A]): Option[A] =
    getStringProperty(name).map(converter.convert)

  /**
   * Reads a property and converts it to the requested type.
   *
   * @return the converted property if it is present, `default` otherwise
   * @throws IllegalArgumentException in case the property can not be converted
   */
  def getProperty[A](name: String, default: A)(implicit converter: Converter[String, A]): A =
    getStringProperty(name).map(converter.convert).getOrElse(default)

  /**
   * Reads a required property and converts it to the requested type.
   *
   * @return the converted property
   * @throws IllegalArgumentException in case the property is not present, or can not be converted
   */
  def getRequiredProperty[A](name: String)(implicit converter: Converter[String, A]): A =
    getStringProperty(name).map(converter.convert).getOrElse(throw new IllegalArgumentException(s"option ${unwrap(name)} is required"))

  /**
   * @return a new configuration which queries another configuration in case a property is not present
   *         in this configuration
   */
  def fallbackTo(fallback: Configuration): Configuration = new ChainedConfiguration(this, fallback)

  /**
   * @return a new configuration in which all property names are prepended with the given `prefix`
   */
  def withPrefix(prefix: String): Configuration = {
    val self = this
    new Configuration {
      def getStringProperty(name: String) = self.getStringProperty(prefix + name)
      override protected[config] def unwrap(name: String) = prefix + super.unwrap(name)
    }
  }

  // NOTE: be careful not to return `Some(null)`
  protected[config] def getStringProperty(name: String): Option[String]

  protected[config] def unwrap(name: String) = name
}

object EmptyConfiguration extends Configuration {
  override protected[config] def getStringProperty(name: String) = None
}

class ChainedConfiguration(confs: Configuration*) extends Configuration {
  override def getStringProperty(name: String) = confs.collectFirst(unlift(_.getStringProperty(name)))
}

class MapConfiguration(map: Map[String, String]) extends Configuration {
  override def getStringProperty(name: String) = map.get(name)
}

object SystemConfiguration extends MapConfiguration(new SystemProperties())

class InlineConfiguration(valuePairs: (String, String)*) extends MapConfiguration(valuePairs.toMap)

class PropertiesConfiguration(props: Properties) extends Configuration {
  def getStringProperty(name: String): Option[String] = Option(props.getProperty(name))
}

class PropertiesFileConfiguration(propsFile: File)
  extends PropertiesConfiguration(PropertiesFileConfiguration.propsReader(propsFile))

private object PropertiesFileConfiguration {
  def propsReader(f: File): Properties = {
    val p = new Properties()
    p.load(new FileInputStream(f))
    p
  }
}

trait Converter[A,B] {
  def convert(a: A): B
}

object Converter {
  implicit object String2String extends Converter[String, String] { def convert(a: String) = a }
  implicit object String2Int extends Converter[String, Int] { def convert(a: String) = a.toInt }
  implicit object String2Double extends Converter[String, Double] { def convert(a: String) = a.toDouble }
  implicit object String2Long extends Converter[String, Long] { def convert(a: String) = a.toLong }
  implicit object String2Bool extends Converter[String, Boolean] {
    def convert(a: String) = a match {
      case "true" => true
      case "false" => false
      case _ => throw new IllegalArgumentException(s"not a boolean string ('$a'), must be 'true' or 'false'")
    }
  }
  implicit object String2InetSocketAddress extends Converter[String, InetSocketAddress] {
    def convert(a: String) = InetSocketAddressParser(a, OtagoLog.DefaultServerPort)
  }
  implicit object String2File extends Converter[String, File] { def convert(a: String) = new File(a) }
}
