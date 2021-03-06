package org.hyperscala.examples.basic

import org.hyperscala.web.site.Webpage
import org.hyperscala.html._
import org.hyperscala.event.{ClickEvent, JavaScriptEvent}
import org.hyperscala.realtime.Realtime

/**
 * @author Matt Hicks <mhicks@outr.com>
 */
class RealtimeExample extends Webpage {
  require(Realtime)

  body.contents += new tag.Button(content = "Click Me!") {
    event.click := JavaScriptEvent()

    listeners.synchronous {
      case evt: ClickEvent => info("I've been clicked!")
    }
  }
}
