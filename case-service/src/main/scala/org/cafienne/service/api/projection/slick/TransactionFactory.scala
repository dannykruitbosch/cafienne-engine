package org.cafienne.service.api.projection.slick

trait TransactionFactory[T <: SlickTransaction[_]] {
  def createTransaction(actorId: String, tenant: String): T
}
