package org.cafienne.service.db.migration.versions

import org.cafienne.service.api.cases.table.CaseTables
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.db.migration.DbSchemaVersion
import slick.migration.api.TableMigration

object QueryDB_1_1_6 extends DbSchemaVersion
  with CaseTables
  with TaskTables
  with CaseTablesV1 {

  val version = "1.1.6"
  val migrations = dropPK & enhanceCaseTeamTable & addPK

  import dbConfig.profile.api._

  def dropPK = TableMigration(TableQuery[CaseInstanceTeamMemberTableV1]).dropPrimaryKeys(_.pk)

  // Add 2 new columns for memberType ("user" or "role") and case ownership
  //  Existing members all get memberType "user" and also all of them get ownership.
  //  Ownership is needed, because otherwise no one can change the case team anymore...
  // Also we rename columns role and user_id to caseRole and memberId (since member is not just user but can also hold a tenant role)
  def enhanceCaseTeamTable = TableMigration(TableQuery[CaseInstanceTeamMemberTable])
    .renameColumnFrom("user_id", _.memberId)
    .renameColumnFrom("role", _.caseRole)
    .addColumnAndSet(_.isTenantUser, true)
    .addColumnAndSet(_.isOwner, true)

  def addPK = TableMigration(TableQuery[CaseInstanceTeamMemberTable]).addPrimaryKeys(_.pk)

}
