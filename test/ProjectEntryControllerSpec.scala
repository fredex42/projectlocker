import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import helpers.{ProjectCreateHelper, ProjectCreateHelperImpl}
import models.{ProjectEntry, ProjectRequestFull}
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import testHelpers.TestDatabase
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class ProjectEntryControllerSpec extends Specification with Mockito {
  val mockedProjectHelper = mock[ProjectCreateHelperImpl]

  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .overrides(bind[ProjectCreateHelper].toInstance(mockedProjectHelper))
    .build

  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  implicit val system = ActorSystem("projectentry-controller-spec")
  implicit val materializer = ActorMaterializer()

  def bodyAsJsonFuture(response:Future[play.api.mvc.Result]) = response.flatMap(result=>
    result.body.consumeData.map(contentBytes=> {
      Json.parse(contentBytes.decodeString("UTF-8"))
    })
  )

  "ProjectEntryController.create" should {
    "validate request data and call out to ProjectCreateHelper" in {
      val testCreateDocument =
        """
          |{
          |  "filename": "sometestprojectfile",
          |  "destinationStorageId": 1,
          |  "projectTemplateId": 1,
          |  "user": "test-user"
          |}
        """.stripMargin

      val fakeProjectEntry = ProjectEntry(Some(999),1,Timestamp.valueOf(LocalDateTime.now()),"test-user")
      mockedProjectHelper.create(any[ProjectRequestFull],org.mockito.Matchers.eq(None))(org.mockito.Matchers.eq(db)) answers((arglist,mock)=>Future(Success(fakeProjectEntry)))
      val response = route(application, FakeRequest(
        method="PUT",
        uri="/api/project",
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=testCreateDocument).withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]

      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "projectId").as[Int] must equalTo(999)
      (jsondata \ "detail").as[String] must equalTo("created project")
    }
  }
}
