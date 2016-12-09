import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import javax.jms.TopicSession
import javax.naming.InitialContext
import javax.jms.TopicConnectionFactory
import javax.jms.TopicConnection
import javax.jms.TopicPublisher
import java.util.Properties
import javax.jms.Topic
import javax.jms.Session
import java.util.Calendar
import java.text.SimpleDateFormat
import java.sql.Timestamp
import net.liftweb.json.DefaultFormats
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import net.liftweb.json._
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.types.DataType
import java.util.Calendar
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions.{array, lit, map, struct,sum}


@SerialVersionUID(100L)
class AnalyticsEngine extends Serializable {

/*	val props = new Properties();

	//val source = Source.fromURL(getClass.getResource("/jndi.properties"))

	props.setProperty("java.naming.factory.initial","org.apache.activemq.jndi.ActiveMQInitialContextFactory")
	props.setProperty("java.naming.provider.url","tcp://SambhavPC:61616")
	props.setProperty("connectionFactoryNames","connectionFactory, queueConnectionFactory, topicConnectionFactry")
	props.setProperty("queue.testQueue","testQueue")
	props.setProperty("topic.MyTopic","example.MyTopic")
	props.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "root""*"root"")
	//props.load(getClass.getResourceAsStream("/jndi.properties")) 

	val jndi = new InitialContext(props)

	val conFactory = jndi.lookup("topicConnectionFactry").asInstanceOf[TopicConnectionFactory]

			//username,password
			var connection: TopicConnection = conFactory.createTopicConnection("sambhav", "root")

			var pubSession: TopicSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE)

			val chatTopic = jndi.lookup("MyTopic").asInstanceOf[Topic]

					var publisher: TopicPublisher = pubSession.createPublisher(chatTopic)*/
}


object AnalyticsEngine extends Serializable{

	implicit val formats = new DefaultFormats {
		override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	} 


case class Event1(val eventType:String,val event:CheckOutEvent)
case class Event2(val eventType:String,val event:LocationEvent)

case class CheckOutEvent(
    val userId:String,
		val orgId:Int,
		val storeId:Int,
		val orderId:Int,
		val orderItems:List[orderItems],
		val invoiceAmount:Double,
		val invoiceQuantity:Int,
		val createdStamp:Timestamp  
		)                          
//case class orderItems(val productId:String,val quantity:String,val categoryId:String,val unitPrice:String)
case class orderItems(val orderItemId:Int,val productId:Int,val categoryId:Int,val quantity:Int,
                          val unitPrice:String,val discountApplied:Double,val createdStamp:Timestamp)
case class LocationEvent(
    val eventId:String,
		val userId:String,
		val orgId:Int,
		val storeId:Int,
		val rackId:Int,
		val locEventType:Int,
		val createdStamp:Timestamp)
		
		
case class Event(msg:String)


		//case class Event(data:String)

		def time_delta( t1:Timestamp, t2:Timestamp):Long = { 
	//from datetime import datetime
	val delta = t1.getTime() - t2.getTime()
			return delta
}

def timeAdd( t1:Timestamp):Timestamp = { 
	//from datetime import datetime
	val delta = t1.getTime + 24 * 60 * 60 * 1000
			return new Timestamp(delta)
}


def timeMonth(t1:Timestamp):Int = {

	val month = t1.getMonth()
	return month
}

def timeDay(t1:Timestamp):Int = {

	val day = t1.getDate()
	return day
}


def timeQuarter(t1:Timestamp):Int = {

	val month = t1.getMonth();
	var quarter:Int = 0
			if(month<3){
				quarter = 1
			} else if(month >3 & month<6) {
				quarter = 2
			} else if(month >6 & month<9){
				quarter = 3
			}else{
				quarter = 4
			}

	return quarter
}

def timeWeek(t1:Timestamp):Int ={

	val c = Calendar.getInstance();
	c.setTimeInMillis(t1.getTime())
	c.get(Calendar.WEEK_OF_MONTH);
}


def timeYear(t1:Timestamp):Int = {

	val year = t1.getYear();        
	return year
}

	def AllergyCheck(count:Int):String ={
       if(count>1){
         "allergy detected"
       } else{
         "no allergy detected uptil now"
       }
	
      }
		
	
	def TotalSalesByunitPrice(unitPrice:Double,quantity:Int):Double = {
	  
	  return (unitPrice*quantity)
	}
	
	
	//def TotalSalesByProductCount(
	
	def RemoveTimeDetails(t1:Timestamp) :Timestamp ={
	  
	  
	  t1.setHours(0);
	  t1.setMinutes(0)
	  t1.setSeconds(0)
	  t1
	}
	
	def NewEvaluate(count:Int):Int = {
	  if(count>1){
	   return 0
	  }
	  else if(count==1){
	    return 1
	  }
	  return 0
	}
	
	def RepeatEvaluate(count:Int):Int = {
	  if(count>1){
	   return 1
	  }
	  else if(count==1){
	    return 0
	  }
	  return 1
	}
	def DiffDay(t1:Timestamp,t2:Timestamp):String = {
	  
	  val c1 = Calendar.getInstance();
	    c1.setTimeInMillis(t1.getTime())
	  val d1 =  c1.get(Calendar.DAY_OF_MONTH);
	  val c2 = Calendar.getInstance();
	    c2.setTimeInMillis(t2.getTime())
	  val d2 =  c2.get(Calendar.DAY_OF_MONTH);
	   val result = d1- d2;
	   if(result!=0){
	     return "REPEAT"
	   }else{
	     return "NEW"
	   }
	}
		

def main(args:Array[String]) = {

  System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", """*""")
  
	//Spark initial Setup
	val sparkConf = new SparkConf().setAppName("DStreamAgg")
			.setMaster("local[2]")
			.set("spark.sql.shuffle.partitions","1")
			//.setMaster("spark://ip-172-31-21-112.ec2.internal:7077")
			val ssc = new StreamingContext(sparkConf, Seconds(3))

	val sqlContext = new SQLContext(ssc.sparkContext)
	val lines = ssc.socketTextStream("localhost", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	//val lines = ssc.socketTextStream("23.23.21.63", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	//val lines = ssc.socketTextStream("172.31.28.225", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	//val lines = ssc.socketTextStream("23.23.21.63", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	
	// Assuming ssc is the StreamingContext
//val lines = ssc.receiverStream(new CustomReceiver("23.23.21.63", 9999))
//val words = lines.flatMap(_.split(" "))
   
import sqlContext.implicits._
	lines.foreachRDD(x =>{
	  
	  val df = x.map( o =>
	          
	      Event(o) 
	      ).toDF()
	      
	     // df.show()
	  })
	
	//DB Connection Setup
	//val url = "jdbc:mysql://172.31.28.225:3306/ezcheckout1"
	val url = "jdbc:mysql://localhost:3306/DeadLine"
	val table = "people";
	import java.util.Properties
	val prop = new Properties() 
	prop.put("user", "root")
	prop.put("password", "")
	prop.put("driver", "com.mysql.jdbc.Driver")


	//All the static data loading 
	/*val dailyCategorySale = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySale")
	.option("user", "root")
	.option("password", "")
	.load()

	val dailyCategoryFootFall = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategoryFootFall")
	.option("user", "root")
	.option("password", "")
	.load()*/

	
	val racknCategory = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "racks")
	.option("user", "root")
	.option("password", "root")
	.load()
	
	val Category = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "category")
	.option("user", "root")
	.option("password", "root")
	.load()
	
	/*val rackIdCategory = racknCategory.join(Category,racknCategory("categoryId")===Category("rackId")).drop(Category.col("rackId"))
	.select("rackId","categoryId","categoryName")
	rackIdCategory.show()*/
	
	val rackIdCategory = racknCategory.join(Category,racknCategory("rackId")===Category("rackId")).drop(Category.col("rackId"))
	.select("rackId","categoryId","categoryName")
	rackIdCategory.show()
	

	
	
	//val handle = sqlContext.sparkContext.broadcast(activeMqHandle)
	
	lines.foreachRDD( (rdd: RDD[String], time: Time) => {
		import sqlContext.implicits._
		
		

		val LocationEventData =  rdd.filter(x => {

			val json = parse(x)
					val eventType = getEventType(json)
					eventType=="LocationEvent"

		})

		val CheckOutEventData =  rdd.filter(x => {

			val json = parse(x)
					val eventType = getEventType(json)
					eventType=="CheckOutEvent"

		})

		val LocationDF =   LocationEventData.map ( x =>  {

			val json = parse(x)
					val event = (json.extract[Event2])
					println(event.event.rackId)
					println(event.event.createdStamp)

					event.event

		}).toDF()
		
		
		val CheckOutDF =   CheckOutEventData.map ( x =>  {

			val json = parse(x)
					val event = (json.extract[Event1])
					event.event

		}).toDF()
		
		//LocationDF.show()
		//CheckOutDF.show()
		
		//UDF registration 

		val dayUDF = udf(timeDay _ )		
		val weekUDF = udf(timeWeek _)
		val monthUDF = udf(timeMonth _ )
		val quarterUDF = udf(timeQuarter _ )
		val yearUDF = udf(timeYear _ )
		val allergyUDF = udf( AllergyCheck _ )
		
		val totalSalesUDF = udf(TotalSalesByunitPrice _)
		
		val timeTrim = udf(RemoveTimeDetails _)
		
		val newUserUDF = udf(NewEvaluate _)
		val repeatUserUDF = udf(RepeatEvaluate _)
		
		val DateSub = udf(DiffDay _)

	import org.apache.spark.sql.functions._      
		 //exploding the CheckOutEvent
 
			//LocationDF.show()
	//DailyCategoryFootFall 
	/*
	val FootFallStartDF = LocationDF.join(rackIdCategory,LocationDF("rackId")===rackIdCategory("rackId"))
		                                .drop("rackId","rackId","categoryName")
		                                
  //FootFallStartDF.show()		                                
	val categoryFootFall  = FootFallStartDF.withColumnRenamed("createdStamp","time")
		                                       .withColumn("footfall", lit(1))
						//.select("userId","orgId","storeId","day","month","year","category","FootFall")
						//.select("orgId","storeId","time","categoryId","categoryName","footfall")
						.select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"footfall")

	val categoryFootFall_static = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	//.option("dbtable", "DailyCategoryFootFall")
	.option("dbtable", "dailyCategoryFootFallTemp")  //Name Changed for temporary table
	.option("user", "root")
	.option("password", "")
	.load()			
	
	//categoryFootFall_static.show()	
	
	val joinedCategoryFootFall = categoryFootFall.union(categoryFootFall_static)	
	
	val aggregatedCategoryFootFall = joinedCategoryFootFall.groupBy("orgId","storeId","time","categoryId")
	                              .agg(sum(joinedCategoryFootFall("footfall")).alias("footfallCount"))
	
	//adding new columns - day month and year to aggregated
	                              
  val categoryTimeBasedFootFall = aggregatedCategoryFootFall
                                          .withColumn("day", dayUDF(aggregatedCategoryFootFall("time"))) 	                              
                                          .withColumn("month", monthUDF(aggregatedCategoryFootFall("time")))
                                          .withColumn("year", yearUDF(aggregatedCategoryFootFall("time")))
	
  val finaldailyCategoryFootFall = categoryTimeBasedFootFall
                                      .select("orgId","storeId","day","month","year","time","categoryId","footfallCount")                                   
  //categoryTimeBasedFootFall.show()                        
  //aggregatedCategoryFootFall.show()
                                      
                                      
// finaldailyCategoryFootFall.show()                                    
	                          
 finaldailyCategoryFootFall.write.mode(SaveMode.Overwrite).jdbc(url,"dailyCategoryFootfall",prop) 
 categoryFootFall.write.mode(SaveMode.Append).jdbc(url,"dailyCategoryFootFallTemp",prop)  

 
 ////////////////////////////////////////////////////////////////////
 
 
/* val CheckOutStartDF = CheckOutDF.withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","createdStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice")*/
		
 	  val CheckOutStartDF = CheckOutDF.withColumnRenamed("createdStamp","outerStamp").withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","outerStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice","Products.discountApplied","Products.orderItemId"
						                              ,"Products.createdStamp").withColumnRenamed("createdStamp", "orderTimestamp").withColumnRenamed("outerStamp", "createdStamp")
 
   	//CheckOutStartDF.show()
		val CategorSaleStartDF = CheckOutStartDF.join(rackIdCategory,CheckOutStartDF("categoryId")===rackIdCategory("categoryId"))
		                                .drop(CheckOutStartDF("categoryId")).drop("rackId").drop("categoryName")
		                                
		                                
		//CategorSaleStartDF.show()  
		val categorySale  = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                      //.withColumn("sale", lit(1))
		                                      .withColumn("sale",CategorSaleStartDF("quantity"))
						//.select("userId","orgId","storeId","day","month","year","category","Sale")
						.select("orgId","storeId","time","categoryId","sale")
						.select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"sale")
						
		//categorySale.show()
			
	  val categorySale_static = sqlContext.read.format("jdbc").option("url",url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "dailyCategorySale")
	.option("user", "root")
	.option("password", "")
	.load()			
	
	val joinedCategorySale = categorySale.union(categorySale_static)
	
	val aggregatedCategorySale = joinedCategorySale.groupBy("orgId","storeId","time","categoryId")
	                              .agg(sum(joinedCategorySale("sale")).alias("saleCountAgg")) 		
		  
	//adding the month and year column into the aggregated column
	val modifiedAggregatedCategorySale = aggregatedCategorySale
	                                                         .withColumn("day",dayUDF(aggregatedCategorySale("time")))
	                                                         .withColumn("month", monthUDF(aggregatedCategorySale("time")))
	                                                         .withColumn("year", yearUDF(aggregatedCategorySale("time")))       
	
	 val finalAggregatedCategorySale = modifiedAggregatedCategorySale
	                                     .select("orgId", "storeId","day","month","year","time","categoryId","saleCountAgg")
	                                     
	//finalAggregatedCategorySale.show()                                     
	  //finalAggregatedCategorySale.show()
	//modifiedAggregatedCategorySale.show()
	                              
 // aggregatedCategorySale.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySaleCount",prop)
 
  categorySale.write.mode(SaveMode.Append).jdbc(url,"dailyCategorySale",prop)
 
 
 
  
  
  
 
 
 
 ////////////////////////////////////////////////////////////////////
 
  
  
  val categorySaleAmount =	CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                    .withColumn("saleAmount", 
		                                    totalSalesUDF(CategorSaleStartDF("unitPrice"),CategorSaleStartDF("quantity")))
		                                   .select("orgId","storeId","time","categoryId","saleAmount") 
		                                   .select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"saleAmount")
		
		//categorySaleAmount.show()                                   
		                                   
		val categorySaleAmount_static = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "dailyCategorySalePrice")
	.option("user", "root")
	.option("password", "")
	.load()			
	
	
	 val joinedCategorySaleAmount = categorySaleAmount.union(categorySaleAmount_static)
	
	 val aggregatedCategorySaleAmount = joinedCategorySaleAmount.groupBy("orgId","storeId","time","categoryId")
	                              .agg(sum(joinedCategorySaleAmount("saleAmount")).alias("saleAmountAgg")) 
		                                   
	
  //adding the month and year column into the aggregated column	                              
	
	val modifiedAggregatedCategorySaleAmount =  aggregatedCategorySaleAmount
	                                                            .withColumn("day",dayUDF(aggregatedCategorySaleAmount("time")))
	                                                            .withColumn("month", monthUDF(aggregatedCategorySaleAmount("time")))
	                                                            .withColumn("year", yearUDF(aggregatedCategorySaleAmount("time")))    
	    
	                                                            
	 val finalAggregatedCategorySaleAmount = modifiedAggregatedCategorySaleAmount
	                                                    .select("orgId","storeId","day","month","year","time","categoryId","saleAmountAgg")
    
//	finalAggregatedCategorySaleAmount.show()                                                    
	//modifiedAggregatedCategorySaleAmount.show()
  // aggregatedCategorySaleAmount.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySalePriceAgg",prop)
 
  categorySaleAmount.write.mode(SaveMode.Append).jdbc(url,"dailyCategorySalePrice",prop)
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  ///////////////////////////////////////////////////////////////////////
 
  //Joining data sets
  
  
  val dailyCategorySaleWithCountAgg = finalAggregatedCategorySale.join(finalAggregatedCategorySaleAmount,
                                Seq("orgId","storeId","day","month","year","time","categoryId"))
                                /*.drop(finalAggregatedCategorySaleAmount("orgId"))
                                .drop(finalAggregatedCategorySaleAmount("storeId"))
                                .drop(finalAggregatedCategorySaleAmount("day"))
                                .drop(finalAggregatedCategorySaleAmount("month"))
                                .drop(finalAggregatedCategorySaleAmount("year"))
                                .drop(finalAggregatedCategorySaleAmount("time"))
                                .drop(finalAggregatedCategorySaleAmount("categoryId"))*/
     */                           
  //  dailyCategorySaleWithCountAgg.show()
  //////////////////////////////////////////////////////////////////////////
	/*
	 val events = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "events")
	.option("user", "root")
	.option("password", "")
	.load()		  
	
	val userFirstVisit_interim = events.select("orgId","storeId","userId","createdStamp").withColumnRenamed("createdStamp","time")
    
	val userFirstVisit= userFirstVisit_interim.groupBy("orgId","storeId","userId").agg(min(userFirstVisit_interim("time")).alias("firstTime"))
	
	
	userFirstVisit.show()
	
	val  userCurrentvisit =  LocationDF
                         .withColumn("count", lit(1))
		                     .drop("rackId")
		                     .withColumnRenamed("createdStamp","time")
		                     .select($"orgId",$"storeId",$"userId",$"time")
		// (orgId int,storeId int,userId varchar(255),time Timestamp,count int);
     
		                     
		                     
		//val updatedTime = userFirstVisit.withColumnRenamed("firstTime", "time")
		
		
		
  val joinedDataSet = userCurrentvisit.join(userFirstVisit,Seq("orgId","storeId","userId"))
  joinedDataSet.show()
	val userVisitType = joinedDataSet.withColumn("visitType", DateSub(joinedDataSet("time"),joinedDataSet("firstTime")))
	                                .drop("firstTime")
	
	userVisitType.show()      
	
	val staticVisitType  = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userVisitType")
	.option("user", "root")
	.option("password", "")
	.load()		
	
	val newRepeatJoin = userVisitType.union(staticVisitType)
	
	newRepeatJoin.show()
	
	//newRepeatJoin.groupBy("orgId","storeId","time","visitType",")
	
	
	//val aggregates = newRepeatJoin.groupBy("orgId","storeId","time").count().alias("repeated_visitors")
	  //                                  .where(col("visitType")==="REPEAT")
	import org.apache.spark.sql.functions;                               
	
 val aggregates_r = newRepeatJoin.filter(newRepeatJoin("visitType")==="REPEAT")
                      .groupBy("orgId","storeId","time").agg(countDistinct(newRepeatJoin("userId")).alias("repeated_visitors"))
 //val aggregates_r = aggregates_r_t.withColumnRenamed("countDistinct","repeated_visitors")
  val aggregates_n = newRepeatJoin.filter(newRepeatJoin("visitType")==="NEW")
                      //.groupBy("orgId","storeId","time").count().alias("new_visitors")
                      .groupBy("orgId","storeId","time").agg(countDistinct(newRepeatJoin("userId")).alias("new_visitors"))
 // val aggregates_n = aggregates_n_t.withColumnRenamed("countdistinct","new_visitors")                   
                      
    aggregates_r.show()
    aggregates_n.show()
		
    
     val finalDF = aggregates_r.join(aggregates_n,Seq("orgId","storeId","time"),"outer")
   // val finalAggregate = finalDF.na.fill(0, Seq("new_visitors","repeated_visitors"))
  //  finalAggregate.show()
    
    import org.apache.spark.sql.functions.{lit, when}
  import org.apache.spark.sql.types.IntegerType
    val extractDateAsInt = udf(
   (d: String) => d.substring(0, 10).filterNot("-".toSet).toInt
      )

 val finalAggregate =    finalDF.withColumn("y",
  when($"repeated_visitors".isNull, lit(null))
    .otherwise(extractDateAsInt($"repeated_visitors"))
    .cast(IntegerType)
)

 finalAggregate.show()
 
 */
 ///////////////////////////////////////////////////////////////////////////////////////////////////
 
 
  //////////////////////////////////////////////////////////////////////// 
    
    //CategorSaleStartDF.write.mode(SaveMode.Append).jdbc(url,"dailyUserCheckOut
    //To be removed
 
		//checkOut Code
 	  val CheckOutStartDF = CheckOutDF.withColumnRenamed("createdStamp","outerStamp").withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","outerStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice","Products.discountApplied","Products.orderItemId"
						                              ,"Products.createdStamp").withColumnRenamed("createdStamp", "orderTimestamp").withColumnRenamed("outerStamp", "createdStamp")
 
   	//CheckOutStartDF.show()
		val CategorSaleStartDF = CheckOutStartDF.join(rackIdCategory,CheckOutStartDF("categoryId")===rackIdCategory("categoryId"))
		                                .drop(CheckOutStartDF("categoryId")).drop("rackId").drop("categoryName")
	 
		//footfall Code
		val FootFallStartDF = LocationDF.join(rackIdCategory,LocationDF("rackId")===rackIdCategory("rackId"))
		                                .drop("rackId","rackId","categoryName")
		                                
  //FootFallStartDF.show()		                                
	val categoryFootFall  = FootFallStartDF.withColumnRenamed("createdStamp","time")
		                                       .withColumn("footfall", lit(1))
						//.select("userId","orgId","storeId","day","month","year","category","FootFall")
						//.select("orgId","storeId","time","categoryId","categoryName","footfall")
						.select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"footfall")

	val categoryFootFall_static = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	//.option("dbtable", "DailyCategoryFootFall")
	.option("dbtable", "dailyCategoryFootFallTemp")  //Name Changed for temporary table
	.option("user", "root")
	.option("password", "")
	.load()			
	
	//categoryFootFall_static.show()	
	
	val joinedCategoryFootFall = categoryFootFall.union(categoryFootFall_static)	
	
	val aggregatedCategoryFootFall = joinedCategoryFootFall.groupBy("orgId","storeId","time","categoryId")
	                              .agg(sum(joinedCategoryFootFall("footfall")).alias("footfallCount"))  
	                              
	                              
	                              
	 	
	//adding new columns - day month and year to aggregated
	                              
  val categoryTimeBasedFootFall = aggregatedCategoryFootFall
                                          .withColumn("day", dayUDF(aggregatedCategoryFootFall("time"))) 	                              
                                          .withColumn("month", monthUDF(aggregatedCategoryFootFall("time")))
                                          .withColumn("year", yearUDF(aggregatedCategoryFootFall("time")))
	
  val finaldailyCategoryFootFall = categoryTimeBasedFootFall
                                      .select("orgId","storeId","day","month","year","time","categoryId","footfallCount")                                   
  //categoryTimeBasedFootFall.show()                        
  //aggregatedCategoryFootFall.show()
                                      
                                      
// finaldailyCategoryFootFall.show()                                    
	                          
 finaldailyCategoryFootFall.write.mode(SaveMode.Overwrite).jdbc(url,"dailyCategoryFootfall",prop) 
 categoryFootFall.write.mode(SaveMode.Append).jdbc(url,"dailyCategoryFootFallTemp",prop)  
		                                
	//****************************///////////**************************************
		                                
		                                
		                                
    //For Calculating the Converted/Failed Users
		 //val total_interim =  unionedVisit_interim.select($"orgId",$"storeId",$"userId",$"time")		  
		 //val Total = unionedVisit_interim.groupBy("orgId","storeId","time").agg(countDistinct(unionedVisit_interim("userId")).alias("total"))
		  
		  
		  
		  val Total = aggregatedCategoryFootFall.withColumnRenamed("footfallCount", "total")
		  Total.show()
		  
		 val converted_dynamic = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                           .select($"orgId",$"storeId",$"userId",$"categoryId",timeTrim($"time").alias("time"))
		                                           
		 val coverted_static_interim  =  sqlContext.read.format("jdbc").option("url",url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "converted_static")
	.option("user", "root")
	.option("password", "")
	.load()		                     
	
	
	
	 val  converted_static = coverted_static_interim.select("orgId","storeId","userId","categoryId","time")
	                                
	                                
	     converted_static.show()
	                                
	val finalConverted =  converted_static.union(converted_dynamic)                      
	                                
		                                           
		 val Converted = finalConverted.groupBy("orgId","storeId","time","categoryId").agg(countDistinct(finalConverted("userId")).alias("converted"))
		  Converted.show()                 
		    
		 val stats_interim =  Total.join(Converted,Seq("orgId","storeId","time","categoryId"),"outer")
		 
		  val stats =  stats_interim.na.fill(0);
		 //val statsFinal = stats.withColumn("failed", Total("total") - Converted("converted"))
		   //                                                              .withColumn("total",Total("total"))
		  val statsFinal = stats.withColumn("failed", stats("total") - stats("converted"))
		                                                                 .withColumn("total",stats("total"))
		                                                                 .withColumn("day",dayUDF(stats("time")))
		                                                                 .withColumn("month",monthUDF(stats("time")))
		                                                                 .withColumn("year",yearUDF(stats("time")))
		 
		                                                             
		 converted_dynamic.write.mode(SaveMode.Append).jdbc(url,"converted_static",prop)
		 statsFinal.show()
 
 
 
 
 /////////////////////////////////////////////////////////////////////////////////////////////////////
 
 
 
	/*newRepeatJoin.groupBy("orgId","storeId","time").agg(
	                  functions.count(col("visitType")==="REPEAT").as("count"),
                      functions.max("date").as("maxdate")
                      ).show()
	                                    
	                                    
	                                    aggregates.show()
	           .agg( count(newRepeatJoin("visitType").equals("REPEAT")).alias("repeated_visitors"),
	                  count(newRepeatJoin("visitType").equals("NEW")).alias("new_visitors")
	                )
	                
	val finalTotal = aggregates.withColumn("total",aggregates("new_visitors") + aggregates("repeated_visitors"))	
	finalTotal.show()
	userVisitType.write.mode(SaveMode.Append).jdbc(url,"userVisitType",prop)
	finalTotal.write.mode(SaveMode.Overwrite).jdbc(url,"dailyVisitors",prop)*/
	  /*val checkEarlierVisit = userCurrentvisit.union(updatedTime)                    
  
		
		checkEarlierVisit.show()
	val staticUpdatedEntry = checkEarlierVisit.groupBy("orgId","storeId","userId")
		                       .agg(min(checkEarlierVisit("time")).alias("firstTime"))
		                       .withColumn("count",lit(1))
  staticUpdatedEntry.show()                       
	staticUpdatedEntry.write.mode(SaveMode.Overwrite).jdbc(url,"userFirstVisit",prop)*/
	
	
	
	
  ///////////////////////////////////////////////////////////////////////////                             
   //static table for First Visit maintainence
   /* val userFirstVisit = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userFirstVisit")
	.option("user", "root")
	.option("password", "")
	.load()		                            
    
	val  userCurrentvisit =  LocationDF
                         .withColumn("count", lit(1))
		                     .drop("rackId")
		                     .withColumnRenamed("createdStamp","time")
		                     .select($"orgId",$"storeId",$"userId",$"time",$"count")
		// (orgId int,storeId int,userId varchar(255),time Timestamp,count int);
     
		val updatedTime = userFirstVisit.withColumnRenamed("firstTime", "time")
		val checkEarlierVisit = userCurrentvisit.union(updatedTime)                    
  
		
		checkEarlierVisit.show()
	val staticUpdatedEntry = checkEarlierVisit.groupBy("orgId","storeId","userId")
		                       .agg(min(checkEarlierVisit("time")).alias("firstTime"))
		                       .withColumn("count",lit(1))
  staticUpdatedEntry.show()                       
	staticUpdatedEntry.write.mode(SaveMode.Overwrite).jdbc(url,"userFirstVisit",prop)
	
	val updatedUserFirstVisit = sqlContext.read.format("jdbc").option("url", url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userFirstVisit")
	.option("user", "root")
	.option("password", "")
	.load()		 
	
	val joinedDataSet = userCurrentvisit.join(updatedUserFirstVisit,Seq("orgId","storeId","userId"))
	val userVisitType = joinedDataSet.withColumn("visitType", DateSub(joinedDataSet("time"),joinedDataSet("firstTime")))
	                                .drop("firstTime")
	
	val repeat_visitors = userVisitType.filter(userVisitType("visitType") ==="REPEAT")
	
	val new_visitors = userVisitType.filter(userVisitType("visitType") ==="NEW")
	
	
  val repeatCount = 	repeat_visitors.groupBy("orgId","storeId","time")
                              .agg(countDistinct(repeat_visitors("userId")).alias("repeated_visitors"))
                              

  val newCount = 	new_visitors.groupBy("orgId","storeId","time")
                              .agg(countDistinct(new_visitors("userId")).alias("new_visitors"))
                              
  val finalDailyVisitors_interim =  newCount.join(repeatCount,
                                            Seq("orgId","storeId","time"))
                                            
   finalDailyVisitors_interim
                    .withColumn("day",dayUDF(finalDailyVisitors_interim("time")))
                    .withColumn("month",monthUDF(finalDailyVisitors_interim("time")))
                    .withColumn("year",monthUDF(finalDailyVisitors_interim("time")))
         
   finalDailyVisitors_interim.show()      */                                 
  ///////////////////////////////////////////////////////////////////////
 
    //New and Repeat Visitors 
       
    
    /*
    
   val visits =  LocationDF
                         .withColumn("visitCount", lit(1))
		                     .drop("rackId")
		                     .withColumnRenamed("createdStamp","time")
		                     .select($"orgId",$"storeId",$"userId",timeTrim($"time").alias("time"),$"visitCount")
		                                  
 
  val visits_static =  sqlContext.read.format("jdbc").option("url",url)
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userVisit")
	.option("user", "root")
	.option("password", "")
	.load()				
	
	val unionedVisit_interim = visits.union(visits_static)
	
	//converting time into day level info
	val unionedVisit = unionedVisit_interim.select($"orgId",$"storeId",$"userId",$"time",$"visitCount")
	
   /** DailyLevel New/Repeat Visitors **/ 
   
	//adding new column for daily - New and Repeat
	//also adding different levels of time dimension
	 val  dailyVisitDF =  unionedVisit.groupBy("orgId","storeId","time","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))  
		                                    .select("orgId","storeId","time","userId","VisitCount")
	
	dailyVisitDF.show()
  //writing logic for dailyVisit		                                    
	//dailyVisitDF.write.mode(SaveMode.Overwrite).jdbc(url,"dailyVisit",prop)
  val newRepeatTable_temp = dailyVisitDF
		                                   .withColumn("New", newUserUDF(dailyVisitDF("VisitCount")))                          
		                                   .withColumn("Repeat", repeatUserUDF(dailyVisitDF("VisitCount")))		                                   
		                                   //.withColumn("day",dayUDF(dailyVisitDF("time")))
		                                   //.withColumn("month",monthUDF(dailyVisitDF("time")))
		                                   //.withColumn("year",yearUDF(dailyVisitDF("time")))
		 
	newRepeatTable_temp.show()
	val newRepeatTable_temp2 = newRepeatTable_temp
		                                      .select("orgId","storeId","time","New","Repeat")
  newRepeatTable_temp2.show()
	//newRepeatTable_temp.write.mode(SaveMode.Overwrite).jdbc(url,"newRepeatDaily",prop)	                                   
	                                   
	println(newRepeatTable_temp2.schema)
	val dailyVisitors_interim = newRepeatTable_temp2.groupBy("orgId","storeId","time")
	                   .agg(sum(newRepeatTable_temp2("New")).alias("new_visitors"),
	                        sum(newRepeatTable_temp2("Repeat")).alias("repeated_visitors"))
	             //      .agg(sum(newRepeatTable_temp2("Repeat")).alias("repeated_visitors"))
	                   
	dailyVisitors_interim.show()
	                  
	val dailyVisitors_interim2 = dailyVisitors_interim.withColumn("total",dailyVisitors_interim("new_visitors") + dailyVisitors_interim("repeated_visitors")) 
	val dailyVisitors = dailyVisitors_interim2
	                                .withColumn("day",dayUDF(dailyVisitors_interim("time")))
	                                .withColumn("month",monthUDF(dailyVisitors_interim("time")))
	                                .withColumn("year",yearUDF(dailyVisitors_interim("time")))
	                                .select("orgId","storeId","day","month","year","time","new_visitors",
	                                        "repeated_visitors","total")
	
		  
		dailyVisitors.show()	
		
		dailyVisitors.write.mode(SaveMode.Overwrite).jdbc(url, "dailyVisitors",prop)
		
		//newRepeatTable_temp.write.mode(SaveMode.Overwrite).jdbc(url,"newRepeatDaily",prop)
		  
    visits.write.mode(SaveMode.Append).jdbc(url,"userVisit",prop)
    
    
    
    */
    
    
    
    
    
    
    
    
    
    /*
    
   //////////////////////////////////////////////////////////////////////// 
    
    //CategorSaleStartDF.write.mode(SaveMode.Append).jdbc(url,"dailyUserCheckOut
    
    //For Calculating the Converted/Failed Users
		 val total_interim =  unionedVisit_interim.select($"orgId",$"storeId",$"userId",$"time")		  
		 val Total = unionedVisit_interim.groupBy("orgId","storeId","time").agg(countDistinct(unionedVisit_interim("userId")).alias("total"))
		  Total.show()
		  
		 val converted_dynamic = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                           .select("orgId","storeId","userId","time")
		                                           
		 val coverted_static_interim  =  sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "orders")
	.option("user", "root")
	.option("password", "")
	.load()		                     
	
	
	
	 val  converted_static = coverted_static_interim.select("orgId","storeId","userId","createdStamp")
	                                .withColumnRenamed("createdStamp", "time")
	                                
	     converted_static.show()
	                                
	val finalConverted =  converted_static.join(converted_dynamic)                      
	                                
		                                           
		 val Converted = finalConverted.groupBy("orgId","storeId","time").agg(countDistinct(finalConverted("userId")).alias("converted"))
		  Converted.show()                 
		  
		 val stats =  Total.join(Converted,Seq("orgId","storeId","time")).withColumn("failed", Total("total") - Converted("converted"))
		                                                                 .withColumn("total",Total("total"))
		 
		 stats.show()
    
    
    */
    
    
    
    
    
    
    
    
    
    
    
    /////////////////////////////////////////////////////////////////////////////
    
  //categoryFootFall.show()
			///////////
		
		 //STORE PROFILING
		//////////
		
		
		
	/*					
  
  
   import org.apache.spark.sql.functions._                         
  
   ///sale by total count
   //exploding the CheckOutEvent
		/*val CheckOutStartDF = CheckOutDF.withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","createdStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice")*/
						                              
		//CheckOutStartDF.show()
		val CategorSaleStartDF = CheckOutStartDF.join(rackIdCategory,CheckOutStartDF("categoryId")===rackIdCategory("categoryId"))
		                                .drop(CheckOutStartDF("categoryId")).drop("rackId")
		                                
		                                
		//CategorSaleStartDF.show()                                
		
		
		val categorySale  = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                      //.withColumn("sale", lit(1))
		                                      .withColumn("sale",CategorSaleStartDF("quantity"))
						//.select("userId","orgId","storeId","day","month","year","category","Sale")
						.select("orgId","storeId","time","categoryId","categoryName","sale")
						.select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"categoryName",$"sale")
						
		//categorySale.show()
			
		val categorySale_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySale")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	
	val joinedCategorySale = categorySale.union(categorySale_static)
	
	val aggregatedCategorySale = joinedCategorySale.groupBy("orgId","storeId","time","categoryId","categoryName")
	                              .agg(sum(joinedCategorySale("sale")).alias("saleCountAgg")) 		
		  
	//adding the month and year column into the aggregated column
	val modifiedAggregatedCategorySale = aggregatedCategorySale
	                                                         .withColumn("day",dayUDF(aggregatedCategorySale("time")))
	                                                         .withColumn("month", monthUDF(aggregatedCategorySale("time")))
	                                                         .withColumn("year", yearUDF(aggregatedCategorySale("time")))       
	                              
	//modifiedAggregatedCategorySale.show()
	                              
 // aggregatedCategorySale.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySaleCount",prop)
 
 // categorySale.write.mode(SaveMode.Append).jdbc(url,"DailyCategorySale",prop)
						
//	categorySale.show()
		
		
////Sale by total unitPrice
		
		
	val categorySaleAmount =	CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                    .withColumn("saleAmount", 
		                                    totalSalesUDF(CategorSaleStartDF("unitPrice"),CategorSaleStartDF("quantity")))
		                                   .select("orgId","storeId","time","categoryId","categoryName","saleAmount") 
		                                   .select($"orgId",$"storeId",timeTrim($"time").alias("time"),$"categoryId",$"categoryName",$"saleAmount")
		
		//categorySaleAmount.show()                                   
		                                   
		val categorySaleAmount_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySalePrice")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	
	 val joinedCategorySaleAmount = categorySaleAmount.union(categorySaleAmount_static)
	
	 val aggregatedCategorySaleAmount = joinedCategorySaleAmount.groupBy("orgId","storeId","time","categoryId","categoryName")
	                              .agg(sum(joinedCategorySaleAmount("saleAmount")).alias("saleAmountAgg")) 
		                                   
	
  //adding the month and year column into the aggregated column	                              
	
	  val modifiedAggregatedCategorySaleAmount =  aggregatedCategorySaleAmount
	                                                            .withColumn("day",dayUDF(aggregatedCategorySaleAmount("time")))
	                                                            .withColumn("month", monthUDF(aggregatedCategorySaleAmount("time")))
	                                                            .withColumn("year", yearUDF(aggregatedCategorySaleAmount("time")))    
	                              
    //modifiedAggregatedCategorySaleAmount.show()
   // aggregatedCategorySaleAmount.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySalePriceAgg",prop)
 
    //categorySaleAmount.write.mode(SaveMode.Append).jdbc(url,"DailyCategorySalePrice",prop)

	
   ////////
   /// New Visitor/Repeat Visitor
   ////////
 
 
   val visits =  LocationDF
		                                  .withColumn("visitCount", lit(1))
		                                  .drop("rackId")
		                                  .withColumnRenamed("createdStamp","time")
		                                  .select($"orgId",$"storeId",$"userId",timeTrim($"time").alias("time"),$"visitCount")
		                                  
 
  val visits_static =  sqlContext.read.format("jdbc").option("url", "jdbc:mysql://localhost:3306/Retail")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userVisit")
	.option("user", "root")
	.option("password", "")
	.load()				
	
	val unionedVisit_interim = visits.union(visits_static)
	
	//converting time into day level info
	val unionedVisit = unionedVisit_interim.select($"orgId",$"storeId",$"userId",$"time",$"visitCount")
	
   /** DailyLevel New/Repeat Visitors **/ 
   
	//adding new column for daily - New and Repeat
	//also adding different levels of time dimension
	 val  dailyVisitDF =  unionedVisit.groupBy("orgId","storeId","time","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))  
		                                    .select("orgId","storeId","time","userId","VisitCount")
	
  //writing logic for dailyVisit		                                    
	dailyVisitDF.write.mode(SaveMode.Overwrite).jdbc(url,"dailyVisit",prop)
  val newRepeatTable_temp = dailyVisitDF
		                                   .withColumn("New", newUserUDF(dailyVisitDF("VisitCount")))                          
		                                   .withColumn("Repeat", repeatUserUDF(dailyVisitDF("VisitCount")))		                                   
		                                   //.withColumn("day",dayUDF(dailyVisitDF("time")))
		                                   //.withColumn("month",monthUDF(dailyVisitDF("time")))
		                                   //.withColumn("year",yearUDF(dailyVisitDF("time")))
		                                   
	newRepeatTable_temp.write.mode(SaveMode.Overwrite).jdbc(url,"newRepeatDaily",prop)	                                   
	                                   
	
	newRepeatTable_temp.groupBy("orgId","storeId","time")
	                   .agg(sum(newRepeatTable_temp("")))
	
	//write newRepeatTable as temporary table                                   
		 
		                                   
		                               
	
		  
	 //load newRepeat Table static and merge with dynamic  
		  
		newRepeatTable_temp.show()
   	
		visits.write.mode(SaveMode.Append).jdbc(url,"userVisit",prop)		  
		
		
		newRepeatTable_temp.write.mode(SaveMode.Overwrite).jdbc(url,"newRepeatDaily",prop)
		  
		  
		  //For Calculating the Converted/Failed Users
		  val total_interim =  unionedVisit_interim.select($"orgId",$"storeId",$"userId",$"time")		  
		  val Total = unionedVisit_interim.groupBy("orgId","storeId","time").agg(countDistinct(unionedVisit_interim("userId")).alias("total"))
		  Total.show()
		  
		  val converte_interim = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                           .select("orgId","storeId","userId","time")
		  val Converted = converte_interim.groupBy("orgId","storeId","time").agg(countDistinct(converte_interim("userId")).alias("converted"))
		   Converted.show()                 
		  
		 val stats =  Total.join(Converted,Seq("orgId","storeId","time")).withColumn("failed", Total("total") - Converted("converted"))
		                                                                 .withColumn("total",Total("total"))
		 
		 stats.show()*/
		 
		 
		 
		  /*val dailyVisit_Repeat = dailyVisitDF.filter(dailyVisitDF("VisitCount").gt(1))
		  val dailyVisit_New = dailyVisitDF.filter($"VisitCount" ===1)
		  
		 dailyVisit_Repeat.write.mode(SaveMode.Overwrite).jdbc(url,"dailyRepeatVisitors",prop)
		 dailyVisit_New.write.mode(SaveMode.Overwrite).jdbc(url,"dailyNewVisitors",prop)
		
		 
		  
		  dailyVisitDF.show()
		  
		  dailyVisit_New.show() */
		  /*
		  
		  /** Monthly New/Repeat Visitors **/
		  val  monthlyVisitDF =  unionedVisit.groupBy("orgId","storeId","month","year","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount")) 
		                                     .select("orgId","storeId","time","userId","visitCount")
   	
		  val monthlyVisit_Repeat = monthlyVisitDF.filter(monthlyVisitDF("VisitCount").gt(1))
		  val monthlyVisit_New = monthlyVisitDF.filter($"VisitCount" ===1)
		  
		  monthlyVisit_Repeat.write.mode(SaveMode.Overwrite).jdbc(url,"monthlyRepeatVisitors",prop)
		  monthlyVisit_New.write.mode(SaveMode.Overwrite).jdbc(url,"monthlyNewVisitors",prop)
		
		 	  
		   /** Yearly New/Repeat Visitors **/
		  val  yearlyVisitDF =  unionedVisit.groupBy("orgId","storeId","month","year","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))     
		                                     .select("orgId","storeId","time","userId","visitCount")
   	
		  val yearlyVisit_Repeat = yearlyVisitDF.filter(yearlyVisitDF("VisitCount").gt(1))
		  val yearlyVisit_New = yearlyVisitDF.filter($"VisitCount" ===1)
		  
		  yearlyVisit_New.show() */
		 // yearlyVisit_Repeat.write.mode(SaveMode.Overwrite).jdbc(url,"yearlyRepeatVisitors",prop)
	//	  yearlyVisit_New.write.mode(SaveMode.Overwrite).jdbc(url,"yearlyNewVisitors",prop)
		
		  
		///////////
		
		 //USER PROFILING
		//////////
		
		/*
		val userProfile = CategorSaleStartDF
		val timedUserProfile  = userProfile.withColumn("day", dayUDF(userProfile.col("createdStamp")))
		                                  .withColumn("month", monthUDF(userProfile.col("createdStamp")))
		                                  .withColumn("year", yearUDF(userProfile.col("createdStamp")))
		                                  .drop("createdStamp")
		             
		val UserProfile =  sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userProfile")
	.option("user", "root")
	.option("password", "root")
	.load()                        
	
	
	  val staticUP   = UserProfile.select("userId","orderId","productId","quantity")	 
	  
	  
		timedUserProfile.show()
		val AllergyCheckDF = timedUserProfile.select("userId","orderId","productId","quantity")
		
		val joinedUP = staticUP.union(AllergyCheckDF)
		
		val joinedAllergyData = joinedUP.join(AllergenIndicators,joinedUP("productId")===AllergenIndicators("productId"))
		              .drop(AllergenIndicators("productId"))
		   
		val IndicativeUserBuyingHabit =    joinedAllergyData.groupBy("userId","orderId").count()       
		
		IndicativeUserBuyingHabit.show()
		
		
		//val PushNotification_Allergy =IndicativeUserBuyingHabit.groupBy("userId").agg( lit("Allergy detected"))
		val PushNotification_Allergy_Agg =IndicativeUserBuyingHabit.groupBy("userId")
		                                            .agg(sum(IndicativeUserBuyingHabit("count")).alias("purchasedCount"))
		                                            
		                                            
		 val  PushNotification_Allergy =  PushNotification_Allergy_Agg.withColumn("Allergy", 
		                                   allergyUDF(PushNotification_Allergy_Agg("purchasedCount")))
		    /*val message = activeMqHandle.pubSession.createTextMessage()
        message.setText("u1,Allergen,p10")
          
				activeMqHandle.publisher.publish(message)             */    
		
		
	PushNotification_Allergy.show()
		val gp = PushNotification_Allergy.rdd.groupBy( x => (x(0).toString(),(x(0).toString(),x(2).toString())) )		  
		  gp.foreach(p => {
		    val activeMqHandle:EZAnalyticsEngine = new EZAnalyticsEngine()
		    println( p._1.toString())
		    val message = activeMqHandle.pubSession.createTextMessage()
        message.setText("u1,Allergen,p11")
          
				activeMqHandle.publisher.publish(message) 
		   
		  })
		//
		timedUserProfile.write.mode(SaveMode.Append).jdbc(url,"userProfile",prop)
		//timedUserProfile.show()
		*/
	})

	ssc.start()
	ssc.awaitTermination()


}

def getEventType(json: JValue):String ={

	implicit val formats = new DefaultFormats {
		override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	}
	val childs = (json \ "eventType")
			val eventType = childs.extract[String]

					println(eventType)
					eventType

}

}