import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CoroutineSocket(
    host: String,
    port: Int
) {
    private val sc = AsynchronousSocketChannel.open()
    private val addr = InetSocketAddress(host, port)
    private val mainClientCoroutine = CoroutineScope(Dispatchers.IO + Job())


    var actionGetCommunicator:(Communicator)->Unit ={}

    fun start() {
        mainClientCoroutine.launch {
            if (sc.isOpen) {
                suspendCoroutine {
                    sc.connect(addr,it, AsyncHandler())
                }
                val com = Communicator(sc,mainClientCoroutine)
                actionGetCommunicator(com)
            }
        }
    }


    class AsyncHandler<T>: CompletionHandler<T, Continuation<T>> {
        override fun completed(result: T, attachment: Continuation<T>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<T>) {
            println(exc.message)
            attachment.resumeWithException(exc)
        }
    }

}