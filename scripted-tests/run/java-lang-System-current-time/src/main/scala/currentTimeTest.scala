import scala.scalanative.native._, stdio._
import java.lang.System

object Hello {
  def main(args: Array[String]): Unit = {
  	val d: Long = System.getCurrentMillis()
    fprintf(stderr, c"Hello, world!")
    assert(d != 0)
  }
}
