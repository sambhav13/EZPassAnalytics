import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SaveMode


class AllergenProfiler {
  
  
  
}


object AllergenProfiler{
  
  
  def main(args:Array[String]) = {
    
    println("hello")
    
    val  conf = new SparkConf()
                        .setMaster("local[2]")
                        .setAppName("CustomerAllergenProfiler")
                        .set("spark.sql.shuffle.partitions","1")
    
    val sc = new SparkContext(conf)
    
    val sqlContext = new SQLContext(sc)
    
   
   
   	//val url = "jdbc:mysql://172.31.28.225:3306/ezcheckout1"
   	val url = "jdbc:mysql://localhost:3306/Retail"
	  val table = "people";
	
    import java.util.Properties
  	val prop = new Properties() 
  	prop.put("user", "root")
  	prop.put("password", "")
  	prop.put("driver", "com.mysql.jdbc.Driver")


  	/*val AllergenCategory = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://localhost:3306/Retail")
  	.option("driver", "com.mysql.jdbc.Driver")
  	.option("dbtable", "AllergenCategory")
  	.option("user", "root")
  	.option("password", "")
  	.load()
  	
  	AllergenCategory.show()*/
  	
  	
  	
  	//load user Allegen Profile
  	val userAllergenProfile = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://localhost:3306/Retail")
  	.option("driver", "com.mysql.jdbc.Driver")
  	.option("dbtable", "UserAllergenProfile")
  	.option("user", "root")
  	.option("password", "")
  	.load()
  	
  	import org.apache.spark.sql.functions._
  	val userAllergenOrderCount  = userAllergenProfile.groupBy("orgId", "storeId","userId")
  	                                            .agg(countDistinct("orderId").alias("OrderCount"))	 	                                                  
  	                                           //.withColumnRenamed("countDistinct", "OrderCount")
  
  	                                           
    //filtering and building user profile based on the number of orders(currently 3)
  	val userAccountsForProfileBuild = userAllergenOrderCount.filter(userAllergenOrderCount("OrderCount")>=3)    
  	                                                            .select("userId")
  
  	userAccountsForProfileBuild.show()
  	                                                            
  	                                                            
  	val profiledUsers =   userAccountsForProfileBuild.join(userAllergenProfile,
  	                                                   userAccountsForProfileBuild("userId")===userAllergenProfile("userId"))
  	                                                   .drop(userAllergenProfile("userId"))
  	val userProfileDump = profiledUsers.select("orgId", "storeId","userId","Allergens") 
  	
  
  	
  	val df = userProfileDump.explode("Allergens","Allergen"){x:String => x.asInstanceOf[String].split(",")}
    val allergenProfile = df.drop("Allergens").distinct()
    
    
  	allergenProfile.show()
  	//userProfileDump.withColumn("Allergen", explode(userProfileDump("Allergens"))).show()
  	               
  	allergenProfile.write.mode(SaveMode.Append).jdbc(url,"UserAllergen",prop)
  	
  		//userProfileDump.show()
  	//agg("orderId"))
  	
  	
  	
  	
  	
  	/*
  	//All the static data loading 
  	val userAllergen = sqlContext.read.format("jdbc").option("url", "jdbc:mysql://localhost:3306/ezcheckout1")
  	.option("driver", "com.mysql.jdbc.Driver")
  	.option("dbtable", "UserAllergen")
  	.option("user", "root")
  	.option("password", "root")
  	.load()
  	*/
    
    
  }
}