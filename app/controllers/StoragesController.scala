package controllers

import javax.inject.{Inject, Singleton}

import models.{ProjectEntryRow, StorageEntry, StorageEntryRow, StorageSerializer, StorageType, StorageTypeSerializer}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Request}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StoragesController @Inject()
    (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider, cacheImpl:SyncCacheApi)
    extends GenericDatabaseObjectController[StorageEntry] with StorageSerializer with StorageTypeSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  val knownTypes = List(
    StorageType("Local",needsLogin=false,hasSubfolders=true),
    StorageType("ObjectMatrix",needsLogin = true,hasSubfolders = false),
    StorageType("S3",needsLogin = true,hasSubfolders = true)
  )

  implicit val dbConfig:DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectall = dbConfig.db.run(
    TableQuery[StorageEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[StorageEntry]) = result.asInstanceOf[Seq[StorageEntry]]  //implicit translation should handle this
  override def jstranslate(result: StorageEntry) = result  //implicit translation should handle this

  override def insert(storageEntry: StorageEntry) = dbConfig.db.run(
    (TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) += storageEntry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[StorageEntry]

  def types = Action {
    Ok(Json.obj("status"->"ok","types"->this.knownTypes))
  }
}
