import scala.collection.mutable

val queue = new mutable.Queue[String]()
queue += "Sathish"
queue += "kumar"

queue.dequeue()
queue.dequeue()
queue.size

queue.dequeue()



