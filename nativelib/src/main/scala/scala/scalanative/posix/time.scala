package scala.scalanative
package posix

import scala.scalanative.native.{struct, extern, Ptr, name}

@name("sys/time")
@extern
object time {

	type time_t = Long
	type suseconds_t = Long

	@struct
	class timeval(
		val tv_sec: time_t,
		val tv_usec: time_t
	)

	def gettimeofday(tv: Ptr[timeval], tz: Ptr[_]): Unit = extern
}