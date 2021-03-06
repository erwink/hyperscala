package org.hyperscala.ui.widgets

import org.hyperscala.html._
import org.powerscala.property._
import org.hyperscala.html.constraints.BodyChild
import org.hyperscala.css.attributes._
import org.powerscala.property.event.PropertyChangingEvent
import org.powerscala.bus.Routing
import org.hyperscala.event.{ClickEvent, JavaScriptEvent}
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ListEditor[T] extends tag.Div {
  def manifest: Manifest[T]
  def createEditor(): BodyChild

  val defaultValue = manifest.erasure.defaultForType[T]

  val list = new StandardProperty[List[T]]("list", Nil)(this, Manifest.classType[List[T]](classOf[List[T]])) with ListProperty[T] {
    override def +=(t: T) = if (!value.contains(t)) {   // No duplicates allowed
      super.+=(t)
    }
  }
  val current = new StandardProperty[T]("current")(this, manifest)

  val listDiv = new tag.Div
  val editorDiv = new tag.Div {
    style.clear = Clear.Both
    style.float = Float.Left
  }
  val editorButtons = new tag.Div {
    style.float = Float.Left
  }
  val editor = createEditor()

  list.listeners.synchronous {
    case evt: PropertyChangingEvent if (evt.newValue == null) => Routing.Stop   // Nulls not allowed
  }
  list.onChange {
    updateList()
  }

  override protected def initialize() {
    super.initialize()

    setup()
  }

  def setup() = {
    contents += listDiv
    contents += editorDiv
    contents += editorButtons

    editorDiv.contents += editor

    editorButtons.contents += new tag.Button(content = "Add") {
      event.click := JavaScriptEvent()

      listeners.synchronous {
        case evt: ClickEvent => addCurrent()
      }
    }
  }

  /**
   * Adds the current item to the list. If the current item is editing an existing item it will replace it in the list.
   */
  def addCurrent() = if (current() != null) {
    list += current()
    current := defaultValue
  }

  // TODO: add editing support for editable types

  /**
   * Reloads the visual list of items from the "list" property. Automatically invoked when "list" changes.
   */
  def updateList() = {
    val l = list()

    // Remove items from the list
    listDiv.contents.foreach {
      case editor: ListEditorItem[_] => if (!l.contains(editor.value)) {
        editor.removeFromParent()
      }
    }
    // Add items not in the list
    l.foreach {
      case value => if (editorItemByValue(value).isEmpty) {
        listDiv.contents += createListItem(value)
      }
    }
    // Verify the correct ordering
    l.zipWithIndex.foreach {
      case (value, index) => {
        val editorItem = editorItemByValue(value).get
        if (listDiv.contents.indexOf(editorItem) != index) {
          println("Correcting the index of %s".format(value))
          editorItem.removeFromParent()
          listDiv.contents.insert(index, editorItem)
        }
      }
    }
  }

  private def editorItemByValue(value: T) = listDiv.contents.find(c => c.asInstanceOf[ListEditorItem[T]].value == value)

  def createListItem(value: T): ListEditorItem[T] = new DefaultListEditorItem[T](value, this)
}

trait ListEditorItem[T] extends BodyChild {
  def value: T
}

class DefaultListEditorItem[T](val value: T, editor: ListEditor[T]) extends tag.Div with ListEditorItem[T] {
  contents += value.toString
  contents += new tag.Button(content = "Delete") {
    event.click := JavaScriptEvent()

    listeners.synchronous {
      case evt: ClickEvent => delete()
    }
  }

  def delete() = editor.list -= value
}