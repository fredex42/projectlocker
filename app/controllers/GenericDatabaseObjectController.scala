package controllers
import java.sql.Timestamp

import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.ByteString
import models.{FileEntry, FileEntryRow, StorageEntry, StorageEntryRow}
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import play.api.mvc._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import auth.Security

import play.api.libs.streams.Accumulator

trait GenericDatabaseObjectController[M] extends InjectedController with Security with PlayBodyParsers {
  def validate(request:Request[JsValue]):JsResult[M]

  def selectall:Future[Try[Seq[M]]]
  def selectid(requestedId: Int):Future[Try[Seq[M]]]

  def deleteid(requestedId: Int):Future[Try[Int]]

  def insert(entry: M,uid:String):Future[Any]
  def jstranslate(result:Seq[M]):Json.JsValueWrapper
  def jstranslate(result:M):Json.JsValueWrapper

  val logger: Logger = Logger(this.getClass)

  def list = IsAuthenticatedAsync {uid=>{request=>
    selectall.map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->this.jstranslate(result)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def checksumVerifyParser:BodyParser[JsValue] = BodyParser {req=>
    val sink:Sink[ByteString, Future[ByteString]] = Flow[ByteString].toMat(Sink.fold(ByteString(Array()))(_ :+ _))(Keep.right)
    Accumulator(sink).map(Right.apply)
  }
//  val checksumVerifyParser = parse.using { request=>
//    HMAC.validateContentChecksum(request)
//      .map(didValidate=>if(didValidate) parse.json(999999) else sys.error("Checksum did not match"))
//      .getOrElse {
//        sys.error("Could not validate checksum")
//      }
//  }

  def create = IsAuthenticatedAsync(BodyParsers.parse.json) {uid=>{request =>
    this.validate(request).fold(
      errors => {
        println(s"errors parsing content: $errors")
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      newEntry => {
        this.insert(newEntry,uid).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result.asInstanceOf[Int]))
          case Failure(error)=>
            logger.error(error.toString)
            InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }}

  def getitem(requestedId: Int) = IsAuthenticatedAsync {uid=>{request=>
    selectid(requestedId).map({
      case Success(result)=>
        if(result.isEmpty)
         NotFound("")
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result.head)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def update(id: Int) = IsAuthenticatedAsync(BodyParsers.parse.json) { uid=>{request =>
    this.validate(request).fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      StorageEntry=>Future(Ok(""))
    )
  }}

  def deleteAction(requestedId: Int) = {
    deleteid(requestedId).map({
      case Success(result)=>
        if(result==0)
          NotFound(Json.obj("status" -> "notfound", "id"->requestedId))
        else
          Ok(Json.obj("status" -> "ok", "detail" -> "deleted", "id" -> requestedId))
      case Failure(error)=>
        val errorString = error.toString
        logger.error(errorString)
        if(errorString.contains("violates foreign key constraint") || errorString.contains("Referential integrity constraint violation"))
          Conflict(Json.obj("status"->"error","detail"->"This is still referenced by sub-objects"))
        else
          InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def delete(requestedId: Int) = IsAuthenticatedAsync {uid=>{ request =>
    if(requestedId<0)
      Future(Conflict(Json.obj("status"->"error","detail"->"This is still referenced by sub-objects")))
    else
      deleteAction(requestedId)
  }}
}
