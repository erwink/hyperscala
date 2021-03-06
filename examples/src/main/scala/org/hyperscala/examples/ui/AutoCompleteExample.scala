package org.hyperscala.examples.ui

import org.hyperscala.ui.widgets.AutoCompleteInput
import org.hyperscala.html._
import org.powerscala.Language
import org.powerscala.property.event.PropertyChangeEvent
import org.hyperscala.web.site.Webpage
import org.hyperscala.realtime.Realtime

/**
 * @author Matt Hicks <mhicks@powerscala.org>
 */
class AutoCompleteExample extends Webpage {
  require(Realtime)
  body.style.fontFamily = "Arial, sans-serif"

  body.contents += new tag.Div {
    style.paddingAll = 25.px

    val input = new AutoCompleteInput[Language]("language", Language.English) {
      def complete(value: String) = {
        val v = value.toLowerCase
        Language.values.collect {
          case l if (l.name().toLowerCase.contains(v)) => l
        }.slice(0, 10)
      }
    }
    input.property.listeners.synchronous {
      case evt: PropertyChangeEvent => println("OldValue: %s, NewValue: %s".format(evt.oldValue, evt.newValue))
    }
    contents += input

    contents += "Hello world! Goodbye world! Blah blah blah!"
  }
}
