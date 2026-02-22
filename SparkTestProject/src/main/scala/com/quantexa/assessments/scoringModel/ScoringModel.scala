package com.quantexa.assessments.scoringModel

import com.quantexa.assessments.accounts.AccountAssessment.AccountData
import com.quantexa.assessments.customerAddresses.CustomerAddress.AddressData
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession


/***
  * Part of the Quantexa solution is to flag high risk countries as a link to these countries may be an indication of
  * tax evasion.
  *
  * For this question you are required to populate the flag in the ScoringModel case class where the customer has an
  * address in the British Virgin Islands.
  *
  * This flag must be then used to return the number of customers in the dataset that have a link to a British Virgin
  * Islands address.
  */

object ScoringModel extends App {


  //Create a spark context, using a local master so Spark runs on the local machine
  val spark = SparkSession.builder().master("local[*]").appName("ScoringModel").getOrCreate()

  //importing spark implicits allows functions such as dataframe.as[T]

  //Set logger level to Warn
  Logger.getRootLogger.setLevel(Level.WARN)

  case class CustomerDocument(
                               customerId: String,
                               forename: String,
                               surname: String,
                               //Accounts for this customer
                               accounts: Seq[AccountData],
                               //Addresses for this customer
                               address: Seq[AddressData]
                             )

  case class ScoringModel(
                           customerId: String,
                           forename: String,
                           surname: String,
                           //Accounts for this customer
                           accounts: Seq[AccountData],
                           //Addresses for this customer
                           address: Seq[AddressData],
                           linkToBVI: Boolean
                         )


  //END GIVEN CODE

}
