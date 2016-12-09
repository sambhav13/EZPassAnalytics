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
class EZPassAnalyticsEngine extends Serializable {

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


object EZPassAnalyticsEngine extends Serializable{

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
		

def main(args:Array[String]) = {

  System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", """*""")
  
	//Spark initial Setup
	val sparkConf = new SparkConf().setAppName("DStreamAgg")
			.setMaster("local[2]")
			.set("spark.sql.shuffle.partitions","1")
			//.setMaster("spark://ip-172-31-21-112.ec2.internal:7077")
			val ssc = new StreamingContext(sparkConf, Seconds(3))

	val sqlContext = new SQLContext(ssc.sparkContext)
	//val lines = ssc.socketTextStream("localhost", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	//val lines = ssc.socketTextStream("23.23.21.63", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	//val lines = ssc.socketTextStream("172.31.28.225", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	val lines = ssc.socketTextStream("23.23.21.63", 9999, StorageLevel.MEMORY_AND_DISK_SER)
	
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
	val url = "jdbc:mysql://172.31.28.225:3306/ezcheckout1"
	//val url = "jdbc:mysql://172.31.28.225:3306/ezcheckout1"
	val table = "people";
	import java.util.Properties
	val prop = new Properties() 
	prop.put("user", "root")
	prop.put("password", "root")
	prop.put("driver", "com.mysql.jdbc.Driver")


	//All the static data loading 
	val dailyCategorySale = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategorySale")
	.option("user", "root")
	.option("password", "root")
	.load()

	val dailyCategoryFootFall = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategoryFootFall")
	.option("user", "root")
	.option("password", "root")
	.load()

	
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
	//rackIdCategory.show()
	

	
	
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

		 import org.apache.spark.sql.functions._      
		 //exploding the CheckOutEvent
		val CheckOutStartDF = CheckOutDF.withColumnRenamed("createdStamp","outerStamp").withColumn("Products", explode(CheckOutDF("orderItems")))
						                      .select("userId","orgId","storeId","orderId","outerStamp","Products.productId","Products.quantity"
						                              ,"Products.categoryId","Products.unitPrice","Products.discountApplied","Products.orderItemId"
						                              ,"Products.createdStamp").withColumnRenamed("createdStamp", "orderTimestamp").withColumnRenamed("outerStamp", "createdStamp")
			
						                              
	 
	 val userAllergenProfile_interim  =  CheckOutStartDF.select("orgId","storeId","userId","createdStamp","productId");
		
		
		
		/*val Products = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "Products")
	.option("user", "root")
	.option("password", "root")
	.load()
		
	val joinedData = userAllergenProfile_interim.join(Products,Seq("orgId","storeId"),"inner")
	                                            .drop("barcodeId","categoryId","productName","subName",
	                                                  "measuringUnit","unitPrice","quantity"
	                                                  ,"packOf","imageUrl","ingredients","createdStamp")
	                                                  
	  joinedData.show()*/
	     
	 //joinedData.write.mode(SaveMode.Append).jdbc(url,"UserAllergenProfile",prop)
	//	joinedData.drop("
    CheckOutStartDF.show()
			///////////
		
		 //STORE PROFILING
		//////////
		
		
		val FootFallStartDF = LocationDF.join(rackIdCategory,LocationDF("rackId")===rackIdCategory("rackId"))
		                                .drop("rackId","rackId")
		                                
		
		 
		
		//Logic for Footfall based on filtering BeacondId(Entrance)
		                                
	 
		                                
		import org.apache.spark.sql.functions.{array, lit, map, struct,sum}
		val categoryFootFall  = FootFallStartDF.withColumnRenamed("createdStamp","time")
		                                       .withColumn("FootFall", lit(1))
						//.select("userId","orgId","storeId","day","month","year","category","FootFall")
						.select("orgId","storeId","time","categoryId","categoryName","FootFall")
						
						
						
						
		val categoryFootFall_static = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://172.31.28.225:3306/ezcheckout1")
	.option("driver", "com.mysql.jdbc.Driver")
	.option("dbtable", "DailyCategoryFootFall")
	.option("user", "root")
	.option("password", "root")
	.load()			
	
	//categoryFootFall_static.show()
	
	
	val joinedCategoryFootFall = categoryFootFall.union(categoryFootFall_static)
	
	val aggregatedCategoryFootFall = joinedCategoryFootFall.groupBy("orgId","storeId","time","categoryId","categoryName")
	                              .agg(sum(joinedCategoryFootFall("FootFall")).alias("FootFallCount"))
	
	//adding new columns - day month and year to aggregated
	                              
  val categoryTimeBasedFootFall = aggregatedCategoryFootFall
                                          .withColumn("day", dayUDF(aggregatedCategoryFootFall("time"))) 	                              
                                          .withColumn("month", monthUDF(aggregatedCategoryFootFall("time")))
                                          .withColumn("year", yearUDF(aggregatedCategoryFootFall("time")))
	
                            
  //categoryTimeBasedFootFall.show()                        
  //aggregatedCategoryFootFall.show()
	                          
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
		                                .drop(CheckOutStartDF("categoryId")).drop("rackId")
		                                
		                                
		//CategorSaleStartDF.show()                                
		
		
		val categorySale  = CategorSaleStartDF.withColumnRenamed("createdStamp","time")
		                                      .withColumn("sale", lit(1))
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
	                              .agg(sum(joinedCategorySale("sale")).alias("saleCount")) 		
		  
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
		 
		 stats.show()
		 
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