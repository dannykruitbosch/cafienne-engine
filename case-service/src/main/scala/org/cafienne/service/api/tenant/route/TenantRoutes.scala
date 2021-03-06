/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.server.Directives._
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.UserQueries

import scala.collection.immutable.Seq


@Path("/tenant")
class TenantRoutes(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {
  val tenantOwnersRoute = new TenantOwnersRoute(userQueries)(userCache)
  val tenantUsersRoute = new TenantUsersRoute(userQueries)(userCache)
  val formerAddTenantUserRoute = new DeprecatedTenantOwnersRoute(userQueries)(userCache)

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[TenantOwnersRoute], classOf[TenantUsersRoute])
  }

  override def routes = pathPrefix("tenant") {
    tenantOwnersRoute.routes ~
    tenantUsersRoute.routes ~
    formerAddTenantUserRoute.routes
  }

}
