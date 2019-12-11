/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.service.api.debug

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations.Api
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs.{GET, Path, Produces}
import org.cafienne.cmmn.instance.casefile.ValueList
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.AuthenticatedRoute

import scala.util.{Failure, Success}

@Api(value = "debug", tags = Array("cases"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/debug")
class DebugRoute
  ()
  (implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory, override implicit val userCache: IdentityProvider)
  extends AuthenticatedRoute {

  val caseEventReader = new ModelEventsReader()

  override def routes =
    pathPrefix("debug") {
      getEvents
    }

  @Path("/{caseInstanceId}")
  @GET
  @Operation(
    summary = "Get the list of events in a case",
    description = "Returns the list of events in a case instance",
    tags = Array("debug"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]))
    ),
    responses = Array(
      new ApiResponse ( description = "Case found and returned", responseCode = "200"),
      new ApiResponse ( description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getEvents = get {
    path(Segment) { caseInstanceId =>
      optionalUser { user =>
        implicit val valueListMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: ValueList =>
          HttpEntity(ContentTypes.`application/json`, value.toString)
        }

//        println("Entering rout with optional uer "+user)

        onComplete(caseEventReader.getEvents(user, caseInstanceId)) {
          case Success(value) => complete(StatusCodes.OK, value)
          case Failure(err) => complete(StatusCodes.NotFound, err)
        }
      }
    }

  }
}
