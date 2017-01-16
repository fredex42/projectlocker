package models

import slick.driver.PostgresDriver.api._

class FileAssociationRow(tag: Tag) extends Table[(Int,Int,Int)](tag, "ProjectFileAssociation") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectEntry = column[Int]("ProjectEntry")
  def fileEntry = column[Int]("FileEntry")

  def projectEntryFk=foreignKey("fk_ProjectEntry",projectEntry,TableQuery[ProjectEntryRow])(_.id)
  def fileEntryFk=foreignKey("fk_FileEntry",fileEntry,TableQuery[FileEntryRow])(_.id)

  def * = (id, projectEntry, fileEntry)
}