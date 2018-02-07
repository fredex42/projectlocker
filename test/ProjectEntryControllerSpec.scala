import org.junit.runner._
import org.specs2.runner._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class ProjectEntryControllerSpec extends GenericControllerSpec {
  override val componentName: String = "ProjectEntryController"
  override val uriRoot: String = "/project"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("name","opensWith","targetVersion")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"name":"Premiere 2014 test","opensWith":"AdobePremierePro.app","targetVersion":"14.0"}"""
  override val testCreateDocument: String =  """{"name":"My Wonderful Editor","opensWith":"MyWonderfulEditor.app","targetVersion":"3.6"}"""
  override val minimumNewRecordId = 4
  override val testDeleteId: Int = 3
  override val testConflictId: Int = 1
}
