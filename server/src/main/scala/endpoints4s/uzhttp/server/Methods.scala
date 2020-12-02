package endpoints4s.uzhttp.server

import endpoints4s.algebra
import uzhttp.Request

trait Methods extends algebra.Methods {
  type Method = Request.Method

  def Get: Method = Request.Method.GET

  def Post: Method = Request.Method.POST

  def Put: Method = Request.Method.PUT

  def Delete: Method = Request.Method.DELETE

  def Patch: Method = Request.Method.PATCH

  def Options: Method = Request.Method.PATCH
}
