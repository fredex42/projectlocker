package models

import java.util.UUID

import play.api.Logger
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
import play.api.libs.functional.syntax._
import slick.lifted.{TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class PlutoWorkingGroup (id:Option[Int], hide:Option[String], name:String, uuid:String) {
  private val logger = Logger(getClass)
  /**
    *  writes this model into the database, inserting if id is None and returning a fresh object with id set. If an id
    * was set, then updates the database record and returns the same object. */
  def save(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[PlutoWorkingGroup]] = id match {
    case None=>
      val insertQuery = TableQuery[PlutoWorkingGroupRow] returning TableQuery[PlutoWorkingGroupRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult.asInstanceOf[PlutoWorkingGroup])  //maybe only intellij needs the cast here?
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[PlutoWorkingGroupRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  /**
    * inserts this record into the database if there is nothing with the given uuid present
    * @param db - implicitly provided database object
    * @return a Future containing a Try containing a [[PlutoWorkingGroup]] object.
    *         If it was newly saved, or exists in the db, the id member will be set.
    */
  def ensureRecorded(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[PlutoWorkingGroup]] = {
    db.run(
      TableQuery[PlutoWorkingGroupRow].filter(_.uuid===uuid).result.asTry
    ).flatMap({
      case Success(rows)=>
        if(rows.isEmpty) {
          logger.info(s"Saving working group $name ($uuid) to the database")
          this.save
        } else {
          Future(Success(rows.head))
        }
      case Failure(error)=>
        throw error
        Future(Failure(error))
    })
  }

  /**
    * returns the contents as a string->string map, for passing to postrun actions
    * @return
    */
  def asStringMap:Map[String,String] = Map(
    "workingGroupName"->name,
    "workingGroupUuid"->uuid,
    "workingGroupHide"->hide.getOrElse("")
  )
}

class PlutoWorkingGroupRow(tag:Tag) extends Table[PlutoWorkingGroup](tag, "PlutoWorkingGroup") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def hide = column[Option[String]]("s_hide")
  def name = column[String]("s_name")
  def uuid = column[String]("u_uuid")

  def * = (id.?, hide, name, uuid) <> (PlutoWorkingGroup.tupled, PlutoWorkingGroup.unapply)
}

object PlutoWorkingGroup extends ((Option[Int],Option[String], String, String)=>PlutoWorkingGroup) {
  def entryForUuid(uuid:String)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoWorkingGroup]] = db.run(
    TableQuery[PlutoWorkingGroupRow].filter(_.uuid===uuid).result.asTry
  ).map({
    case Success(resultSeq)=>resultSeq.headOption
    case Failure(error)=>throw error
  })

  def entryForId(id:Int)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoWorkingGroup]] = db.run(
    TableQuery[PlutoWorkingGroupRow].filter(_.id===id).result
  ).map(resultSeq=>resultSeq.headOption)
}

trait PlutoWorkingGroupSerializer extends TimestampSerialization {
  implicit val workingGroupWrites:Writes[PlutoWorkingGroup] = (
    (JsPath \ "id").writeNullable[Int] and
    (JsPath \ "hide").writeNullable[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "uuid").write[String]
    )(unlift(PlutoWorkingGroup.unapply))

  implicit val workingGroupReads:Reads[PlutoWorkingGroup] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "hide").readNullable[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "uuid").read[String]
    )(PlutoWorkingGroup.apply _)
}

