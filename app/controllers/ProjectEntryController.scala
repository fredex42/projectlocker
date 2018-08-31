package controllers

import javax.inject.{Inject, Named, Singleton}
import akka.actor.ActorRef
import akka.pattern.ask
import auth.Security
import com.unboundid.ldap.sdk.LDAPConnectionPool
import exceptions.{BadDataException, ProjectCreationError, RecordNotFoundException}
import helpers.ProjectCreateHelper
import models._
import models.messages.{NewAdobeUuid, NewAssetFolder, NewProjectCreated}
import play.api.cache.SyncCacheApi
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc._
import play.mvc.Http.Response
import services.actors.creation.{CreationMessage, GenericCreationActor}
import services.actors.creation.GenericCreationActor.{NewProjectRequest, ProjectCreateTransientData}
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

@Singleton
class ProjectEntryController @Inject() (@Named("project-creation-actor") projectCreationActor:ActorRef,
                                        @Named("message-processor-actor") messageProcessor:ActorRef,
                                        cc:ControllerComponents, config: Configuration,
                                        dbConfigProvider: DatabaseConfigProvider, projectHelper:ProjectCreateHelper,
                                        cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectControllerWithFilter[ProjectEntry,ProjectEntryFilterTerms]
    with ProjectEntrySerializer with ProjectEntryFilterTermsSerializer with ProjectRequestSerializer
    with ProjectRequestPlutoSerializer with UpdateTitleRequestSerializer with FileEntrySerializer
    with PlutoConflictReplySerializer with Security
{
  override implicit val cache:SyncCacheApi = cacheImpl

  val dbConfig = dbConfigProvider.get[PostgresProfile]
  implicit val implicitConfig = config

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int):Future[Try[Seq[ProjectEntry]]] = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).result.asTry
  )

  protected def selectVsid(vsid: String):Future[Try[Seq[ProjectEntry]]] = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.vidispineProjectId === vsid).result.asTry
  )

  override def dbupdate(itemId:Int, entry:ProjectEntry) = Future(Failure(new RuntimeException("Not implemented")))

  def getByVsid(vsid:String) = IsAuthenticatedAsync {uid=>{request=>
    selectVsid(vsid).map({
      case Success(result)=>
        if(result.isEmpty)
          NotFound("")
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}


  /**
    * Fully generic container method to process an update request
    * @param requestedId an ID to identify what should be updated, this is passed to `selector`
    * @param selector a function that takes `requestedId` and returns a Future, containing a Try, containing a sequence of ProjectEntries
    *                 that correspond to the provided ID
    * @param f a function to perform the actual update.  This is only called if selector returns a valid sequence of at least one ProjectEntry,
    *          and is called for each ProjectEntry in the sequence that `selector` returns.
    *          It should return a Future containing a Try containing the number of rows updated.
    * @tparam T the data type of `requestedId`
    * @return A Future containing a sequnce of results for each invokation of f. with either a Failure indicating why
    *         `f` was not called, or a Success with the result of `f`
    */
  def doUpdateGenericSelector[T](requestedId:T, selector:T=>Future[Try[Seq[ProjectEntry]]])(f: ProjectEntry=>Future[Try[Int]]):Future[Seq[Try[Int]]] = selector(requestedId).flatMap({
    case Success(someSeq)=>
        if(someSeq.isEmpty)
          Future(Seq(Failure(new RecordNotFoundException(s"No records found for id $requestedId"))))
        else
          Future.sequence(someSeq.map(f))
    case Failure(error)=>Future(Seq(Failure(error)))
  })

  /**
    * Most updates are done with the primary key, this is a convenience method to call [[doUpdateGenericSelector]]
    * with the appropriate selector and data type for the primary key
    * @param requestedId integer primary key value identifying what should be updated
    * @param f a function to perform the actual update. See [[doUpdateGenericSelector]] for details
    * @return see [[doUpdateGenericSelector]]
    */
  def doUpdateGeneric(requestedId:Int)(f: ProjectEntry=>Future[Try[Int]]) = doUpdateGenericSelector[Int](requestedId,selectid)(f)

  /**
    * Update the vidisipineId on a data record
    * @param requestedId primary key of the record to update
    * @param newVsid new vidispine ID. Note that this is an Option[String] as the id can be null
    * @return a Future containing a Try containing an Int describing the number of records updated
    */
  def doUpdateVsid(requestedId:Int, newVsid:Option[String]):Future[Seq[Try[Int]]] = doUpdateGeneric(requestedId){ record=>
    val updatedProjectEntry = record.copy (vidispineProjectId = newVsid)
    dbConfig.db.run (
      TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
    )
  }

  /**
    * generic code for an endpoint to update the title
    * @param requestedId identifier of the record to update
    * @param updater function to perform the actual update.  This is passed requestedId and a string to change the title to
    * @tparam T type of @reqestedId
    * @return a Future[Response]
    */
  def genericUpdateTitleEndpoint[T](requestedId:T)(updater:(T,String)=>Future[Seq[Try[Int]]]) = IsAuthenticatedAsync(parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=> {
        val results = updater(requestedId, updateTitleRequest.newTitle).map(_.partition(_.isSuccess))

        results.map(resultTuple => {
          val failures = resultTuple._2
          val successes = resultTuple._1

          if (failures.isEmpty)
            Ok(Json.obj("status" -> "ok", "detail" -> s"${successes.length} record(s) updated"))
          else
            genericHandleFailures(failures, requestedId)
        })
      }
    )
  }}

  /**
    * endpoint to update project title field of record based on primary key
    * @param requestedId
    * @return
    */
  def updateTitle(requestedId:Int) = genericUpdateTitleEndpoint[Int](requestedId) { (requestedId,newTitle)=>
    doUpdateGeneric(requestedId) {record=>
      val updatedProjectEntry = record.copy (projectTitle = newTitle)
      dbConfig.db.run (
        TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
      )
    }
  }

  /**
    * endoint to update project title field of record based on vidispine id
    * @param vsid
    * @return
    */
  def updateTitleByVsid(vsid:String) = genericUpdateTitleEndpoint[String](vsid) { (vsid,newTitle)=>
    doUpdateGenericSelector[String](vsid,selectVsid) { record=> //this lambda function is called once for each record
      val updatedProjectEntry = record.copy(projectTitle = newTitle)
      dbConfig.db.run(
        TableQuery[ProjectEntryRow].filter(_.id === record.id.get).update(updatedProjectEntry).asTry
      )
    }
  }


  def genericHandleFailures[T](failures:Seq[Try[Int]], requestedId:T) = {
    val notFoundFailures = failures.filter(_.failed.get.getClass==classOf[RecordNotFoundException])

    if(notFoundFailures.length==failures.length) {
      NotFound(Json.obj("status" -> "error", "detail" -> s"no records found for $requestedId"))
    } else {
      InternalServerError(Json.obj("status" -> "error", "detail" -> failures.map(_.failed.get.toString)))
    }
  }

  def updateVsid(requestedId:Int) = IsAuthenticatedAsync(parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=>{
        val results = doUpdateVsid(requestedId, updateTitleRequest.newVsid).map(_.partition(_.isSuccess))

        results.map(resultTuple => {
          val failures = resultTuple._2
          val successes = resultTuple._1

          if (failures.isEmpty)
            Ok(Json.obj("status" -> "ok", "detail" -> s"${successes.length} record(s) updated"))
          else {
            genericHandleFailures(failures, requestedId)
          }
        })
      }
    )
  }}

  def filesList(requestedId: Int) = IsAuthenticatedAsync {uid=>{request=>
    implicit val db = dbConfig.db

    selectid(requestedId).flatMap({
      case Failure(error)=>
        logger.error(s"could not list files from project ${requestedId}",error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
      case Success(someSeq)=>
        someSeq.headOption match { //matching on pk, so can only be one result
          case Some(projectEntry)=>
            projectEntry.associatedFiles.map(fileList=>Ok(Json.obj("status"->"ok","files"->fileList)))
          case None=>
            Future(NotFound(Json.obj("status"->"error","detail"->s"project $requestedId not found")))
        }
    })
  }}

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].sortBy(_.created.desc).drop(startAt).take(limit).result.asTry //simple select *
  )

  override def selectFiltered(startAt: Int, limit: Int, terms: ProjectEntryFilterTerms): Future[Try[Seq[ProjectEntry]]] = {
    dbConfig.db.run(
      terms.addFilterTerms {
        TableQuery[ProjectEntryRow]
      }.sortBy(_.created.desc).drop(startAt).take(limit).result.asTry
    )
  }

  override def jstranslate(result: Seq[ProjectEntry]):Json.JsValueWrapper = result
  override def jstranslate(result: ProjectEntry):Json.JsValueWrapper = result  //implicit translation should handle this

  /*this is pointless because of the override of [[create]] below, so it should not get called,
   but is needed to conform to the [[GenericDatabaseObjectController]] protocol*/
  override def insert(entry: ProjectEntry,uid:String) = Future(Failure(new RuntimeException("ProjectEntryController::insert should not have been called")))

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectEntry]

  override def validateFilterParams(request: Request[JsValue]): JsResult[ProjectEntryFilterTerms] = request.body.validate[ProjectEntryFilterTerms]

//  def createFromFullRequest(rq:ProjectRequestFull)(implicit db: slick.jdbc.PostgresProfile#Backend#Database) = projectHelper.create(rq,None).map({
//    case Failure(error)=>
//      logger.error("Could not create new project", error)
//      error match {
//        case projectCreationError:ProjectCreationError=>
//          BadRequest(Json.obj("status"->"error","detail"->projectCreationError.getMessage))
//        case _=>
//          InternalServerError(Json.obj("status"->"error","detail"->error.toString))
//      }
//    case Success(projectEntry)=>
//      logger.error(s"Created new project: $projectEntry")
//      Ok(Json.obj("status"->"ok","detail"->"created project", "projectId"->projectEntry.id.get))
//  })

  def createFromFullRequest(rq:ProjectRequestFull) = {
    implicit val timeout:akka.util.Timeout = 60.seconds

    val initialData = ProjectCreateTransientData(None, None, None)

    val msg = NewProjectRequest(rq,None,initialData)
    (projectCreationActor ? msg).mapTo[CreationMessage].map({
      case GenericCreationActor.ProjectCreateSucceeded(succeededRequest, projectEntry)=>
        logger.info(s"Created new project: $projectEntry")
        Ok(Json.obj("status"->"ok","detail"->"created project", "projectId"->projectEntry.id.get))
      case GenericCreationActor.ProjectCreateFailed(failedRequest, error)=>
        logger.error("Could not create new project", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  override def create = IsAuthenticatedAsync(parse.json) {uid=>{ request =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=> {
        val fullRequestFuture=projectRequest.copy(user=uid).hydrate
        fullRequestFuture.flatMap({
          case None=>
            Future(BadRequest(Json.obj("status"->"error","detail"->"Invalid template or storage ID")))
          case Some(rq)=>
            createFromFullRequest(rq)
        })
      })
  }}

  def handleMatchingProjects(rq:ProjectRequestFull, matchingProjects:Seq[ProjectEntry], force: Boolean):Future[Result] = {
    implicit val db = dbConfig.db
    logger.info(s"Got matching projects: $matchingProjects")
    if (matchingProjects.nonEmpty) {
      if (!force) {
        Future.sequence(matchingProjects.map(proj => PlutoConflictReply.getForProject(proj)))
          .map(plutoMatch => Conflict(Json.obj("status" -> "conflict","detail"->"projects already exist", "result" -> plutoMatch)))
      } else {
        logger.info("Conflicting projects potentially exist, but continuing anyway as force=true")
        createFromFullRequest(rq)
      }
    } else {
      logger.info("No matching projects, creating")
      createFromFullRequest(rq)
    }
  }

  def createExternal(force:Boolean) = IsAuthenticatedAsync(parse.json) {uid=>{ request:Request[JsValue] =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequestPluto].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=>
        projectRequest.hydrate.flatMap({
          case Left(errorList)=>
            Future(BadRequest(Json.obj("status"->"error","detail"->errorList)))
          case Right(rq)=>
            if(rq.existingVidispineId.isDefined){
              ProjectEntry.lookupByVidispineId(rq.existingVidispineId.get).flatMap({
                case Success(matchingProjects)=>
                  handleMatchingProjects(rq, matchingProjects, force)
                case Failure(error)=>
                  logger.error("Unable to look up vidispine ID: ", error)
                  Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
              })
            } else {
              createFromFullRequest(rq)
            }
        })
    )
  }}

  def getDistinctOwnersList:Future[Try[Seq[String]]] = {
    //work around distinctOn bug - https://github.com/slick/slick/issues/1712
    dbConfig.db.run(sql"""select distinct(s_user) from "ProjectEntry"""".as[String].asTry)
  }

  def distinctOwners = IsAuthenticatedAsync {uid=>{request=>
    getDistinctOwnersList.map({
      case Success(ownerList)=>
        Ok(Json.obj("status"->"ok","result"->ownerList))
      case Failure(error)=>
        logger.error("Could not look up distinct project owners: ", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def lookupProjectTypeForPluto(projectEntry:ProjectEntry) = {
    implicit val db=dbConfig.db
    val projectTemplateFuture = ProjectTemplate.entryFor(projectEntry.projectTemplateId.get)
    val projectTypeFuture = ProjectType.entryFor(projectEntry.projectTypeId)

    Future.sequence(Seq(projectTemplateFuture, projectTypeFuture)).flatMap({resultSeq=>
      val failures = resultSeq.collect({
        case Failure(error)=>error
      })

      if(failures.nonEmpty){
        for(f <- failures) yield logger.error("Could not look up pluto data", f)
        throw new RuntimeException("Could not look up pluto data, see previous errors for details")
      } else {
        val projectTemplate = resultSeq.head.asInstanceOf[Option[ProjectTemplate]].get
        val projectType = resultSeq(1).asInstanceOf[Try[ProjectType]].get
        projectType.forPluto(projectTemplate)
      }
    })
  }

  def pokePluto(projectId:Int) = IsAuthenticatedAsync {uid=>{request=>
    implicit val db=dbConfig.db
    ProjectEntry.entryForId(projectId).flatMap({
      case Success(projectEntry)=>
        if(projectEntry.projectTemplateId.isEmpty)
          Future(NotFound(Json.obj("status"->"error","detail"->s"Project $projectId does not have a template reference stored on it")))
        else{
          val ptForPlutoFuture = lookupProjectTypeForPluto(projectEntry)
          val commissionFuture = projectEntry.getCommission

          Future.sequence(Seq(ptForPlutoFuture, commissionFuture)).map(resultSeq=>{
            val projectType = resultSeq.head.asInstanceOf[ProjectTypeForPluto]
            val commission = resultSeq(1).asInstanceOf[Option[PlutoCommission]]

            if(commission.isDefined) {
              messageProcessor ! NewProjectCreated(projectEntry, projectType, commission.get, projectEntry.created.getTime)

              ProjectMetadata.entryFor(projectId, "created_asset_folder").map({
                case Some(metadata)=>
                  logger.info(s"Project ${projectEntry.projectTitle} ($projectId) has an asset folder at ${metadata.value}")
                  metadata.value match {
                    case Some(assetFolderPath)=>
                      messageProcessor ! NewAssetFolder(assetFolderPath, Some(projectId), None) //passing None as the vidispine ID will put the message processor into a retry loop, to pick the ID up when there is one.
                    case None=>
                  }

                case None=>
                  logger.info(s"Project ${projectEntry.projectTitle} ($projectId) has no asset folder")
              })

              ProjectMetadata.entryFor(projectId, "new_adobe_uuid").map({
                case Some(metadata)=>
                  logger.info(s"Project ${projectEntry.projectTitle} ($projectId) has an adobe UUID: ${metadata.value}")
                  metadata.value match {
                    case Some(adobeUuid)=>messageProcessor ! NewAdobeUuid(projectEntry, adobeUuid)
                    case None=>
                  }
                case None=>
                  logger.info(s"Project ${projectEntry.projectTitle} ($projectId) has no adobe UUID")
              })

              Ok(Json.obj("status"->"ok","detail"->"Queued message to update pluto"))
            } else {
              logger.error(s"Can't create project ${projectEntry.projectTitle} ($projectId) in Pluto as it does not have a commission associated with it")
              BadRequest(Json.obj("status"->"error","detail"->"Project has no commission"))
            }
          })
        }
    case Failure(error)=>
      logger.error(s"Could not look up project entry for $projectId", error)
      Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
    })
  }}
}
