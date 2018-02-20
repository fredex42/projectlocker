/*
 * Copyright (C) 2015 Jason Mar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package auth

import play.api.mvc._
import play.api.libs.Files.TemporaryFile
import controllers.routes
import auth._
import com.unboundid.ldap.sdk.LDAPConnectionPool
import play.api.cache.SyncCacheApi
import play.api.libs.json._

import scala.concurrent.Future

trait Security {
  implicit val cache:SyncCacheApi
  implicit val ldapConnectionPool:LDAPConnectionPool

  //if this returns something, then we are logged in
  private def username(request: RequestHeader) = request.session.get("uid")

  private def onUnauthorized(request: RequestHeader) = {
    Results.Forbidden(Json.obj("status"->"error","detail"->"Not logged in"))
  }

  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    uid => Action(request => f(uid)(request))
  }

  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[Result]) = Security.Authenticated(username, onUnauthorized) {
    uid => Action.async(request => f(uid)(request))
  }

  def IsAuthenticatedAsync[A](b: BodyParser[A])(f: => String => Request[A] => Future[Result]) = Security.Authenticated(username, onUnauthorized) {
    uid=> Action.async(b)(request => f(uid)(request))
  }

  def IsAuthenticated(b: BodyParser[MultipartFormData[TemporaryFile]] = BodyParsers.parse.multipartFormData)(f: => String => Request[MultipartFormData[TemporaryFile]] => Result) = {
    Security.Authenticated(username, onUnauthorized) { uid => Action(b)(request => f(uid)(request)) }
  }

  def HasRole(requiredRoles: List[String])(f: => String => Request[AnyContent] => Result) = IsAuthenticated {
    uid => 
      request => 
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).length > 0 => f(uid)(request)
          case _ => Results.Forbidden
        }
  }

  def HasRoleUpload(requiredRoles: List[String])(b: BodyParser[MultipartFormData[TemporaryFile]] = BodyParsers.parse.multipartFormData)(f: => String => Request[MultipartFormData[TemporaryFile]] => Result) = IsAuthenticated(b) {
    uid => 
      request => 
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).length > 0 => f(uid)(request)
          case _ => 
            Results.Forbidden
        }
  }

  def GetRole(uid: String) : Option[List[String]] = {
    uid match {
      case "sv-ela-t" => Some(List("admin","user"))
      case _ => None
    }
  }

}
