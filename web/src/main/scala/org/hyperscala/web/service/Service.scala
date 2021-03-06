package org.hyperscala.web.service

import java.util.concurrent.atomic.AtomicBoolean
import org.powerscala.reflect._

import org.powerscala.json._
import java.lang.reflect.Modifier
import org.hyperscala.web.site.Website
import com.outr.webcommunicator.netty._
import handler.RequestHandler
import org.jboss.netty.channel.{MessageEvent, ChannelHandlerContext}
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.util.CharsetUtil

/**
 * Service allows methods to be created in implementations that act as endpoints.
 *
 * For example, if the Service uri is "/user" and you provide a method "create(name: String, phone: String)" then when
 * the uri "/user/create" is invoked with JSON referencing "name" and "phone" the method will be invoked and the return
 * will be converted to JSON as well.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait Service extends WebResource with RequestHandler {
  private val initialized = new AtomicBoolean(false)
  private var endpoints = Map.empty[String, EnhancedMethod]

  /**
   * The base URI used to build the endpoint URIs
   */
  def uri: String

  /**
   * Init is called either the first
   */
  def init(): Unit = {
    endpoints = getClass.methods.collect {
      case m if (m.declaring.hasType(classOf[Service]) &&
                 m.declaring.javaClass != classOf[Service] &&
                 Modifier.isPublic(m.javaMethod.getModifiers) &&
                 !m.name.contains("$") &&
                 !Service.excludedMethods.contains(m.name)) => m
    }.map(m => m.name -> m).toMap
  }

  /**
   * Calls the endpoint defined by the supplied uri. Service.uri is removed from the beginning leaving the method name
   * to be invoked on this service. The request is received as a JSON String that is extracted to call the method with
   * arguments by name.
   *
   * @param uri the complete uri
   * @param request the JSON data as a String
   * @return the JSON response as a String
   */
  def callEndpoint(uri: String, request: String): String = {
    val endpoint = uri.substring(this.uri.length + 1)
    val method = endpoints(endpoint)
    val args = parse[Map[String, Any]](request)   // TODO: verify this will work!
    val result = method.apply[AnyRef](this, args)
    generate(result)
  }

  /**
   * Registers this service and all its endpoints with the provided Website.
   *
   * @param website the website to register with
   */
  final def register(website: Website[_]) = {
    if (initialized.compareAndSet(false, true)) {
      init()
    }
    endpoints.keys.foreach {
      case key => website.register(this)//website.register("%s/%s".format(uri, key), this)
    }
  }

  def request(webapp: NettyWebapp, context: ChannelHandlerContext, event: MessageEvent) = event.getMessage match {
    case request: HttpRequest if (hasEndpoint(request.path)) => Some(this)
    case _ => None
  }


  def apply(webapp: NettyWebapp, context: ChannelHandlerContext, event: MessageEvent) = {
    if (initialized.compareAndSet(false, true)) {
      init()
    }
    val request = event.getMessage.asInstanceOf[HttpRequest]
    val data = request.getContent.toString(CharsetUtil.UTF_8)
    val result = callEndpoint(request.path, data)
    streamString(result, context, request, "application/json", enableCaching = false)
  }

  def hasEndpoint(uri: String) = if (uri.startsWith(this.uri + "/")) {
    val endpoint = uri.substring(this.uri.length + 1)
    endpoints.contains(endpoint)
  } else {
    false
  }
}

object Service {
  private val excludedMethods = List("uri", "service", "apply", "disposed", "init", "callEndpoint", "register")
}