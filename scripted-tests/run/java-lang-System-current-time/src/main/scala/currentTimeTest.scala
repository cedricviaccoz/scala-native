import scala.scalanative.native._, stdio._
import java.lang.System

object currentTimeTest {
  def main(args: Array[String]): Unit = {
  	val time: Long = System.currentTimeMillis()
  	
  	//current system time on a 64-bit unix system representing 
  	// 31 october 2016, 16:18:04
  	val halloweenTime: Long = 1477927084326L

  	//maximum value possible for a 64-bit signed integer
  	val theLongestTime: Long = 9223372036854775807L

  	assert(time > 0)
    assert(time > halloweenTime)
    assert(time < theLongestTime)
  }
}
