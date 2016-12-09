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


@SerialVersionUID(100L)
class EZAnalyticsEngine extends Serializable {

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


object EZAnalyticsEngine extends Serializable{

	implicit val formats = new DefaultFormats {
		override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	} 


case class Event1(val eventType:String,val event:CheckOutEvent)
case class Event2(val eventType:String,val event:LocationEvent)

case class CheckOutEvent(
    val userId:String,
		val orgId:String,
		val storeId:String,
		val orderId:String,
		val orderItems:List[orderItems],
		val invoiceAmount:Double,
		val invoiceQuantity:Int,
		val createdStamp:Timestamp  
		)                          
//case class orderItems(val productId:String,val quantity:String,val categoryId:String,val unitPrice:String)
case class orderItems(val orderItemId:String,val productId:String,val categoryId:String,val quantity:String,
                          val unitPrice:String,val discountApplied:Double,val createdStamp:Timestamp)
case class LocationEvent(
    val eventId:String,
		val userId:String,
		val orgId:String,
		val storeId:String,
		val rackId:String,
		val locEventType:String,
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
	      
	      df.show()
	  })
	
	//DB Connection Setup
	val url = "jdbc:mysql://172.31.28.225:3306/RetailEasyPass"
	val table = "people";
	import java.util.Properties
	val prop = new Properties() 
	prop.put("user", "root")
	prop.put("password", "root")
	prop.put("driver", "com.mysql.jdbc.Driver")


	//All the static data loading 
	val dailyCategorySale = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySale")
	.option("user", "root")
	.option("password", "root")
	.load()

	val dailyCategoryFootFall = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategoryFootFall")
	.option("user", "root")
	.option("password", "root")
	.load()

	
	val racknCategory = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "racks")
	.option("user", "root")
	.option("password", "root")
	.load()
	
	val Category = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
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
	

	
	val AllergenIndicators = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "AllergenIndicators")
	.option("user", "root")
	.option("password", "root")
	.load()
	
	
	
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
		CheckOutDF.show()
		
		//UDF registration 

		val dayUDF = udf(timeDay _ )		
		val weekUDF = udf(timeWeek _)
		val monthUDF = udf(timeMonth _ )
		val quarterUDF = udf(timeQuarter _ )
		val yearUDF = udf(timeYear _ )
		val allergyUDF = udf( AllergyCheck _ )
		
		val totalSalesUDF = udf(TotalSalesByunitPrice _)

		 import org.apache.spark.sql.functions._      
		 //exploding the CheckOutEvent
		val CheckOutStartDF = CheckOutDF.withColumnRenamed("createdStamp","outerStamp").withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","outerStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice","Products.discountApplied","Products.orderItemId"
						                              ,"Products.createdStamp").withColumnRenamed("createdStamp", "orderTimestamp").withColumnRenamed("outerStamp", "createdStamp")
						                              
		CheckOutStartDF.show()
			///////////
		
		 //STORE PROFILING
		//////////
		
		
		val FootFallStartDF = LocationDF.join(rackIdCategory,LocationDF("rackId")===rackIdCategory("rackId"))
		                                .drop("rackId","rackId","categoryId")
		                                .withColumnRenamed("categoryName", "category")
		
		 
		
		//Logic for Footfall based on filtering BeacondId(Entrance)
		                                
	 
		                                
		import org.apache.spark.sql.functions.{array, lit, map, struct,sum}
		val categoryFootFall  = FootFallStartDF.withColumn("day", dayUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("month", monthUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("year", yearUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("FootFall", lit(1))
						//.select("userId","orgId","storeId","day","month","year","category","FootFall")
						.select("orgId","storeId","day","month","year","category","FootFall")
						
						
		val categoryFootFall_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategoryFootFall")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	categoryFootFall_static.show()
	
	
	val joinedCategoryFootFall = categoryFootFall.union(categoryFootFall_static)
	
	val aggregatedCategoryFootFall = joinedCategoryFootFall.groupBy("orgId","storeId","day","month","year","category")
	                              .agg(sum(joinedCategoryFootFall("FootFall")).alias("FootFallCount"))
	
	                          
 //aggregatedCategoryFootFall.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategoryFootFallCount",prop)
 
 //categoryFootFall.write.mode(SaveMode.Append).jdbc(url,"DailyCategoryFootFall",prop)  

  //categoryFootFall.show()
						
  
  
   import org.apache.spark.sql.functions._                         
  
   ///sale by total count
   //exploding the CheckOutEvent
		/*val CheckOutStartDF = CheckOutDF.withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","createdStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice")*/
						                              
		//CheckOutStartDF.show()
		val CategorSaleStartDF = CheckOutStartDF.join(rackIdCategory,CheckOutStartDF("categoryId")===rackIdCategory("categoryId"))
		                                .drop("categoryId","rackId","categoryId")
		                                .withColumnRenamed("categoryName", "category")
		                                
		//CategorSaleStartDF.show()                                
		
		
		val categorySale  = CategorSaleStartDF.withColumn("day", dayUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("month", monthUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("year", yearUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("sale", lit(1))
						//.select("userId","orgId","storeId","day","month","year","category","Sale")
						.select("orgId","storeId","day","month","year","category","sale")
						
				
		val categorySale_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySale")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	
	val joinedCategorySale = categorySale.union(categorySale_static)
	
	 val aggregatedCategorySale = joinedCategorySale.groupBy("orgId","storeId","day","month","year","category")
	                              .agg(sum(joinedCategorySale("sale")).alias("saleCount")) 		
		  
	
	                             
	                              
	//aggregatedCategorySale.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySaleCount",prop)
 
// categorySale.write.mode(SaveMode.Append).jdbc(url,"DailyCategorySale",prop)
						
	//	categorySale.show()
		
		
		////Sale by total unitPrice
		
		
	val categorySaleAmount =	CategorSaleStartDF.withColumn("day", dayUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("month", monthUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("year", yearUDF(CategorSaleStartDF.col("createdStamp")))
		                                  .withColumn("saleAmount", 
		                                    totalSalesUDF(CategorSaleStartDF("unitPrice"),CategorSaleStartDF("quantity")))
		                                   .select("orgId","storeId","day","month","year","category","saleAmount") 
		
		categorySaleAmount.show()                                   
		                                   
		val categorySaleAmount_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySalePrice")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	
	val joinedCategorySaleAmount = categorySaleAmount.union(categorySaleAmount_static)
	
	 val aggregatedCategorySaleAmount = joinedCategorySaleAmount.groupBy("orgId","storeId","day","month","year","category")
	                              .agg(sum(joinedCategorySaleAmount("saleAmount")).alias("saleAmountAgg")) 
		                                   
		   
	
	//aggregatedCategorySaleAmount.write.mode(SaveMode.Overwrite).jdbc(url,"DailyCategorySalePriceAgg",prop)
 
 // categorySaleAmount.write.mode(SaveMode.Append).jdbc(url,"DailyCategorySalePrice",prop)

	
   ////////
   /// New Visitor/Repeat Visitor
   ////////
 
 
 val visits =  LocationDF.withColumn("day", dayUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("month", monthUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("year", yearUDF(LocationDF.col("createdStamp")))
		                                  .withColumn("visitCount", lit(1))
		                                  .drop("rackId","createdStamp")
		                                  .select("orgId","storeId","day","month","year","userId","visitCount")
 
val visits_static =  sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "userVisit")
	.option("user", "root")
	.option("password", "root")
	.load()				
	
	val unionedVisit = visits.union(visits_static)
		                                  
   /** DailyLevel New/Repeat Visitors **/ 
   
		  val  dailyVisitDF =  unionedVisit.groupBy("orgId","storeId","day","month","year","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))                      
   	
		  val dailyVisit_Repeat = dailyVisitDF.filter(dailyVisitDF("VisitCount").gt(1))
		  val dailyVisit_New = dailyVisitDF.filter($"VisitCount" ===1)
		  
		 //dailyVisit_Repeat.write.mode(SaveMode.Overwrite).jdbc(url,"dailyRepeatVisitors",prop)
		 //dailyVisit_New.write.mode(SaveMode.Overwrite).jdbc(url,"dailyNewVisitors",prop)
		
		  //visits.write.mode(SaveMode.Append).jdbc(url,"userVisit",prop)
		  
		  dailyVisitDF.show()
		  
		  
		  /** Monthly New/Repeat Visitors **/
		  val  monthlyVisitDF =  unionedVisit.groupBy("orgId","storeId","month","year","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))                      
   	
		  val monthlyVisit_Repeat = monthlyVisitDF.filter(monthlyVisitDF("VisitCount").gt(1))
		  val monthlyVisit_New = monthlyVisitDF.filter($"VisitCount" ===1)
		  
		  monthlyVisit_Repeat.write.mode(SaveMode.Overwrite).jdbc(url,"monthlyRepeatVisitors",prop)
		  monthlyVisit_New.write.mode(SaveMode.Overwrite).jdbc(url,"monthlyNewVisitors",prop)
		
		 	  
		   /** Yearly New/Repeat Visitors **/
		  val  yearlyVisitDF =  unionedVisit.groupBy("orgId","storeId","month","year","userId")
		                                    .agg(sum(unionedVisit("visitCount")).alias("VisitCount"))                      
   	
		  val yearlyVisit_Repeat = yearlyVisitDF.filter(yearlyVisitDF("VisitCount").gt(1))
		  val yearlyVisit_New = yearlyVisitDF.filter($"VisitCount" ===1)
		  
		  yearlyVisit_New.show()
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
		             
		val UserProfile =  sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/RetailEasyPass")
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