package org.cafienne.service.db.querydb

import org.cafienne.infrastructure.jdbc.schema.CafienneDatabaseDefinition
import org.cafienne.service.db.querydb.schema.versions.{QueryDB_1_0_0, QueryDB_1_1_10, QueryDB_1_1_11, QueryDB_1_1_5, QueryDB_1_1_6}

object QueryDB extends CafienneDatabaseDefinition with QueryDBSchema {
  def verifyConnectivity() {
    useSchema(Seq(QueryDB_1_0_0, QueryDB_1_1_5, QueryDB_1_1_6, QueryDB_1_1_10, QueryDB_1_1_11))
  }
}
