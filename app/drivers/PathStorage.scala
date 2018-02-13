package drivers

import java.io._
import java.nio.file.Paths
import java.time.{Instant, ZoneId, ZonedDateTime}
import models.StorageEntry
import play.api.Logger
import scala.util.{Failure, Success, Try}

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage(override val storageRef:StorageEntry) extends StorageDriver{

  /**
    * return a [[java.io.File]] instance for the given path
    * @param path absolute path on the storage
    * @return [[java.io.File]] representing the given path
    */
  def fileForPath(path: String) = {
    new File(path)
  }


  override def writeDataToPath(path: String, dataStream: FileInputStream): Try[Unit] = Try {
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

  def writeDataToPath(path:String, data:Array[Byte]):Try[Unit] = Try {
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

  override def getReadStream(path: String): Try[InputStream] = {
    val f = this.fileForPath(path)
    if(f.exists())
      Success(new BufferedInputStream(new FileInputStream(f)))
    else
      Failure(new RuntimeException(s"Path $path does not exist"))
  }

  override def getMetadata(path: String): Map[Symbol, String] = {
    val f = this.fileForPath(path)
    Map(
      'size->f.length().toString,
      'lastModified->ZonedDateTime.ofInstant(Instant.ofEpochSecond(f.lastModified()),ZoneId.systemDefault()).toString
    )
  }
}
