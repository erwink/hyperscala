package org.hyperscala.ui.binder

import org.hyperscala.ui.Binder
import org.hyperscala.html._
import org.hyperscala.event.JavaScriptEvent
import org.hyperscala.web.site.Webpage
import org.hyperscala.javascript.JavaScriptString
import java.text.SimpleDateFormat
import java.util.Date
import org.hyperscala.jquery.ui.{jQueryUI191, jQueryUI}

/**
 * @author Matt Hicks <mhicks@outr.com>
 */
class InputDateAsLong(format: String = "MM/dd/yyyy") extends Binder[tag.Input, Long] {
  def bind(input: tag.Input) = {
    Webpage().require(jQueryUI, jQueryUI191)

    input.value.onChange {
      val time = try {
        new SimpleDateFormat(format).parse(input.value()).getTime
      } catch {
        case t: Throwable => 0L
      }
      valueProperty := time
    }
    valueProperty.onChange {
      val formatted = valueProperty() match {
        case 0L => ""
        case v => new SimpleDateFormat(format).format(new Date(v))
      }
      input.value := formatted
    }

    input.contents += new tag.Script {
      contents += new JavaScriptString(
        """
          |$(function() {
          | $('#%s').datepicker();
          |});
        """.stripMargin.format(input.id()))
    }
    input.event.change := JavaScriptEvent()
//    Webpage().body.contents += new tag.Script {
//      contents += new JavaScriptString(
//        """
//          |$(function() {
//          | $('#%s').datepicker();
//          |});
//        """.stripMargin.format(input.id()))
//    }
  }
}
