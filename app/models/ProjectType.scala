package models
import exceptions.RecordNotFoundException
import slick.jdbc.PostgresProfile.api._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

trait ProjectTypeSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val typeWrites:Writes[ProjectType] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "opensWith").write[String] and
      (JsPath \ "targetVersion").write[String] and
      (JsPath \ "fileExtension").writeNullable[String] and
      (JsPath \ "plutoType").writeNullable[Int]
    )(unlift(ProjectType.unapply))

  implicit val typeReads:Reads[ProjectType] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "opensWith").read[String] and
      (JsPath \ "targetVersion").read[String] and
      (JsPath \ "fileExtension").readNullable[String] and
      (JsPath \ "plutoType").readNullable[Int]
    )(ProjectType.apply _)

  implicit val forPlutoWrites:Writes[ProjectTypeForPluto] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "plutoType").writeNullable[String] and
      (JsPath \ "plutoSubtype").writeNullable[String]
  )(unlift(ProjectTypeForPluto.unapply))

  implicit val forPlutoRead:Reads[ProjectTypeForPluto] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "plutoType").readNullable[String] and
      (JsPath \ "plutoSubtype").readNullable[String]
    )(ProjectTypeForPluto.apply _)
}

case class ProjectTypeForPluto(id: Option[Int], name: String, plutoType:Option[String], plutoSubtype:Option[String])

case class ProjectType(id: Option[Int],name:String, opensWith: String, targetVersion: String, fileExtension:Option[String]=None, plutoType:Option[Int]) {
  /**
    * Get a list of the postrun actions assocaited with this project type.
    * @param db implicitly provided database object
    * @return A Future, containing a Try indicating whether the database action was successful, containing a Sequence of PostrunAction instances
    */
  def postrunActions(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[PostrunAction]]] = {
    val query = for {
      (assoc, matchingPostrun) <- TableQuery[PostrunAssociationRow] join TableQuery[PostrunActionRow] on (_.postrunEntry === _.id) if assoc.projectType === this.id.get
    } yield matchingPostrun

    db.run(query.result.asTry)
  }

  /**
    * returns the contents as a string->string map, for passing to postrun actions
    * @return
    */
  def asStringMap:Map[String,String] = Map(
    "projectTypeId"->id.getOrElse("").toString,
    "projectTypeName"->name,
    "projectOpensWith"->opensWith,
    "projectTargetVersion"->targetVersion,
    "projectFileExtension"->fileExtension.getOrElse("")
  )

  def getPlutoType(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] = plutoType match {
    case Some(typeId)=>
      db.run(
        TableQuery[PlutoProjectTypeRow].filter(_.id===typeId).result.asTry
      ).map({
        case Success(resultSeq)=>resultSeq.headOption
        case Failure(error)=>throw error
      })
    case None=>
      Future(None)
  }


  /**
    * returns a [[ProjectTypeForPluto]] instance, which contains the UUIDs of the relevant groups
    * @param projectTemplate [[ProjectTemplate]] object to get the subtype from
    * @param db implicitly passed database object
    * @return
    */
  def forPluto(projectTemplate:ProjectTemplate)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[ProjectTypeForPluto] =
    Future.sequence(Seq(getPlutoType,projectTemplate.plutoSubtype)).map(results=>{
      val maybePlutoType = results.head
      val maybePlutoSubtype = results(1)

      ProjectTypeForPluto(id,name,maybePlutoType.map(_.uuid),maybePlutoSubtype.map(_.uuid))
    })
}

class ProjectTypeRow(tag: Tag) extends Table[ProjectType](tag, "ProjectType") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("s_name")
  def opensWith=column[String]("s_opens_with")
  def targetVersion=column[String]("s_target_version")
  def fileExtension=column[Option[String]]("s_file_extension")
  def plutoType = column[Option[Int]]("k_pluto_type")

  def * = (id.?, name, opensWith, targetVersion, fileExtension, plutoType) <> (ProjectType.tupled, ProjectType.unapply)
}

object ProjectType extends ((Option[Int],String,String,String,Option[String], Option[Int])=>ProjectType) {
  def entryFor(entryId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[ProjectType] = {
    db.run(
      TableQuery[ProjectTypeRow].filter(_.id===entryId).result
    ).map(projectsList=>
        if(projectsList.length==1)
          projectsList.head
        else
          throw new RecordNotFoundException(s"No project type found for $entryId")
    )
  }
}