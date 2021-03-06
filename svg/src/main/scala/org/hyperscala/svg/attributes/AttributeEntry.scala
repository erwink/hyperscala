package org.hyperscala.svg.attributes

import org.powerscala.EnumEntry
import org.hyperscala.AttributeValue

/**
 * @author Matt Hicks <mhicks@outr.com>
 */
abstract class AttributeEntry[E <: EnumEntry[E] with AttributeValue](v: String = null, parent: AttributeObject[E]) extends EnumEntry[E]()(parent) with AttributeValue {
  lazy val value = v match {
    case null => generateValue()
    case _ => v
  }

  protected def generateValue() = {
    val b = new StringBuilder
    name().foreach {
      case c if (c.isUpper) => {
        if (b.length > 0) {
          b.append('-')
        }
        b.append(c.toLower)
      }
      case c => b.append(c)
    }
    b.toString()
  }
}