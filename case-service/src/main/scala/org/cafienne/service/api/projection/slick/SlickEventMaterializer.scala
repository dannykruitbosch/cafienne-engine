package org.cafienne.service.api.projection.slick

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.{ModelEvent, TransactionEvent}
import org.cafienne.infrastructure.cqrs.TaggedEventConsumer
import org.cafienne.service.api.projection.LastModifiedRegistration

import scala.concurrent.Future

trait SlickEventMaterializer[M <: ModelEvent[_], T <: SlickTransaction[M]] extends TaggedEventConsumer with TransactionFactory[T] with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val lastModifiedRegistration: LastModifiedRegistration

  private val transactionCache = new TransactionMonitor[T](this.getClass.getSimpleName)
  private def getTransaction(actorId: String, tenant: String) = transactionCache.get(actorId, tenant, (actorId, tenant) => createTransaction(actorId, tenant))

  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done] = {
    modelEvent match {
      case evt: M => {
        val transaction = getTransaction(evt.getActorId, evt.tenant)
        transaction.handleEvent(evt, offsetStorage.name, newOffset).flatMap(_ => {
          evt match {
            case commitEvent: TransactionEvent[_] => {
              transactionCache.remove(evt.getActorId)
              for {
                commitTransaction <- transaction.commit(offsetStorage.name, newOffset, commitEvent)
                informPendingQueries <- {
                  lastModifiedRegistration.handle(commitEvent)
                  Future.successful(Done)
                }
              } yield (commitTransaction, informPendingQueries)._2
            }
            case _ => Future.successful(Done)
          }
        })
      }
      case other => {
        logger.error("Ignoring unexpected model event of type '" + other.getClass.getName() + ". Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
      }
    }
  }
}
