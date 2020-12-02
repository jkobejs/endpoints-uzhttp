package endpoints4s.uzhttp.server

import endpoints4s.algebra

class EndpointsTestApi
    extends Endpoints
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi {}
