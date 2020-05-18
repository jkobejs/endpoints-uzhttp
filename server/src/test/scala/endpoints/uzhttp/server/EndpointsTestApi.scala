package endpoints.uzhttp.server

import endpoints.algebra

class EndpointsTestApi
    extends Endpoints
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi
