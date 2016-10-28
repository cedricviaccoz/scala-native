package scala.scalanative
package apr

import scalanative.native._

@extern object apr_time{
	def apr_time_now(): Long = extern
}