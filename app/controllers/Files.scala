package controllers

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.driver.JdbcProfile
import play.api.libs.json._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import slick.lifted.TableQuery

import scala.concurrent.{CanAwait, Future}
import scala.util.{Failure, Success}


class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[FileEntry] with FileEntrySerializer {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[FileEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = {
    println("In select")
    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
    )
  }

  override def selectall = dbConfig.db.run(
    TableQuery[FileEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[FileEntry]) = result //implicit translation should handle this
  override def jstranslate(result: FileEntry) = result //implicit translation should handle this

  override def insert(entry: FileEntry) = dbConfig.db.run(
    (TableQuery[FileEntryRow] returning TableQuery[FileEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request: Request[JsValue]) = request.body.validate[FileEntry]

  def uploadContent(requestedId: Int) = Action.async(parse.anyContent) { request =>
    implicit val db = dbConfig.db

    request.body.asRaw match {
      case Some(buffer) =>
        dbConfig.db.run(
          TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
        ).flatMap({
          case Success(rows: Seq[FileEntry]) =>
            if (rows.isEmpty) {
              Logger.error(s"File with ID $requestedId not found")
              Future(NotFound(Json.obj("status" -> "error", "detail" -> s"File with ID $requestedId not found")))
            } else {
              val fileRef = rows.head
              //get the storage reference for the file
              fileRef.writeToFile(buffer).map({
                case Success(x) =>
                  Ok(Json.obj("status" -> "ok", "detail" -> "File has been written."))
                case Failure(error) =>
                  InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString))
              })
            }
          case Failure(error) =>
            Logger.error(s"Could not get file to write: ${error.toString}")
            Future(InternalServerError(Json.obj("status" -> "error", "detail" -> s"Could not get file to write: ${error.toString}")))
        })
      case None =>
        Future(BadRequest(Json.obj("status" -> "error", "detail" -> "No upload payload")))
    }
  }
}

