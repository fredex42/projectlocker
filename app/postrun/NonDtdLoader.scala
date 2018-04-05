package postrun

import scala.xml.Elem
import scala.xml.factory.XMLLoader
import javax.xml.parsers.SAXParser

object NonDtdLoader extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.newSAXParser()
  }
}