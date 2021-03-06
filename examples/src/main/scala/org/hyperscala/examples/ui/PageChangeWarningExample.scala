package org.hyperscala.examples.ui

import org.hyperscala.web.site.Webpage
import org.hyperscala.ui.PageChangeWarning

import org.hyperscala.html._
import org.hyperscala.event.{ClickEvent, JavaScriptEvent}

/**
 * @author Matt Hicks <mhicks@outr.com>
 */
class PageChangeWarningExample extends Webpage {
  require(PageChangeWarning)

  body.contents += new tag.Div {
    contents += new tag.A(href = "http://www.google.com", content = "Leave the Page")
    contents += new tag.Button(content = "Set Warning") {
      event.click := JavaScriptEvent()

      listeners.synchronous {
        case evt: ClickEvent => PageChangeWarning.warn("There are unsaved changes.")
      }
    }

    contents += new tag.Button(content = "Clear Warning") {
      event.click := JavaScriptEvent()

      listeners.synchronous {
        case evt: ClickEvent => PageChangeWarning.warn(null)
      }
    }
  }
}
