import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ServerCoroutineSocket(val port: Int = 5004) {
    val ssc: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()
    val mainServerCoroutine = CoroutineScope(Dispatchers.IO + Job())
    val socketAddress: SocketAddress = InetSocketAddress(port)
    val listOfChannels:MutableList<AsynchronousSocketChannel> = mutableListOf()
    val listOfCommunicators : MutableList<Communicator> = mutableListOf()
    val actionGetSocket:MutableList<(AsynchronousSocketChannel)->Unit> = mutableListOf()
    val actionGetCommunicator:MutableList<(Communicator)->Unit> = mutableListOf()
    val dataAnalyser = {s:String -> println(s) }
    val listOfDataAnalysers : MutableList<(String)->Unit> = mutableListOf(dataAnalyser)
    val mainClientCoroutine = CoroutineScope(Dispatchers.IO+Job())


    fun start() {
        mainServerCoroutine.launch {
            ssc.bind(socketAddress)
            while (ssc.isOpen) {
                var sc = suspendCoroutine{
                    ssc.accept(it, AsyncHandler())
                }
                listOfChannels.add(sc)
                listOfCommunicators.add(Communicator(sc,mainServerCoroutine).apply {
                    startReceiving(listOfDataAnalysers)
                    actionGetCommunicator.forEach { v->v(this) }
                })
                actionGetSocket.forEach{v->v(sc)}
            }
        }
    }



    class AsyncHandler<T>: CompletionHandler<T, Continuation<T>> {
        override fun completed(result: T, attachment: Continuation<T>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<T>) {
            attachment.resumeWithException(exc)
        }
    }


}