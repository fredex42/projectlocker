package drivers

import java.io._
import java.nio.file.Paths
import java.util.NoSuchElementException

import models.StorageEntry
import play.api.Logger

import scala.io.Source
import scala.util.Try

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage(override val storageRef:StorageEntry) extends StorageDriver{

  override def fileForPath(path: String) = {
    new File(path)
  }


  override def writeDataToPath(path: String, dataStream: FileInputStream): Unit = {
    val finalPath = storageRef.rootpath match {
      case Some(rootpath)=>Paths.get(rootpath,path)
      case None=>Paths.get(path)
    }

    val f = this.fileForPath(finalPath.toString)
    Logger.info(s"Writing data to ${f.getAbsolutePath}")
    val st = new FileOutputStream(f)

    st.getChannel.transferFrom(dataStream.getChannel, 0, Long.MaxValue)

    st.close()
  }

  def writeDataToPath(path:String, data:Array[Byte]) = {
    val f = this.fileForPath(path)
    Logger.info(s"Writing data to ${f.getAbsolutePath}")
    val st = new FileOutputStream(f)

    st.write(data)
    st.close()
  }

  override def deleteFileAtPath(path: String): Boolean = {
    val f = this.fileForPath(path)
    Logger.info(s"Deleting file at ${f.getAbsolutePath}")
    f.delete()
  }

  override def getWriteStream(path: String): Try[OutputStream] = Try {
    val f = this.fileForPath(path)
    new BufferedOutputStream(new FileOutputStream(f))
  }

  override def getReadStream(path: String): Try[InputStream] = Try {
    val f = this.fileForPath(path)
    new BufferedInputStream(new FileInputStream(f))
  }
}
