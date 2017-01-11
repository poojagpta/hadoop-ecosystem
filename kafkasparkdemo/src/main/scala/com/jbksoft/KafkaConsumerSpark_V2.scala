package com.jbksoft

import java.util

import kafka.serializer.StringDecoder
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka._
import org.apache.spark.streaming.{Seconds, StreamingContext}



/**
  * Created by pooja on 12/19/16.
  */
object KafkaConsumerSpark_V2 {

  def main(args: Array[String]): Unit = {

    //Create a streamContext with 60 sec batch size
    val sparkConf = new SparkConf().setAppName("KafkaConsumerSpark")
     
    val ssc = new StreamingContext(sparkConf, Seconds(60))

    val kafkaParams = Map[String, String](
      "zookeeper.connect" -> "localhost:2181",
      "metadata.broker.list" -> "localhost:9092",
      "group.id" -> "kafka-consumer",
      "auto.offset.reset" -> "largest"
    )

    val topics1 = Array("demo1").toSet
    val stream1 = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topics1)

    val topics2 = Array("demo2").toSet

    val stream2 = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topics2)

    var CustomerStream = stream1.map(record => (record._2.split(",")(0), record._2.split(",")(1)))

    var MemCustomerStream = CustomerStream.reduceByKeyAndWindow((x: String, y: String) => y, (x: String, y: String) => y, Seconds(120), Seconds(60))

    MemCustomerStream.checkpoint(Seconds(240))


    var CustomerTxnStream = stream2.map(record => (record._2.split(",")(0), record._2.split(",")(1)))

    var CustomerJoinTxn = MemCustomerStream.join(CustomerTxnStream).filter(record => (record._2._1).toString.split(" ")(0).equals("A"));

    //Write to kafka back

    CustomerJoinTxn.foreachRDD(rdd => {
      System.out.println("# events = " + rdd.count())

      rdd.foreachPartition(partition => {
        // Print statements in this section are shown in the executor's stdout logs
        val kafkaOpTopic = "test-output"
        val props = new util.HashMap[String, Object]()
        props.put("bootstrap.servers", "localhost:9092")
        props.put("key.serializer",
          "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer",
          "org.apache.kafka.common.serialization.StringSerializer")

        val producer = new KafkaProducer[String, String](props)
        partition.foreach(record => {
          val data = record.toString
          val message = new ProducerRecord[String, String](kafkaOpTopic, null, data)
          producer.send(message)
        })

        producer.close()
      })
    })
  
    ssc.checkpoint("/home/pooja/dev/checkpoint")
    ssc.start()
    ssc.awaitTermination()

  }

}
