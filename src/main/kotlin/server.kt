import kotlinx.coroutines.runBlocking
import java.nio.channels.AsynchronousSocketChannel

fun main() {
    val scs = ServerCoroutineSocket()
    val list :MutableList<Communicator> = mutableListOf()
    scs.actionGetCommunicator.add { v ->
        println("Подключился новый канал")
        list.add(v)
    }
    scs.listOfDataAnalysers.add(
        {s ->
            scs.listOfCommunicators.forEach { c-> c.sendToChannel(s,8) }
        }
    )
    scs.start()
    while (true) {

    }
}