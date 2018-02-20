package controllers

import javax.inject.{Inject, Singleton}

import play.api._
import play.api.mvc._
import auth.{LDAPConnectionPoolWrapper, Security, User}
import com.unboundid.ldap.sdk.LDAPConnectionPool
import models.{LoginRequest, LoginRequestSerializer}
import play.api.cache.SyncCacheApi
import play.api.libs.json._

import scala.util.{Failure, Success}

@Singleton
class Application @Inject() (cc:ControllerComponents, p:PlayBodyParsers, cacheImpl:SyncCacheApi, ldapPool:LDAPConnectionPoolWrapper)
  extends AbstractController(cc) with Security with LoginRequestSerializer {

  implicit val cache:SyncCacheApi = cacheImpl
  implicit val ldapConnectionPool:LDAPConnectionPool = ldapPool.connectionPool.getOrElse(null)

  //this allows the loading of frontend, so shouldn't be authed
  def index(path:String) = Action {
    Ok(views.html.index())
  }

  def authenticate = Action(p.json) { request=>
    ldapPool.connectionPool.fold(
      errors=> {
        Logger.error("LDAP not configured properly", errors)
        InternalServerError(Json.obj("status" -> "error", "detail" -> "ldap not configured properly, see logs"))
      },
      ldapConnectionPool=> {
        implicit val pool: LDAPConnectionPool = ldapConnectionPool
        request.body.validate[LoginRequest].fold(
          errors => {
            BadRequest(Json.obj("status" -> "error", "detail" -> JsError.toJson(errors)))
          },
          loginRequest => {
            User.authenticate(loginRequest.username, loginRequest.password) match {
              case Success(Some(user)) =>
                Ok(Json.obj("status" -> "ok", "detail" -> "Logged in", "uid" -> user.uid)).withSession("uid" -> user.uid)
              case Success(None) =>
                Logger.warn(s"Failed login from ${loginRequest.username} with password ${loginRequest.password} from host ${request.host}")
                Forbidden(Json.obj("status" -> "error", "detail" -> "forbidden"))
              case Failure(error) =>
                Logger.error(s"Authentication error when trying to log in ${loginRequest.username}. This could just mean a wrong password.", error)
                Forbidden(Json.obj("status" -> "error", "detail" -> "forbidden"))
            }
          })
      }
    )
  }

  def isLoggedIn = IsAuthenticated { uid=> { request=>
    Ok(Json.obj("status"->"ok","uid"->uid))
  }}

  def logout = Action { request=>
    Ok(Json.obj("status"->"ok","detail"->"Logged out")).withNewSession
  }
}