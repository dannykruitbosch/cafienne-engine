/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.service.api.projection.query.PlatformQueries

import scala.collection.immutable.Seq

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoutes(platformQueries: PlatformQueries)(override implicit val userCache: IdentityProvider) extends CommandRoute {

  val platformRoute = new PlatformRoute(platformQueries)

  override def routes: Route = pathPrefix("platform") {
    platformRoute.routes
  }

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[PlatformRoute])
  }
}
