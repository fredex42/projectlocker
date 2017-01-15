package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Action, Controller}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success}
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models.{FileEntry, FileEntryRow}
import org.joda.time.DateTime
import play.api.mvc.BodyParsers

import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites

/**
  * Created by localhome on 14/01/2017.
  */

class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) extends Controller {
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)
  implicit val dateWrites = jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val fileWrites: Writes[FileEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "filepath").write[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").write[String] and
      (JsPath \ "isDir").write[Boolean] and
      (JsPath \ "ctime").write[Timestamp] and
      (JsPath \ "mtime").write[Timestamp] and
      (JsPath \ "atime").write[Timestamp]
    )(unlift(FileEntry.unapply))

  implicit val fileReads: Reads[FileEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "filepath").read[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").read[String] and
      (JsPath \ "isDir").read[Boolean] and
      (JsPath \ "ctime").read[Timestamp] and
      (JsPath \ "mtime").read[Timestamp] and
      (JsPath \ "atime").read[Timestamp]
    )(FileEntry.apply _)

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def list = Action.async {
    dbConfig.db.run(
      TableQuery[FileEntryRow].result.asTry //simple select *
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->result))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[FileEntry].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      FileEntry => {
          dbConfig.db.run(
            (TableQuery[FileEntryRow] += FileEntry).asTry
          ).map({
            case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "result" -> result))
            case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
          }
          )
      }
    )
  }

  def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[FileEntry].fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      FileEntry=>{
        val newFE = FileEntry.copy(Some(id))  //update file entry with the ID that we were given

      }
    )
    Future(Ok(""))
  }

  def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }

  def init = Action.async {
    dbConfig.db.run(
      DBIO.seq(
        TableQuery[FileEntryRow].schema.create
      ).asTry
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok"))
      case Failure(error)=>
        error.printStackTrace()
        InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
    })
  }
}
