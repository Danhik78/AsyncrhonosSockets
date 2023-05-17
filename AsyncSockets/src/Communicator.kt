import jdk.incubator.vector.ByteVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Communicator(
    val sc:AsynchronousSocketChannel,
    val scope:CoroutineScope
) {

    class MyByteArrayBuilder(){
        val list :MutableList<ByteArray> = mutableListOf()
        fun add(arr:ByteArray) = list.add(arr)
        fun toByteArray():ByteArray{
            var len =0
            list.forEach { arr -> len +=arr.size }
            val array = ByteArray(len)
            var curr = 0
            list.forEach { ar->
                ar.forEachIndexed{ i,v->
                    array[curr] = ar[i]
                    curr++
                }
            }
            return array
        }
        fun clear() = list.clear()
    }

    fun startReceiving(listDataAnalyser: MutableList<(String)->Unit>){
        scope.launch {
            val buf =ByteBuffer.allocate(1024)
            val bb = MyByteArrayBuilder()
            var str = ""
            var flag = true
            var _b :ByteArray
            val b0 =ByteArray(1)
            while (sc.isOpen){
                while(flag) {
                    suspendCoroutine {
                        sc.read(buf, it, AsyncHandler())
                    }
                    buf.flip()
                    _b = ByteArray(buf.limit())
                    buf.get(_b)
                    if(_b.last()==b0[0]) {
                        flag = false
                        _b = ByteArray(_b.size - 1) { i -> _b[i] }
                    }
                    bb.add(_b)
                    buf.clear()
                }
                flag = true
                str = bb.toByteArray().toString(Charsets.UTF_8)
                listDataAnalyser.forEach { it(str) }
                bb.clear()
                str=""
            }
        }
    }

    fun sendToChannel(string: String,capacity: Int = 1024) =
        sendToChannel(string.toByteArray(Charsets.UTF_8),capacity)

    fun sendToChannel(array: ByteArray,capacity:Int = 1024){
        var _cap = capacity
        if(capacity<8) _cap = 1024
        scope.launch {
            for (i in 0 .. array.size / _cap) {
                sendCap(ByteArray(if(i!=array.size/_cap)_cap else array.size%_cap+1) { j -> if(i*_cap+j<array.size) array[i * _cap + j] else 0 },  _cap, _cap)
            }
        }
    }

    suspend private fun sendCap(arr:ByteArray,index:Int,capacity: Int){
        if (sc.isOpen) {
            val buf = ByteBuffer.allocate(capacity)
            buf.put(arr)
            buf.flip()
            suspendCoroutine {
                sc.write(buf, it, AsyncHandler())
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