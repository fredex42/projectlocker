package services.actors.creation

import java.util.UUID
import javax.inject.Inject
import org.slf4j.MDC
import akka.actor.Props
import exceptions.PostrunActionError
import helpers.StorageHelper
import models.ProjectEntry

import scala.concurrent.Future
import scala.util.{Failure, Success}
import models.{FileEntry, ProjectEntry}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

object CopySourceFile {
  def props = Props[CopySourceFile]
  import GenericCreationActor._

  case class CopySourceFileRequest(prq: NewProjectRequest, savedFileEntry: FileEntry) extends CreationMessage
  case class CopySourceFileCompleted(copiedFileEntry:FileEntry)
}

/**
  * copies the template file associated with the template provided in [[services.actors.creation.GenericCreationActor.NewProjectRequest]] to
  * the file given by [[services.actors.creation.GenericCreationActor.ProjectCreateTransientData.destFileEntry]]
  */
class CopySourceFile  @Inject() (dbConfigProvider:DatabaseConfigProvider) extends GenericCreationActor {
  override val persistenceId = "creation-get-storage-actor"

  import CopySourceFile._
  import GenericCreationActor._
  private implicit val db=dbConfigProvider.get[JdbcProfile].db

  override def receiveCommand: Receive = {
    case copyRequest:NewProjectRequest=>
      doPersistedAsync(copyRequest) { (msg,originalSender)=>
        val rq = copyRequest.rq
        val savedFileEntry = copyRequest.data.destFileEntry.get
        MDC.put("copyRequest", copyRequest.toString)
        MDC.put("originalSender", originalSender.toString())

        logger.debug("persisted copy request event to journal, now performing")

        rq.destinationStorage.getStorageDriver match {
          case None=>
            logger.error(s"No storage driver was configured for ${rq.destinationStorage}")
            Future(Failure(new RuntimeException(s"No storage driver was configured for ${rq.destinationStorage}")))
          case Some(storageDriver)=>
            MDC.put("storageDriver", storageDriver.toString)
            logger.info(s"Got storage driver: $storageDriver")
            val storageHelper = new StorageHelper
            rq.projectTemplate.file.flatMap(sourceFileEntry=>{
              MDC.put("sourceFileEntry", sourceFileEntry.toString)
              MDC.put("savedFileEntry", savedFileEntry.toString)
              logger.info(s"Copying from file $sourceFileEntry to $savedFileEntry")
              storageHelper.copyFile(sourceFileEntry, savedFileEntry)
            }).map({
              case Left(error)=>
                val errorString = error.mkString("\n")
                logger.error(errorString)
                originalSender ! StepFailed(copyRequest.data, new RuntimeException(errorString))
                Failure(new RuntimeException(errorString))
              case Right(copiedFileEntry:FileEntry)=>
                logger.debug(copiedFileEntry.toString)
                val updatedData = copyRequest.data.copy(destFileEntry = Some(copiedFileEntry))
                originalSender ! StepSucceded(updatedData)
            })

//             fileCopyFuture.flatMap({
//              case Left(error)=>
//                logger.error(s"File copy failed: ${error.toString}")
//                Future(Failure(new RuntimeException(error.mkString("\n"))))
//              case Right(writtenFile)=>
//                logger.info(s"Creating new project entry from $writtenFile")
//                ProjectEntry.createFromFile(writtenFile, rq.projectTemplate, rq.title, createTime,
//                  rq.user,rq.workingGroupId, rq.commissionId, rq.existingVidispineId).flatMap({
//                  case Success(createdProjectEntry)=>
//                    logger.info(s"Project entry created as id ${createdProjectEntry.id}")
//                    if(rq.shouldNotify) sendCreateMessageToSelf(createdProjectEntry, rq.projectTemplate)
//                    doPostrunActions(writtenFile, createdProjectEntry, rq.projectTemplate) map {
//                      case Left(errorMessage)=>
//                        Failure(new PostrunActionError(errorMessage))
//                      case Right(successMessage)=>
//                        logger.info(successMessage)
//                        Success(createdProjectEntry)
//                    }
//                  case Failure(error)=>
//                    logger.error("Could not create project file: ", error)
//                    Future(Failure(error))
//                })
//            })
        }
      }
    case rollbackRequest:NewProjectRollback=>
      logger.debug("no rollback needed for this actor")
    case _=>
      super.receiveCommand
  }
}
