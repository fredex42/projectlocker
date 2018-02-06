package drivers

import java.io._

import scala.io.Source

/**
  * Protocol for StorageDriver classes
  */
trait StorageDriver {
  val bufferSize = 10
  val storageRef:models.StorageEntry
  def fileForPath(path:String):File

//  def writeDataToPath(path:String, dataStream:Stream[Byte])
  def writeDataToPath(path:String, dataStream:BufferedInputStream)
  def writeDataToPath(path:String, data:Array[Byte])
}
