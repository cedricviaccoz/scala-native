package java.nio.channels

//implements part of FileChannelImpl actually

final class WriteOnlyFileChannel(val stream: Object, val handle: Long)
    extends FileChannel /*Impl*/ {

  private var append: Boolean = false;

  def this(stream: Object, handle: Long, isAppend: Boolean) = {
    this(stream, handle)
    append = isAppend
  }

  def implCloseChannel(): Unit = ???
  /*if (stream.isInstanceOf[Closeable]){
            (stream.asInstanceOf[Closeable]).close()
        }*/
}
