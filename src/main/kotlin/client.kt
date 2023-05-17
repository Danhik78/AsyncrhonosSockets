fun main(){

    val cs = CoroutineSocket("localhost",5004)
    cs.actionGetCommunicator ={c->
        c.startReceiving(mutableListOf({s-> println(s) }))
        while(true) {
            c.sendToChannel(readln(),13)
        }
    }
    cs.start()
    while (true){Thread.sleep(100000)}
}