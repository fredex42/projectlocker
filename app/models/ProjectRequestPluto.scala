package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

//need to pick up default storage
//need to map the right project template id - how? maybe have a defaults option for each pluto project type?

/**
  * this model represents a request from Pluto to create a project
  * @param filename filename to create with
  * @param title title of the project
  * @param plutoProjectTypeName pluto project type. This must correspond to a defaults key, which identifies the template to use
  * @param user user that initiated the operation
  * @param workingGroupUuid uuid of the working group that this project will belong to
  * @param commissionVSID vidispine/pluto ID of the commission that this project will belong to
  */
case class ProjectRequestPluto(filename:String,title:String, plutoProjectTypeName:String,
                          user:String, workingGroupUuid: String, commissionVSID: String) {
  /* looks up the ids of destination storage and project template, and returns a new object with references to them or None */
  def hydrate(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[ProjectRequestFull]] = {
    val storageFuture = StorageEntryHelper.defaultProjectfileStorage
    val projectTemplateFuture = ProjectTemplate.defaultEntryFor(plutoProjectTypeName)
    val workingGroupFuture = PlutoWorkingGroup.entryForUuid(workingGroupUuid)
    val commissionFuture = PlutoCommission.entryForVsid(commissionVSID)

    Future.sequence(Seq(storageFuture, projectTemplateFuture, workingGroupFuture, commissionFuture)).map(resultSeq=>{
      val successfulResults = resultSeq.flatten
      if(successfulResults.length==4){
        Some(ProjectRequestFull(this.filename,
          successfulResults.head.asInstanceOf[StorageEntry],
          this.title,
          successfulResults(1).asInstanceOf[ProjectTemplate],
          user,
          successfulResults(2).asInstanceOf[PlutoWorkingGroup].id,
          successfulResults(3).asInstanceOf[PlutoCommission].id))
      } else None
    })
  }
}

trait ProjectRequestPlutoSerializer {
  implicit val projectRequestPlutoReads:Reads[ProjectRequestPluto] = (
    (JsPath \ "filename").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "plutoProjectTypeName").read[String] and
      (JsPath \ "user").read[String] and
      (JsPath \ "workingGroupUuid").read[String] and
      (JsPath \ "commissionVSID").read[String]
    )(ProjectRequestPluto.apply _)
}
