package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import scala.concurrent.ExecutionContext.Implicits.global

trait StorageSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val storageWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int]
    )(unlift(StorageEntry.unapply))

  implicit val storageReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int]
    )(StorageEntry.apply _)
}

case class StorageEntry(override val id: Option[Int], rootpath: Option[String], storageType: String,
                        user:Option[String], password:Option[String], host:Option[String], port:Option[Int])  extends GenericModel(id){

}

class StorageEntryRow(tag:Tag) extends Table[StorageEntry](tag, "StorageEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc) //Autoincrement generates invalid SQL for Postgres, not sure why
  def rootpath = column[Option[String]]("rootpath")
  def storageType = column[String]("storageType")
  def user = column[Option[String]]("user")
  def password = column[Option[String]]("password")
  def host = column[Option[String]]("host")
  def port = column[Option[Int]]("port")

  def * = (id.?,rootpath,storageType,user,password,host,port) <> (StorageEntry.tupled, StorageEntry.unapply)
}


