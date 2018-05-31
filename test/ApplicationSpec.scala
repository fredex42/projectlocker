import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.cache.SyncCacheApi
import play.api.cache.ehcache.EhCacheModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import testHelpers.TestDatabase
import scala.reflect.ClassTag

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification with MockedCacheApi {
  sequential

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder().disable(classOf[EhCacheModule])
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .overrides(bind[SyncCacheApi].toInstance(mockedSyncCacheApi))
    .build

  private val injector:Injector = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .injector()

  def inject[T : ClassTag]: T = injector.instanceOf[T]

  "Application" should {
    "render the index page" in  {
      val home = route(application, FakeRequest(GET, "/")).get

        status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("<title>Projectlocker</title>")
    }
  }
}
