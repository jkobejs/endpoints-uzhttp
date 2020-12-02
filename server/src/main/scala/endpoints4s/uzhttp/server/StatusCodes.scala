package endpoints4s.uzhttp.server

import endpoints4s.algebra
import uzhttp.Status

trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Status

  def OK = Status.Ok

  def Created = Status.Created

  def Accepted = Status.Accepted

  def NoContent = Status(204, "No Content")

  def BadRequest = Status(400, "Bad Request")

  def Unauthorized = Status(401, "Unauthorized")

  def Forbidden = Status(403, "Forbidden")

  def NotFound = Status(404, "Not Found")

  final def PayloadTooLarge = Status(413, "Payload Too Large")

  def TooManyRequests = Status(429, "Too Many Requests")

  def InternalServerError = Status(500, "Internal Server Error")

  def NotImplemented = Status(501, "Not Implemented")
}
